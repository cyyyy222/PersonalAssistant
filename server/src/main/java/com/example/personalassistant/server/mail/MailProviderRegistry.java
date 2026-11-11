package com.example.personalassistant.server.mail;

import com.example.personalassistant.server.common.ApiException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class MailProviderRegistry {

    private final Map<String, MailProvider> providers = Map.of(
            "qq.com", new MailProvider("qq.com", "imap.qq.com", 993, "smtp.qq.com", 465),
            "foxmail.com", new MailProvider("foxmail.com", "imap.qq.com", 993, "smtp.qq.com", 465),
            "163.com", new MailProvider("163.com", "imap.163.com", 993, "smtp.163.com", 465),
            "126.com", new MailProvider("126.com", "imap.126.com", 993, "smtp.126.com", 465)
    );

    public MailProvider resolve(String email) {
        String domain = domainOf(email);
        MailProvider provider = providers.get(domain);
        if (provider == null) {
            throw new ApiException(400, "暂不支持该邮箱域名");
        }
        return provider;
    }

    public String domainOf(String email) {
        if (email == null) {
            throw new ApiException(400, "请输入邮箱地址");
        }
        int at = email.lastIndexOf('@');
        if (at < 1 || at == email.length() - 1) {
            throw new ApiException(400, "邮箱地址格式不正确");
        }
        return email.substring(at + 1).trim().toLowerCase(Locale.ROOT);
    }
}
