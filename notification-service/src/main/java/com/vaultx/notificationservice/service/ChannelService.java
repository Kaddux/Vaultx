package com.vaultx.notificationservice.service;

import com.vaultx.notificationservice.service.NotificationTemplateBuilder.Template;
import com.vaultx.user.grpc.UserProfile;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final JavaMailSender mailSender;

    @Value("${mail.from:no-reply@vaultx.com}")
    private String fromAddress;

    public boolean deliver(String channel, UserProfile profile, Template template) {
        try {
            switch (channel) {
                case "EMAIL" -> sendEmail(profile, template);
                case "SMS" -> {
                    log.info("[SIMULATED SMS] To: {} | {} - {}",
                            profile.getUserId(), template.title(), template.message());
                }
                case "PUSH" -> {
                    log.info("[SIMULATED PUSH] Device: {} | {} - {}",
                            profile.getUserId(), template.title(), template.message());
                }
                default -> throw new IllegalArgumentException("Unknown channel: " + channel);
            }
            return true;
        } catch (Exception e) {
            log.error("Channel delivery failed for {}: {}", channel, e.getMessage());
            return false;
        }
    }

    private void sendEmail(UserProfile profile, Template template) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(profile.getEmail());
        helper.setSubject(template.title());
        helper.setText(template.message(), true);
        mailSender.send(message);
        log.info("Email sent to {} | Subject: {}", profile.getEmail(), template.title());
    }
}
