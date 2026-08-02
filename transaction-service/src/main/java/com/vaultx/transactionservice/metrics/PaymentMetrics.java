package com.vaultx.transactionservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PaymentMetrics {

    private final MeterRegistry meterRegistry;

    public PaymentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordPaymentCompleted() {
        Counter.builder("vaultx.payments.completed")
                .description("Total number of payments completed")
                .register(meterRegistry)
                .increment();
    }

    public void recordPaymentFailed() {
        Counter.builder("vaultx.payments.failed")
                .description("Total number of payments failed")
                .register(meterRegistry)
                .increment();
    }

    public void recordEscrowReleased() {
        Counter.builder("vaultx.escrows.released")
                .description("Total number of escrows released")
                .register(meterRegistry)
                .increment();
    }

    public void recordEscrowRefunded() {
        Counter.builder("vaultx.escrows.refunded")
                .description("Total number of escrows refunded")
                .register(meterRegistry)
                .increment();
    }
}
