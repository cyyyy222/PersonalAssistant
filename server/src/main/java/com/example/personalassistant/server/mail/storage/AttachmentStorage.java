package com.example.personalassistant.server.mail.storage;

import java.io.InputStream;

public interface AttachmentStorage {

    String provider();

    StoredObject save(String key, InputStream input, long declaredSize, String contentType);

    InputStream open(String key);

    boolean exists(String key);

    void delete(String key);
}
