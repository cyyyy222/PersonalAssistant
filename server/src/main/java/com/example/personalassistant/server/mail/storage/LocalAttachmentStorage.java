package com.example.personalassistant.server.mail.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalAttachmentStorage implements AttachmentStorage {

    private final Path root;

    public LocalAttachmentStorage(@Value("${app.storage.local-root:./data/attachments}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public String provider() {
        return "local";
    }

    @Override
    public StoredObject save(String key, InputStream input, long declaredSize, String contentType) {
        try {
            Path target = resolve(key);
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long[] size = new long[]{0};
            try (DigestInputStream digestInput = new DigestInputStream(input, digest);
                 OutputStream output = Files.newOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = digestInput.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    size[0] += read;
                }
            }
            return new StoredObject(provider(), key, size[0],
                    HexFormat.of().formatHex(digest.digest()));
        } catch (Exception exception) {
            throw new IllegalStateException("保存附件失败", exception);
        }
    }

    @Override
    public InputStream open(String key) {
        try {
            return Files.newInputStream(resolve(key));
        } catch (IOException exception) {
            throw new IllegalStateException("读取附件失败", exception);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.isRegularFile(resolve(key));
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException exception) {
            throw new IllegalStateException("删除附件失败", exception);
        }
    }

    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法的附件存储路径");
        }
        return target;
    }
}
