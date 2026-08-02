package com.vaultx.bidding;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires running Kafka, PostgreSQL, and gRPC infrastructure")
class BiddingServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
