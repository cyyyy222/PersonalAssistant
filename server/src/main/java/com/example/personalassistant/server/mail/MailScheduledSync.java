package com.example.personalassistant.server.mail;

import com.example.personalassistant.server.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MailScheduledSync {

    private static final Logger log = LoggerFactory.getLogger(MailScheduledSync.class);

    private final MailAccountRepository accountRepository;
    private final MailService mailService;

    public MailScheduledSync(MailAccountRepository accountRepository, MailService mailService) {
        this.accountRepository = accountRepository;
        this.mailService = mailService;
    }

    @Scheduled(fixedDelayString = "${app.mail.sync-interval-ms:600000}", initialDelayString = "30000")
    public void syncAllAccounts() {
        for (MailAccount account : accountRepository.findAll()) {
            try {
                mailService.syncAccount(account);
            } catch (ApiException exception) {
                if (exception.getStatus() != 409) {
                    log.warn("Scheduled mail sync failed for account {}: {}", account.id(), exception.getMessage());
                }
            } catch (Exception exception) {
                log.warn("Scheduled mail sync failed for account {}", account.id(), exception);
            }
        }
    }
}
