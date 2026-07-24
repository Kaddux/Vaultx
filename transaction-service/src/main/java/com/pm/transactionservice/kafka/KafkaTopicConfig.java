package com.pm.transactionservice.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

//Replication Factor = 3 in production and Replication Factor = 1 in local dev environment
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

    // --- Main Topics ---

    @Bean
    public NewTopic bidPlacedTopic() {
        return new NewTopic("bid.placed", 6, (short) 1);
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
    public NewTopic auctionWonTopic() {
        return new NewTopic("auction.won", 6, (short) 1);
    }

    @Bean
    public NewTopic auctionLostTopic() {  // Changed from snake_case typo in architecture doc
        return new NewTopic("auction.lost", 6, (short) 1);
    }

    // --- Retry Topics (3 retry attempts) ---

    @Bean
    public NewTopic bidPlacedRetryTopic() {
        return new NewTopic("bid.placed.retry", 3, (short) 1);
    }

    @Bean
    public NewTopic auctionWonRetryTopic() {
        return new NewTopic("auction.won.retry", 3, (short) 1);
    }

    @Bean
    public NewTopic auctionEndedRetryTopic() {
        return new NewTopic("auction.ended.retry", 3, (short) 1);
    }

    // --- Dead Letter Topics ---

    @Bean
    public NewTopic bidPlacedDlqTopic() {
        return new NewTopic("bid.placed.dlq", 1, (short) 1);
    }

    @Bean
    public NewTopic auctionWonDlqTopic() {
        return new NewTopic("auction.won.dlq", 1, (short) 1);
    }

    @Bean
    public NewTopic auctionEndedDlqTopic() {
        return new NewTopic("auction.ended.dlq", 1, (short) 1);
    }
}
