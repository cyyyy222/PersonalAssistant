package com.example.personalassistant.server.mail;

import com.example.personalassistant.server.common.ApiException;
import jakarta.mail.Address;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.BodyPart;
import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.imap.IMAPStore;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
public class ImapClient {

    public record ConnectionInfo(int messageCount, long uidValidity) {
    }

    public record SyncOutcome(long uidValidity, long lastUid, boolean uidValidityChanged, int fetchedMessages) {
    }

    public interface SyncSink {
        void reset(long uidValidity);

        void message(ParsedMail mail);

        void progress(long uidValidity, long lastUid);
    }

    private static final Logger log = LoggerFactory.getLogger(ImapClient.class);

    private final int connectionTimeoutMs;
    private final int readTimeoutMs;
    private final int syncBatchSize;
    private final int maxMessagesPerSync;

    public ImapClient(
            @Value("${app.mail.connection-timeout-ms:10000}") int connectionTimeoutMs,
            @Value("${app.mail.read-timeout-ms:15000}") int readTimeoutMs,
            @Value("${app.mail.sync-batch-size:50}") int syncBatchSize,
            @Value("${app.mail.max-messages-per-sync:300}") int maxMessagesPerSync
    ) {
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.syncBatchSize = Math.max(1, syncBatchSize);
        this.maxMessagesPerSync = Math.max(1, maxMessagesPerSync);
    }

    public ConnectionInfo test(MailProvider provider, String email, String secret) {
        Store store = null;
        Folder inbox = null;
        try {
            store = session(provider.imapHost(), provider.imapPort()).getStore("imaps");
            store.connect(provider.imapHost(), provider.imapPort(), email, secret);
            sendClientId(store, email);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            long uidValidity = ((UIDFolder) inbox).getUIDValidity();
            int count = inbox.getMessageCount();
            return new ConnectionInfo(count, uidValidity);
        } catch (MessagingException exception) {
            throw mapException(exception);
        } catch (Exception exception) {
            throw new ApiException(502, "连接邮箱失败，请稍后再试");
        } finally {
            close(inbox, store);
        }
    }

    public SyncOutcome sync(MailAccount account, String secret, MailSyncState state, SyncSink sink) {
        Store store = null;
        Folder inbox = null;
        try {
            store = session(account.imapHost(), account.imapPort()).getStore("imaps");
            store.connect(account.imapHost(), account.imapPort(), account.imapUsername(), secret);
            sendClientId(store, account.imapUsername());

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            UIDFolder uidFolder = (UIDFolder) inbox;
            long uidValidity = uidFolder.getUIDValidity();
            boolean changed = state.uidValidity() != 0 && state.uidValidity() != uidValidity;
            if (changed) {
                sink.reset(uidValidity);
            }

            long lastUid = changed ? 0 : state.lastUid();
            Message[] candidates = uidFolder.getMessagesByUID(lastUid + 1, UIDFolder.LASTUID);
            if (candidates == null) {
                candidates = new Message[0];
            }

            int limit = Math.min(candidates.length, maxMessagesPerSync);
            int fetched = 0;
            for (int offset = 0; offset < limit; offset += syncBatchSize) {
                int end = Math.min(offset + syncBatchSize, limit);
                Message[] batch = Arrays.copyOfRange(candidates, offset, end);
                FetchProfile profile = new FetchProfile();
                profile.add(FetchProfile.Item.ENVELOPE);
                profile.add(FetchProfile.Item.FLAGS);
                profile.add(FetchProfile.Item.CONTENT_INFO);
                profile.add(UIDFolder.FetchProfileItem.UID);
                inbox.fetch(batch, profile);

                for (Message message : batch) {
                    try {
                        long uid = uidFolder.getUID(message);
                        ParsedMail parsed = parse(message, uid);
                        sink.message(parsed);
                        lastUid = Math.max(lastUid, uid);
                        fetched++;
                    } catch (Exception parseError) {
                        log.warn("Skip unreadable mail UID in account {}: {}", account.id(), parseError.getMessage());
                    }
                }
                sink.progress(uidValidity, lastUid);
            }

            return new SyncOutcome(uidValidity, lastUid, changed, fetched);
        } catch (MessagingException exception) {
            throw mapException(exception);
        } catch (Exception exception) {
            throw new ApiException(502, "同步邮箱失败，请稍后再试");
        } finally {
            close(inbox, store);
        }
    }

