package com.pm.notificationservice.controller;

import com.pm.notificationservice.dto.NotificationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationRequestController {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestNotification(
            @Valid @RequestBody NotificationRequest request) {

        Map<String, Object> payload = Map.of(
                "eventType", "NOTIFICATION_REQUESTED",
                "userId", request.getUserId().toString(),
                "title", request.getTitle(),
                "message", request.getMessage()
        );

        kafkaTemplate.send("notification.requested", request.getUserId().toString(), payload)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published notification.requested for userId={} (offset={})",
                                request.getUserId(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish notification.requested for userId={}",
                                request.getUserId(), ex);
                    }
                });

        return ResponseEntity.accepted().body(Map.of(
                "status", "QUEUED",
                "requestId", UUID.randomUUID().toString()
        ));
    }
}
