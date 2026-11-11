package com.example.personalassistant.server.mail;

import com.example.personalassistant.server.common.ApiException;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

@Component
public class SmtpClient {

    public record OutgoingMail(
            String to,
            String cc,
            String bcc,
            String subject,
            String body,
            boolean html,
            String inReplyTo,
            String references,
            List<MailAttachmentService.AttachmentData> attachments
    ) {
    }

    public record SentMail(String messageId, MimeMessage message) {
    }

    private final int connectionTimeoutMs;
    private final int readTimeoutMs;

    public SmtpClient(
            @Value("${app.mail.connection-timeout-ms:10000}") int connectionTimeoutMs,
            @Value("${app.mail.read-timeout-ms:15000}") int readTimeoutMs
    ) {
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public SentMail send(MailAccount account, String secret, OutgoingMail mail) {
        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.host", account.smtpHost());
            properties.put("mail.smtp.port", String.valueOf(account.smtpPort()));
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.ssl.enable", String.valueOf(account.smtpSsl()));
            properties.put("mail.smtp.connectiontimeout", String.valueOf(connectionTimeoutMs));
            properties.put("mail.smtp.timeout", String.valueOf(readTimeoutMs));
            properties.put("mail.smtp.writetimeout", String.valueOf(readTimeoutMs));
            properties.put("mail.smtp.ssl.checkserveridentity", "true");

            Session session = Session.getInstance(properties);
            MimeMessage message = new MimeMessage(session);

            String displayName = account.displayName() == null || account.displayName().isBlank()
                    ? account.email() : account.displayName();
            message.setFrom(new InternetAddress(account.email(), displayName, "UTF-8"));
            setRecipients(message, Message.RecipientType.TO, mail.to());
            setRecipients(message, Message.RecipientType.CC, mail.cc());
            setRecipients(message, Message.RecipientType.BCC, mail.bcc());
            message.setSubject(mail.subject(), "UTF-8");

            if (mail.inReplyTo() != null && !mail.inReplyTo().isBlank()) {
                message.setHeader("In-Reply-To", mail.inReplyTo());
                message.setHeader("References",
                        mail.references() == null || mail.references().isBlank()
                                ? mail.inReplyTo() : mail.references());
            }

            if (mail.attachments() == null || mail.attachments().isEmpty()) {
                if (mail.html()) {
                    message.setContent(mail.body(), "text/html; charset=UTF-8");
                } else {
                    message.setText(mail.body(), "UTF-8");
                }
            } else {
                MimeMultipart multipart = new MimeMultipart("mixed");
                MimeBodyPart bodyPart = new MimeBodyPart();
                if (mail.html()) {
                    bodyPart.setContent(mail.body(), "text/html; charset=UTF-8");
                } else {
                    bodyPart.setText(mail.body(), "UTF-8");
                }
                multipart.addBodyPart(bodyPart);
                for (MailAttachmentService.AttachmentData attachment : mail.attachments()) {
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    attachmentPart.setFileName(attachment.filename());
                    attachmentPart.setDataHandler(new jakarta.activation.DataHandler(
                            new ByteArrayDataSource(attachment.bytes(), attachment.contentType())
                    ));
                    multipart.addBodyPart(attachmentPart);
                }
                message.setContent(multipart);
            }

            Transport transport = session.getTransport("smtp");
            try {
                transport.connect(account.smtpHost(), account.smtpPort(), account.imapUsername(), secret);
                transport.sendMessage(message, message.getAllRecipients());
            } finally {
                if (transport.isConnected()) {
                    transport.close();
                }
            }
            return new SentMail(message.getMessageID() == null ? "" : message.getMessageID(), message);
        } catch (AuthenticationFailedException exception) {
            throw new ApiException(401, "发件邮箱授权码错误，或 SMTP 服务未开启");
        } catch (SendFailedException exception) {
            throw new ApiException(502, "邮件发送失败，请检查收件人地址");
        } catch (MessagingException exception) {
            Throwable cause = exception;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof UnknownHostException) {
                throw new ApiException(502, "无法解析发件服务器地址");
            }
            if (cause instanceof SocketTimeoutException) {
                throw new ApiException(504, "连接发件服务器超时");
            }
            throw new ApiException(502, "邮件发送失败，请稍后再试");
        } catch (Exception exception) {
            throw new ApiException(500, "邮件发送失败");
        }
    }

    private void setRecipients(MimeMessage message, Message.RecipientType type, String value)
            throws MessagingException {
        if (value == null || value.isBlank()) {
            return;
        }
        message.setRecipients(type, InternetAddress.parse(value, true));
    }
}
