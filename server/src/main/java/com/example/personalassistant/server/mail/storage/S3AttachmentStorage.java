package com.example.personalassistant.server.mail.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Object-storage placeholder for a later migration.
 *
 * <p>Keep using storage_key in mail_attachments; replacing this class with a real
 * S3/OSS/COS implementation must not change the mail API or Android client.</p>
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3AttachmentStorage implements AttachmentStorage {

    private final String bucket;
    private final String region;
    private final String endpoint;

    public S3AttachmentStorage(
            @Value("${app.storage.s3.bucket:}") String bucket,
            @Value("${app.storage.s3.region:}") String region,
            @Value("${app.storage.s3.endpoint:}") String endpoint
    ) {
        this.bucket = bucket;
        this.region = region;
        this.endpoint = endpoint;
    }

    @Override
    public String provider() {
        return "s3";
    }

    @Override
    public StoredObject save(String key, InputStream input, long declaredSize, String contentType) {
        // TODO: upload input to S3/OSS/COS and return the checksum.
        throw new UnsupportedOperationException(
                "S3 storage is not implemented yet. bucket=" + bucket + ", region=" + region + ", endpoint=" + endpoint
        );
    }

    @Override
    public InputStream open(String key) {
        // TODO: return the object stream.
        throw new UnsupportedOperationException("S3 storage is not implemented yet");
    }

    @Override
    public boolean exists(String key) {
        // TODO: HEAD object.
        throw new UnsupportedOperationException("S3 storage is not implemented yet");
    }

    @Override
    public void delete(String key) {
        // TODO: delete object.
        throw new UnsupportedOperationException("S3 storage is not implemented yet");
    }
}
