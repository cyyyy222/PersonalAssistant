package com.example.personalassistant.server.mail;

import com.example.personalassistant.server.common.ApiResponse;
import com.example.personalassistant.server.mail.MailDtos.ArchiveResult;
import com.example.personalassistant.server.mail.MailDtos.MarkReadRequest;
import com.example.personalassistant.server.mail.MailDtos.MarkStarredRequest;
import com.example.personalassistant.server.mail.MailDtos.MessageDetail;
import com.example.personalassistant.server.mail.MailDtos.MessagePage;
import com.example.personalassistant.server.mail.MailDtos.SendRequest;
import com.example.personalassistant.server.mail.MailDtos.SendResult;
import com.example.personalassistant.server.mail.MailDtos.SyncResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mail")
public class MailMessageController {

    private final MailService mailService;

    public MailMessageController(MailService mailService) {
        this.mailService = mailService;
    }

    @GetMapping("/messages")
    public ApiResponse<MessagePage> list(
            @RequestAttribute("userId") long userId,
            @RequestParam long accountId,
            @RequestParam(defaultValue = "INBOX") String folder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Boolean starred,
            @RequestParam(required = false) Boolean archived
    ) {
        return ApiResponse.ok(mailService.listMessages(
                userId, accountId, folder, page, size, keyword, unreadOnly, starred, archived
        ));
    }

    @GetMapping("/messages/{id}")
    public ApiResponse<MessageDetail> detail(@RequestAttribute("userId") long userId,
                                             @PathVariable long id) {
        return ApiResponse.ok(mailService.getMessage(userId, id));
    }

    @PostMapping("/messages/{id}/read")
    public ApiResponse<Void> markRead(@RequestAttribute("userId") long userId,
                                      @PathVariable long id,
                                      @RequestBody MarkReadRequest request) {
        mailService.markRead(userId, id, request.read());
        return ApiResponse.ok(null);
    }

    @PostMapping("/messages/{id}/star")
    public ApiResponse<Void> markStarred(@RequestAttribute("userId") long userId,
                                         @PathVariable long id,
                                         @RequestBody MarkStarredRequest request) {
        mailService.markStarred(userId, id, request.starred());
        return ApiResponse.ok(null);
    }

    @PostMapping("/messages/{id}/archive")
    public ApiResponse<ArchiveResult> archive(@RequestAttribute("userId") long userId,
                                              @PathVariable long id) {
        return ApiResponse.ok(new ArchiveResult(mailService.archive(userId, id)));
    }

    @PostMapping("/sync-all")
    public ApiResponse<List<SyncResult>> syncAll(@RequestAttribute("userId") long userId) {
        return ApiResponse.ok(mailService.syncAll(userId));
    }

    @PostMapping("/send")
    public ApiResponse<SendResult> send(@RequestAttribute("userId") long userId,
                                        @Valid @RequestBody SendRequest request) {
        return ApiResponse.ok(mailService.send(userId, request));
    }
}
