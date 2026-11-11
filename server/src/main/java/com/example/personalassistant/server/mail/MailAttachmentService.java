package com.example.personalassistant.server.mail;

import com.example.personalassistant.server.common.ApiException;
import com.example.personalassistant.server.mail.storage.AttachmentStorage;
import com.example.personalassistant.server.mail.storage.StoredObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

@Service
public class MailAttachmentService {

    public record AttachmentDownload(String filename, String contentType, long size, InputStream input) {
    }

    public record AttachmentData(String filename, String contentType, byte[] bytes) {
    }

    private final MailAttachmentRepository attachmentRepository;
    private final MailAccountRepository accountRepository;
    private final MailCryptoService cryptoService;
    private final ImapClient imapClient;
    private final AttachmentStorage storage;

    public MailAttachmentService(
            MailAttachmentRepository attachmentRepository,
            MailAccountRepository accountRepository,
            MailCryptoService cryptoService,
            ImapClient imapClient,
            AttachmentStorage storage
    ) {
        this.attachmentRepository = attachmentRepository;
        this.accountRepository = accountRepository;
        this.cryptoService = cryptoService;
        this.imapClient = imapClient;
        this.storage = storage;
    }

    public List<MailAttachment> list(long userId, long messageId) {
        return attachmentRepository.findByMessageAndUser(messageId, userId);
    }

    public void replaceForMessage(long messageId, List<ParsedAttachment> attachments) {
        attachmentRepository.replaceForMessage(messageId, attachments, storage.provider());
    }

    public void deleteByFolder(long accountId, String folder) {
        attachmentRepository.deleteByFolder(accountId, folder);
    }

    public AttachmentDownload download(long userId, long attachmentId) {
        MailAttachmentRepository.AttachmentSource source = attachmentRepository.findSource(attachmentId, userId)
                .orElseThrow(() -> new ApiException(404, "附件不存在"));
        MailAccount account = accountRepository.findByIdAndUser(source.accountId(), userId)
                .orElseThrow(() -> new ApiException(404, "邮箱账号不存在"));

        if (source.storageKey() != null && !source.storageKey().isBlank() && storage.exists(source.storageKey())) {
            return new AttachmentDownload(source.filename(), source.contentType(), source.size(),
                    storage.open(source.storageKey()));
        }

        String secret = cryptoService.decrypt(account.secretCipher());
        ImapClient.AttachmentContent content = imapClient.downloadAttachment(
                account, secret, source.folder(), source.uid(), source.partIndex()
        );
        String key = account.id() + "/" + source.messageId() + "/" + source.id() + "-" + safeName(content.filename());
        StoredObject stored = storage.save(
                key, new ByteArrayInputStream(content.bytes()), content.bytes().length, content.contentType()
        );
        attachmentRepository.updateStored(
                source.id(), stored.provider(), stored.key(), stored.size(), stored.checksum()
        );
        return new AttachmentDownload(content.filename(), content.contentType(), content.bytes().length,
                new ByteArrayInputStream(content.bytes()));
    }

    public AttachmentData readForSend(long userId, long attachmentId) {
        AttachmentDownload download = download(userId, attachmentId);
        try (InputStream input = download.input()) {
            return new AttachmentData(download.filename(), download.contentType(), input.readAllBytes());
        } catch (Exception exception) {
            throw new ApiException(500, "读取附件失败");
        }
    }

    public void deleteByAccount(long accountId) {
        attachmentRepository.deleteByAccount(accountId);
    }

    private String safeName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "attachment";
        }
        String value = filename.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_").trim();
        return value.length() > 120 ? value.substring(0, 120) : value;
    }
}
