package com.example.personalassistant.server.mail;

import com.example.personalassistant.server.common.ApiResponse;
import com.example.personalassistant.server.mail.MailDtos.AttachmentView;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/mail")
public class MailAttachmentController {

    private final MailAttachmentService attachmentService;

    public MailAttachmentController(MailAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping("/messages/{id}/attachments")
    public ApiResponse<List<AttachmentView>> list(@RequestAttribute("userId") long userId,
                                                  @PathVariable long id) {
        return ApiResponse.ok(attachmentService.list(userId, id).stream().map(AttachmentView::from).toList());
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<InputStreamResource> download(@RequestAttribute("userId") long userId,
                                                        @PathVariable long id) {
        MailAttachmentService.AttachmentDownload download = attachmentService.download(userId, id);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(download.contentType());
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(download.filename(), StandardCharsets.UTF_8)
                .build());
        if (download.size() > 0) {
            headers.setContentLength(download.size());
        }
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(new InputStreamResource(download.input()));
    }
}
