package com.vaultx.bidding.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class BiddingMetrics {

    private final MeterRegistry meterRegistry;

    private final Counter bidsPlaced;
    private final Counter bidsRejected;
    private final Counter auctionsCreated;
    private final Counter auctionsEnded;
    private final Timer bidLatency;
    private final Timer auctionLifecycleDuration;

    public BiddingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.bidsPlaced = Counter.builder("vaultx.bids.placed")
                .description("Total number of bids placed")
                .register(meterRegistry);
        this.bidsRejected = Counter.builder("vaultx.bids.rejected")
                .description("Total number of bids rejected")
                .register(meterRegistry);
        this.auctionsCreated = Counter.builder("vaultx.auctions.created")
                .description("Total number of auctions created")
                .register(meterRegistry);
        this.auctionsEnded = Counter.builder("vaultx.auctions.ended")
                .description("Total number of auctions ended")
                .tag("result", "all")
                .register(meterRegistry);
        this.bidLatency = Timer.builder("vaultx.bid.latency")
                .description("Latency of bid placement in milliseconds")
                .publishPercentileHistogram(true)
                .register(meterRegistry);
        this.auctionLifecycleDuration = Timer.builder("vaultx.auction.lifecycle.duration")
                .description("Duration of a full auction lifecycle in seconds")
                .register(meterRegistry);
    }

    public void recordBidPlaced() {
        bidsPlaced.increment();
    }

    public void recordBidRejected() {
        bidsRejected.increment();
    }

    public void recordAuctionCreated() {
        auctionsCreated.increment();
    }

    public void recordAuctionEnded(String result) {
        auctionsEnded.increment();
        meterRegistry.counter("vaultx.auctions.ended", "result", result).increment();
    }

    public void recordBidLatency(long startNanos) {
        bidLatency.record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
    }

    public void recordAuctionLifecycle(long startEpochSeconds, long endEpochSeconds) {
        auctionLifecycleDuration.record(endEpochSeconds - startEpochSeconds, TimeUnit.SECONDS);
    }
}
