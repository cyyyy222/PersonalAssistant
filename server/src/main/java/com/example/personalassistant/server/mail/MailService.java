package com.example.personalassistant.server.mail;

import com.example.personalassistant.server.common.ApiException;
import com.example.personalassistant.server.mail.MailDtos.AccountView;
import com.example.personalassistant.server.mail.MailDtos.BindRequest;
import com.example.personalassistant.server.mail.MailDtos.MessageDetail;
import com.example.personalassistant.server.mail.MailDtos.MessagePage;
import com.example.personalassistant.server.mail.MailDtos.SendRequest;
import com.example.personalassistant.server.mail.MailDtos.SendResult;
import com.example.personalassistant.server.mail.MailDtos.SyncResult;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MailService {

    private final MailAccountRepository accountRepository;
    private final MailSyncStateRepository syncStateRepository;
    private final MailMessageRepository messageRepository;
    private final MailProviderRegistry providerRegistry;
    private final MailCryptoService cryptoService;
    private final ImapClient imapClient;
    private final SmtpClient smtpClient;
    private final MailAttachmentService attachmentService;
    private final ConcurrentHashMap<Long, AtomicBoolean> syncing = new ConcurrentHashMap<>();

    public MailService(
            MailAccountRepository accountRepository,
            MailSyncStateRepository syncStateRepository,
            MailMessageRepository messageRepository,
            MailProviderRegistry providerRegistry,
            MailCryptoService cryptoService,
            ImapClient imapClient,
            SmtpClient smtpClient,
            MailAttachmentService attachmentService
    ) {
        this.accountRepository = accountRepository;
        this.syncStateRepository = syncStateRepository;
        this.messageRepository = messageRepository;
        this.providerRegistry = providerRegistry;
        this.cryptoService = cryptoService;
        this.imapClient = imapClient;
        this.smtpClient = smtpClient;
        this.attachmentService = attachmentService;
    }

    public List<AccountView> listAccounts(long userId) {
        return accountRepository.findByUser(userId).stream().map(AccountView::from).toList();
    }

    @Transactional
    public AccountView bind(long userId, BindRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        validateEmail(email);
        if (request.secret().isBlank()) {
            throw new ApiException(400, "请输入邮箱授权码");
        }
        if (accountRepository.findByUserAndEmail(userId, email).isPresent()) {
            throw new ApiException(409, "该邮箱已绑定");
        }

        MailProvider provider = providerRegistry.resolve(email);
        ImapClient.ConnectionInfo connection = imapClient.test(provider, email, request.secret().trim());
        String cipher = cryptoService.encrypt(request.secret().trim());
        long accountId = accountRepository.insert(
                userId, email, request.displayName(), provider, cipher, 1
        );
        syncStateRepository.save(accountId, "INBOX", connection.uidValidity(), 0, 0, null);
        return accountRepository.findByIdAndUser(accountId, userId)
                .map(AccountView::from)
                .orElseThrow(() -> new ApiException(500, "邮箱绑定失败"));
    }

    @Transactional
    public void unbind(long userId, long accountId) {
        findAccount(userId, accountId);
        attachmentService.deleteByAccount(accountId);
        messageRepository.deleteByAccount(accountId);
        syncStateRepository.deleteByAccount(accountId);
        accountRepository.delete(accountId, userId);
    }

    public SyncResult sync(long userId, long accountId) {
        return syncAccount(findAccount(userId, accountId));
    }

    public SyncResult syncAccount(MailAccount account) {
        long accountId = account.id();
        AtomicBoolean flag = syncing.computeIfAbsent(accountId, ignored -> new AtomicBoolean(false));
        if (!flag.compareAndSet(false, true)) {
            throw new ApiException(409, "正在同步，请稍后再试");
        }

        try {
            String secret = cryptoService.decrypt(account.secretCipher());
            MailSyncState state = syncStateRepository.find(accountId, "INBOX")
                    .orElseGet(() -> MailSyncState.empty(accountId, "INBOX"));
            int[] counts = new int[]{0, 0};
            ImapClient.SyncOutcome outcome = imapClient.sync(account, secret, state, new ImapClient.SyncSink() {
                @Override
                public void reset(long uidValidity) {
                    attachmentService.deleteByFolder(accountId, "INBOX");
                    messageRepository.clearFolder(accountId, "INBOX");
                    syncStateRepository.save(accountId, "INBOX", uidValidity, 0, 0, null);
                }

                @Override
                public void message(ParsedMail mail) {
                    MailMessageRepository.UpsertResult result = messageRepository.upsert(accountId, "INBOX", mail);
                    attachmentService.replaceForMessage(result.id(), mail.attachments());
                    if (result.created()) {
                        counts[0]++;
                    } else {
                        counts[1]++;
                    }
                }

                @Override
                public void progress(long uidValidity, long lastUid) {
                    syncStateRepository.save(accountId, "INBOX", uidValidity, lastUid, 0, null);
                }
            });

            syncStateRepository.save(accountId, "INBOX", outcome.uidValidity(), outcome.lastUid(), 0, null);
            accountRepository.touchSync(accountId);
            return new SyncResult(counts[0], counts[1], outcome.uidValidityChanged(), outcome.lastUid());
        } catch (ApiException exception) {
            accountRepository.updateError(accountId, exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            accountRepository.updateError(accountId, "同步失败");
            throw new ApiException(500, "同步邮箱失败");
        } finally {
            flag.set(false);
        }
    }

    public List<SyncResult> syncAll(long userId) {
        return accountRepository.findByUser(userId).stream().map(this::syncAccount).toList();
    }

    public MessagePage listMessages(long userId, long accountId, String folder, int page, int size,
                                    String keyword, boolean unreadOnly, Boolean starred, Boolean archived) {
        findAccount(userId, accountId);
        return messageRepository.list(accountId, normalizeFolder(folder), page, size, keyword,
                unreadOnly, starred, archived);
    }

    public MessageDetail getMessage(long userId, long messageId) {
        return messageRepository.findDetail(messageId, userId)
                .orElseThrow(() -> new ApiException(404, "邮件不存在"));
    }

    public void markRead(long userId, long messageId, boolean read) {
        if (messageRepository.findDetail(messageId, userId).isEmpty()) {
            throw new ApiException(404, "邮件不存在");
        }
        messageRepository.markRead(messageId, userId, read);
    }

    public void markStarred(long userId, long messageId, boolean starred) {
        MailMessageRepository.MessageRef ref = messageRepository.findRef(messageId, userId)
                .orElseThrow(() -> new ApiException(404, "邮件不存在"));
        MailAccount account = findAccount(userId, ref.accountId());
        if ("INBOX".equalsIgnoreCase(ref.folder())) {
            String secret = cryptoService.decrypt(account.secretCipher());
            imapClient.setStarred(account, secret, ref.folder(), ref.uid(), starred);
        }
        messageRepository.markStarred(messageId, userId, starred);
    }

    public String archive(long userId, long messageId) {
        MailMessageRepository.MessageRef ref = messageRepository.findRef(messageId, userId)
                .orElseThrow(() -> new ApiException(404, "邮件不存在"));
        if (!"INBOX".equalsIgnoreCase(ref.folder())) {
            throw new ApiException(409, "该邮件已在归档文件夹中");
        }
        MailAccount account = findAccount(userId, ref.accountId());
        String secret = cryptoService.decrypt(account.secretCipher());
        String archiveFolder = imapClient.archive(account, secret, ref.uid());
        accountRepository.updateFolders(account.id(), archiveFolder, account.sentFolder());
        messageRepository.markArchived(messageId, userId, archiveFolder);
        return archiveFolder;
    }

    public SendResult send(long userId, SendRequest request) {
        MailAccount account = findAccount(userId, request.accountId());
        validateAddressList(request.to());
        validateAddressList(request.cc());
        validateAddressList(request.bcc());

        List<MailAttachmentService.AttachmentData> attachments = request.attachmentIds() == null
                ? List.of()
                : request.attachmentIds().stream()
                .map(id -> attachmentService.readForSend(userId, id))
                .toList();

        String inReplyTo = null;
        String references = null;
        if (request.replyToMessageId() != null) {
            MailMessageRepository.MessageRef original = messageRepository
                    .findRef(request.replyToMessageId(), userId)
                    .orElseThrow(() -> new ApiException(404, "原邮件不存在"));
            inReplyTo = original.messageId();
            references = original.messageId();
        }

        String secret = cryptoService.decrypt(account.secretCipher());
        SmtpClient.SentMail sent = smtpClient.send(account, secret, new SmtpClient.OutgoingMail(
                request.to().trim(),
                request.cc(),
                request.bcc(),
                request.subject().trim(),
                request.body(),
                Boolean.TRUE.equals(request.html()),
                inReplyTo,
                references,
                attachments
        ));
        boolean savedToSent = imapClient.appendToSent(account, secret, sent.message());
        return new SendResult(sent.messageId(), savedToSent);
    }

    private MailAccount findAccount(long userId, long accountId) {
        return accountRepository.findByIdAndUser(accountId, userId)
                .orElseThrow(() -> new ApiException(404, "邮箱账号不存在"));
    }

    private String normalizeFolder(String folder) {
        return folder == null || folder.isBlank() ? "INBOX" : folder.trim();
    }

    private void validateAddressList(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            for (InternetAddress address : InternetAddress.parse(value, true)) {
                address.validate();
            }
        } catch (AddressException exception) {
            throw new ApiException(400, "邮箱地址格式不正确");
        }
    }

    private void validateEmail(String email) {
        try {
            InternetAddress address = new InternetAddress(email, true);
            address.validate();
        } catch (AddressException exception) {
            throw new ApiException(400, "邮箱地址格式不正确");
        }
    }
}
