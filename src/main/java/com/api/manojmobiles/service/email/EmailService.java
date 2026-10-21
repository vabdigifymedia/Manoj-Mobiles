package com.api.manojmobiles.service.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// =========================================================================
// [TEMPORARY - PINE LABS APPROVAL]
// Reason: TD Digital SMS credits exhausted; added for PG review team.
// Removal: Delete this class and corresponding endpoints once SMS is restored.
// =========================================================================
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Manoj Mobiles - Verification Code: " + otp);
            helper.setText(buildOtpHtml(otp), true);

            mailSender.send(message);
            log.info("OTP Email successfully sent to {}", maskEmail(toEmail));
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}. Error: {}", maskEmail(toEmail), e.getMessage());
        }
    }

    private String buildOtpHtml(String otp) {
        return "<div style=\"font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">"
                + "<h2 style=\"color: #1e293b; margin-top: 0; text-align: center;\">Manoj Mobiles</h2>"
                + "<p style=\"color: #475569; font-size: 15px; text-align: center;\">Use the following One-Time Password (OTP) to complete your verification:</p>"
                + "<div style=\"margin: 24px 0; padding: 16px; background-color: #f1f5f9; border-radius: 8px; text-align: center;\">"
                + "<span style=\"font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #0f172a;\">" + otp + "</span>"
                + "</div>"
                + "<p style=\"color: #64748b; font-size: 13px; text-align: center;\">This code is valid for 5 minutes. If you did not request this code, please ignore this email.</p>"
                + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />"
                + "<p style=\"color: #94a3b8; font-size: 12px; text-align: center; margin-bottom: 0;\">&copy; Manoj Mobiles. All rights reserved.</p>"
                + "</div>";
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "****";
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return "**" + email.substring(atIndex);
        return email.substring(0, 2) + "****" + email.substring(atIndex);
    }
}
