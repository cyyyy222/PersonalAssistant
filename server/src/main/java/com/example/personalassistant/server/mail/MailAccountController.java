package com.example.personalassistant.server.mail;

import com.example.personalassistant.server.common.ApiResponse;
import com.example.personalassistant.server.mail.MailDtos.AccountView;
import com.example.personalassistant.server.mail.MailDtos.BindRequest;
import com.example.personalassistant.server.mail.MailDtos.SyncResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mail/accounts")
public class MailAccountController {

    private final MailService mailService;

    public MailAccountController(MailService mailService) {
        this.mailService = mailService;
    }

    @GetMapping
    public ApiResponse<List<AccountView>> list(@RequestAttribute("userId") long userId) {
        return ApiResponse.ok(mailService.listAccounts(userId));
    }

    @PostMapping
    public ApiResponse<AccountView> bind(@RequestAttribute("userId") long userId,
                                         @Valid @RequestBody BindRequest request) {
        return ApiResponse.ok(mailService.bind(userId, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> unbind(@RequestAttribute("userId") long userId,
                                    @PathVariable long id) {
        mailService.unbind(userId, id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/sync")
    public ApiResponse<SyncResult> sync(@RequestAttribute("userId") long userId,
                                        @PathVariable long id) {
        return ApiResponse.ok(mailService.sync(userId, id));
    }
}
