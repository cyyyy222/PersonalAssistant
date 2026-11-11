package com.example.personalassistant.server.mail;

public record MailSyncState(
        long id,
        long accountId,
        String folder,
        long uidValidity,
        long lastUid,
        int status,
        String errorMessage
) {
    public static MailSyncState empty(long accountId, String folder) {
        return new MailSyncState(0, accountId, folder, 0, 0, 0, null);
    }
}