    public void setStarred(MailAccount account, String secret, String folderName, long uid, boolean starred) {
        Store store = null;
        Folder folder = null;
        try {
            store = connect(account, secret);
            folder = store.getFolder(folderName == null || folderName.isBlank() ? "INBOX" : folderName);
            folder.open(Folder.READ_WRITE);
            Message message = ((UIDFolder) folder).getMessageByUID(uid);
            if (message == null) {
                throw new ApiException(404, "邮件已不在邮箱中");
            }
            message.setFlag(Flags.Flag.FLAGGED, starred);
        } catch (MessagingException exception) {
            throw mapException(exception);
        } finally {
            close(folder, store);
        }
    }

    public String archive(MailAccount account, String secret, long uid) {
        Store store = null;
        Folder inbox = null;
        try {
            store = connect(account, secret);
            Folder archiveFolder = findFolder(store, account.archiveFolder(),
                    "archive", "archives", "已归档", "归档");
            if (archiveFolder == null) {
                throw new ApiException(400, "QQ 邮箱未提供可用的归档文件夹");
            }

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            Message message = ((UIDFolder) inbox).getMessageByUID(uid);
            if (message == null) {
                throw new ApiException(404, "邮件已不在收件箱中");
            }
            if (store instanceof IMAPStore imapStore && imapStore.hasCapability("MOVE")
                    && inbox instanceof IMAPFolder imapFolder) {
                imapFolder.moveMessages(new Message[]{message}, archiveFolder);
            } else {
                inbox.copyMessages(new Message[]{message}, archiveFolder);
                message.setFlag(Flags.Flag.DELETED, true);
                inbox.expunge();
            }
            return archiveFolder.getFullName();
        } catch (MessagingException exception) {
            throw mapException(exception);
        } finally {
            close(inbox, store);
        }
    }

    public AttachmentContent downloadAttachment(MailAccount account, String secret, String folderName,
                                                long uid, int partIndex) {
        Store store = null;
        Folder folder = null;
        try {
            store = connect(account, secret);
            folder = store.getFolder(folderName == null || folderName.isBlank() ? "INBOX" : folderName);
            folder.open(Folder.READ_ONLY);
            Message message = ((UIDFolder) folder).getMessageByUID(uid);
            if (message == null) {
                throw new ApiException(404, "邮件已不在邮箱中");
            }

            int[] counter = new int[]{0};
            AttachmentContent[] result = new AttachmentContent[1];
            findAttachment(message, partIndex, counter, result, 0);
            if (result[0] == null) {
                throw new ApiException(404, "附件不存在");
            }
            return result[0];
        } catch (MessagingException exception) {
            throw mapException(exception);
        } catch (Exception exception) {
            throw new ApiException(500, "下载附件失败");
        } finally {
            close(folder, store);
        }
    }

    public boolean appendToSent(MailAccount account, String secret, MimeMessage message) {
        Store store = null;
        Folder sentFolder = null;
        try {
            store = connect(account, secret);
            sentFolder = findFolder(store, account.sentFolder(),
                    "sent", "sent messages", "已发送", "已发送邮件");
            if (sentFolder == null) {
                return false;
            }
            sentFolder.open(Folder.READ_WRITE);
            sentFolder.appendMessages(new Message[]{message});
            return true;
        } catch (Exception exception) {
            log.warn("Unable to append sent mail for account {}: {}", account.id(), exception.getMessage());
            return false;
        } finally {
            close(sentFolder, store);
        }
    }

