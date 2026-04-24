package com.nexus.platform.service;

import com.nexus.platform.config.PlatformEmailProperties;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VerificationCodeEmailService {
    private final JavaMailSender mailSender;
    private final PlatformEmailProperties emailProperties;

    public boolean isEnabled() {
        return emailProperties.isEnabled();
    }

    public void sendVerificationCode(String email, String purpose, String code) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(email);
            helper.setSubject(subjectForPurpose(purpose));
            helper.setText(buildHtmlBody(purpose, code), true);
            helper.setFrom(new InternetAddress(
                    emailProperties.getFromAddress(),
                    emailProperties.getFromName(),
                    StandardCharsets.UTF_8.name()
            ).toString());
            mailSender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send verification email", e);
        }
    }

    private String subjectForPurpose(String purpose) {
        return switch (purpose) {
            case "REGISTER" -> "Nexus Platform 註冊驗證碼";
            case "CHANGE_PASSWORD" -> "Nexus Platform 修改密碼驗證碼";
            default -> "Nexus Platform 重設密碼驗證碼";
        };
    }

    private String buildHtmlBody(String purpose, String code) {
        String action = switch (purpose) {
            case "REGISTER" -> "完成註冊";
            case "CHANGE_PASSWORD" -> "修改密碼";
            default -> "重設密碼";
        };
        return """
                <div style="font-family:Arial,'PingFang SC','Microsoft YaHei',sans-serif;color:#1f2937;line-height:1.6">
                  <h2 style="margin:0 0 16px">Nexus Platform 郵箱驗證</h2>
                  <p style="margin:0 0 12px">你正在進行%s，請使用以下驗證碼：</p>
                  <div style="display:inline-block;padding:12px 20px;background:#eff6ff;border:1px solid #bfdbfe;border-radius:12px;font-size:28px;font-weight:700;letter-spacing:6px;color:#1d4ed8">
                    %s
                  </div>
                  <p style="margin:16px 0 0">驗證碼 5 分鐘內有效，請勿透露給他人。</p>
                  <p style="margin:8px 0 0;color:#6b7280;font-size:13px">如果這不是你的操作，請忽略這封郵件。</p>
                </div>
                """.formatted(action, code);
    }
}
