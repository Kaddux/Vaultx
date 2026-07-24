package com.vaultx.bidding.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DltHandler {

    public void handleDlt(ConsumerRecord<?, ?> record) {
        log.error(
                "DLQ message: topic={}, partition={}, offset={}, key={}, value={}",
                record.topic(), record.partition(), record.offset(),
                record.key(), record.value());
    }
}
