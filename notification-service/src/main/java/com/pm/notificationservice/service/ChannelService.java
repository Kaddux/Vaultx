package com.pm.notificationservice.service;

import com.pm.notificationservice.service.NotificationTemplateBuilder.Template;
import com.vaultx.user.grpc.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChannelService {

    public boolean deliver(String channel, UserProfile profile, Template template) {
        try {
            switch (channel) {
                case "EMAIL" -> {
                    log.info("[SIMULATED EMAIL] To: {} | Subject: {} | Body: {}",
                            profile.getEmail(), template.title(), template.message());
                }
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
            // Simulate occasional delivery failure for demo/testing (2%)
            if (Math.random() < 0.02) {
                log.warn("[SIMULATED FAILURE] Channel {} could not be reached", channel);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Channel delivery failed for {}: {}", channel, e.getMessage());
            return false;
        }
    }
}
