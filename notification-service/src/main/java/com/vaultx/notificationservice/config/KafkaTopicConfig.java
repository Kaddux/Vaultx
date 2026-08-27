package com.vaultx.notificationservice.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic userRegisteredTopic() {
        return new NewTopic("user.registered", 6, (short) 1);
    }

    @Bean
    public NewTopic auctionCreatedTopic() {
        return new NewTopic("auction.created", 6, (short) 1);
    }

    @Bean
    public NewTopic auctionStartedTopic() {
        return new NewTopic("auction.started", 6, (short) 1);
    }

    @Bean
    public NewTopic auctionEndedTopic() {
        return new NewTopic("auction.ended", 6, (short) 1);
    }

    @Bean
    public NewTopic bidPlacedTopic() {
        return new NewTopic("bid.placed", 6, (short) 1);
    }

    @Bean
    public NewTopic auctionWonTopic() {
        return new NewTopic("auction.won", 6, (short) 1);
    }

    @Bean
    public NewTopic auctionLostTopic() {
        return new NewTopic("auction.lost", 6, (short) 1);
    }

    @Bean
    public NewTopic paymentCompletedTopic() {
        return new NewTopic("payment.completed", 6, (short) 1);
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return new NewTopic("payment.failed", 6, (short) 1);
    }

    @Bean
    public NewTopic notificationRequestedTopic() {
        return new NewTopic("notification.requested", 6, (short) 1);
    }

    @Bean
    public NewTopic kycSubmittedTopic() {
        return new NewTopic("kyc.submitted", 6, (short) 1);
    }
}