    private Store connect(MailAccount account, String secret) throws MessagingException {
        Store store = session(account.imapHost(), account.imapPort()).getStore("imaps");
        store.connect(account.imapHost(), account.imapPort(), account.imapUsername(), secret);
        sendClientId(store, account.imapUsername());
        return store;
    }

    private Folder findFolder(Store store, String cachedName, String... keywords) throws MessagingException {
        if (cachedName != null && !cachedName.isBlank()) {
            Folder cached = store.getFolder(cachedName);
            if (cached.exists()) {
                return cached;
            }
        }

        Folder[] folders = store.getDefaultFolder().list("*");
        for (Folder folder : folders) {
            String name = folder.getName() == null ? "" : folder.getName().toLowerCase(java.util.Locale.ROOT);
            String fullName = folder.getFullName() == null ? "" : folder.getFullName().toLowerCase(java.util.Locale.ROOT);
            for (String keyword : keywords) {
                if (name.equals(keyword) || name.contains(keyword) || fullName.contains(keyword)) {
                    return folder;
                }
            }
        }
        return null;
    }

    private void findAttachment(Part part, int target, int[] counter, AttachmentContent[] result, int depth)
            throws Exception {
        if (depth > 12 || result[0] != null) {
            return;
        }
        String fileName = part.getFileName();
        if (fileName != null || Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
            if (counter[0] == target) {
                Object content = part.getContent();
                byte[] bytes;
                if (content instanceof InputStream input) {
                    bytes = input.readAllBytes();
                } else if (content instanceof String text) {
                    bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                } else {
                    throw new ApiException(415, "暂不支持该附件类型");
                }
                result[0] = new AttachmentContent(
                        decode(fileName),
                        part.getContentType() == null ? "application/octet-stream" : part.getContentType(),
                        bytes
                );
                return;
            }
            counter[0]++;
            return;
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int index = 0; index < multipart.getCount(); index++) {
                findAttachment(multipart.getBodyPart(index), target, counter, result, depth + 1);
            }
        }
    }

    public record AttachmentContent(String filename, String contentType, byte[] bytes) {
    }

    private Session session(String host, int port) {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", host);
        properties.put("mail.imaps.port", String.valueOf(port));
        properties.put("mail.imaps.ssl.enable", "true");
        properties.put("mail.imaps.ssl.checkserveridentity", "true");
        properties.put("mail.imaps.connectiontimeout", String.valueOf(connectionTimeoutMs));
        properties.put("mail.imaps.timeout", String.valueOf(readTimeoutMs));
        properties.put("mail.imaps.writetimeout", String.valueOf(readTimeoutMs));
        return Session.getInstance(properties);
    }

    private void sendClientId(Store store, String email) {
        if (!(store instanceof IMAPStore imapStore)) {
            return;
        }
        try {
            imapStore.id(Map.of(
                    "name", "PersonalAssistant",
                    "version", "1.0",
                    "vendor", "PersonalAssistant",
                    "support-email", email
            ));
        } catch (MessagingException ignored) {
            // Older IMAP servers may not support the ID extension.
        }
    }

    private ParsedMail parse(Message message, long uid) throws Exception {
        BodyParts parts = new BodyParts();
        extract(message, parts, 0);

        String text = parts.plain.toString().trim();
        String html = parts.html.toString().trim();
        if (text.isEmpty() && !html.isEmpty()) {
            text = Jsoup.parse(html).text();
        }
        String snippet = text.replaceAll("\\s+", " ").trim();
        if (snippet.length() > 200) {
            snippet = snippet.substring(0, 200);
        }

        Address[] from = message.getFrom();
        String fromName = "";
        String fromAddress = "";
        if (from != null && from.length > 0 && from[0] instanceof InternetAddress address) {
            fromName = decode(address.getPersonal());
            fromAddress = address.getAddress();
        }

        String toAddresses = addresses(message.getRecipients(Message.RecipientType.TO));
        LocalDateTime sentAt = toLocal(message.getSentDate());
        LocalDateTime receivedAt = toLocal(message.getReceivedDate());
        String messageId = message instanceof MimeMessage mimeMessage ? mimeMessage.getMessageID() : null;

        return new ParsedMail(
                uid,
                messageId,
                decode(message.getSubject()),
                fromName,
                fromAddress,
                toAddresses,
                sentAt,
                receivedAt,
                snippet,
                text,
                html,
                !parts.attachments.isEmpty(),
                parts.attachments.size(),
                message.isSet(Flags.Flag.SEEN),
                message.isSet(Flags.Flag.FLAGGED),
                Math.max(message.getSize(), 0),
                parts.attachments
        );
    }

    private void extract(Part part, BodyParts parts, int depth) throws Exception {
        if (depth > 12) {
            return;
        }
        String fileName = part.getFileName();
        String disposition = part.getDisposition();
        if (fileName != null || Part.ATTACHMENT.equalsIgnoreCase(disposition)) {
            String[] contentIdHeader = part.getHeader("Content-ID");
            String contentId = contentIdHeader == null || contentIdHeader.length == 0 ? null
                    : contentIdHeader[0].replace("<", "").replace(">", "");
            parts.attachments.add(new ParsedAttachment(
                    parts.attachments.size(),
                    decode(fileName),
                    part.getContentType() == null ? "application/octet-stream" : part.getContentType(),
                    Math.max(part.getSize(), 0),
                    contentId
            ));
            return;
        }
        if (part.isMimeType("text/plain")) {
            Object content = part.getContent();
            if (content instanceof String value) {
                parts.plain.append(value).append('\n');
            }
            return;
        }
        if (part.isMimeType("text/html")) {
            Object content = part.getContent();
            if (content instanceof String value) {
                parts.html.append(value).append('\n');
            }
            return;
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int index = 0; index < multipart.getCount(); index++) {
                BodyPart bodyPart = multipart.getBodyPart(index);
                extract(bodyPart, parts, depth + 1);
            }
            return;
        }
        if (part.isMimeType("message/rfc822")) {
            Object content = part.getContent();
            if (content instanceof Part nested) {
                extract(nested, parts, depth + 1);
            }
        }
    }

    private String addresses(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (Address address : addresses) {
            if (address instanceof InternetAddress internetAddress) {
                values.add(internetAddress.toUnicodeString());
            } else {
                values.add(address.toString());
            }
        }
        return String.join(", ", values);
    }

    private String decode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return MimeUtility.decodeText(value);
        } catch (Exception ignored) {
            return value;
        }
    }

    private LocalDateTime toLocal(java.util.Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private ApiException mapException(MessagingException exception) {
        if (exception instanceof AuthenticationFailedException) {
            return new ApiException(401, "邮箱授权码错误，或邮箱未开启 IMAP 服务");
        }
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof UnknownHostException) {
            return new ApiException(502, "无法解析邮箱服务器地址");
        }
        if (cause instanceof SocketTimeoutException) {
            return new ApiException(504, "连接邮箱服务器超时");
        }
        if (cause instanceof SSLException) {
            return new ApiException(502, "邮箱服务器 TLS 连接失败");
        }
        String message = exception.getMessage();
        if (message != null && message.toLowerCase().contains("imap")) {
            return new ApiException(400, "邮箱未开启 IMAP 服务");
        }
        return new ApiException(502, "连接邮箱失败，请稍后再试");
    }

    private void close(Folder folder, Store store) {
        if (folder != null && folder.isOpen()) {
            try {
                folder.close(false);
            } catch (MessagingException ignored) {
            }
        }
        if (store != null && store.isConnected()) {
            try {
                store.close();
            } catch (MessagingException ignored) {
            }
        }
    }

    private static final class BodyParts {
        private final StringBuilder plain = new StringBuilder();
        private final StringBuilder html = new StringBuilder();
        private final List<ParsedAttachment> attachments = new ArrayList<>();
    }
}
