package com.pm.notificationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class NotificationTemplateBuilder {

    public record Template(UUID recipientId, String eventType,
                           String title, String message, List<String> channels) {}

    public Template build(String topic, String key, JsonNode event) {
        String eventType = mapTopicToEventType(topic);
        if (eventType == null) {
            return null;
        }

        return switch (eventType) {
            case "USER_REGISTERED" -> new Template(
                    uuidOf(event, "userId"), eventType,
                    "Welcome to Vaultx!",
                    "Hi " + textOf(event, "username")
                            + ", your account is ready. Complete KYC to start bidding.",
                    List.of("EMAIL", "PUSH"));

            case "AUCTION_CREATED" -> new Template(
                    uuidOf(event, "sellerId"), eventType,
                    "Auction Created",
                    "Your auction '" + textOf(event, "title")
                            + "' is now pending. It goes live at " + textOf(event, "startTime"),
                    List.of("EMAIL", "PUSH"));

            case "AUCTION_STARTED" -> new Template(
                    uuidOf(event, "sellerId"), eventType,
                    "Auction Live",
                    "Your auction is now LIVE. Bidding ends at " + textOf(event, "endTime"),
                    List.of("PUSH"));

            case "BID_PLACED" -> new Template(
                    uuidOf(event, "bidderId"), eventType,
                    "Bid Placed",
                    "Your bid of " + money(event, "amount")
                            + " on auction " + textOf(event, "auctionId") + " is now winning.",
                    List.of("PUSH"));

            case "AUCTION_WON" -> new Template(
                    uuidOf(event, "winnerId"), eventType,
                    "You Won!",
                    "Congratulations! You won the auction for " + money(event, "finalBid")
                            + ". Complete checkout to claim it.",
                    List.of("EMAIL", "SMS", "PUSH"));

            case "AUCTION_LOST" -> new Template(
                    key != null && !key.isBlank() ? UUID.fromString(key) : uuidOf(event, "winnerId"),
                    eventType,
                    "Auction Ended",
                    "Your bid on auction " + textOf(event, "auctionId")
                            + " did not win. Bidding is now closed.",
                    List.of("PUSH"));

            case "PAYMENT_COMPLETED" -> new Template(
                    uuidOf(event, "sellerId"), eventType,
                    "Payment Received",
                    "Your auction sold for " + money(event, "amount")
                            + " and funds have been released to your wallet.",
                    List.of("EMAIL", "PUSH"));

            case "PAYMENT_FAILED" -> new Template(
                    uuidOf(event, "buyerId"), eventType,
                    "Payment Failed",
                    "Your payment for auction " + textOf(event, "auctionId")
                            + " failed. Please contact support.",
                    List.of("EMAIL"));

            case "NOTIFICATION_REQUESTED" -> new Template(
                    uuidOf(event, "userId"), eventType,
                    textOf(event, "title"),
                    textOf(event, "message"),
                    List.of("EMAIL", "SMS", "PUSH"));

            default -> null;
        };
    }

    private String mapTopicToEventType(String topic) {
        return switch (topic) {
            case "user.registered" -> "USER_REGISTERED";
            case "auction.created" -> "AUCTION_CREATED";
            case "auction.started" -> "AUCTION_STARTED";
            case "auction.ended" -> "AUCTION_ENDED";
            case "bid.placed" -> "BID_PLACED";
            case "auction.won" -> "AUCTION_WON";
            case "auction.lost" -> "AUCTION_LOST";
            case "payment.completed" -> "PAYMENT_COMPLETED";
            case "payment.failed" -> "PAYMENT_FAILED";
            case "notification.requested" -> "NOTIFICATION_REQUESTED";
            default -> null;
        };
    }

    private static String textOf(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null ? "" : value.asText();
    }

    private static UUID uuidOf(JsonNode node, String field) {
        return UUID.fromString(textOf(node, field));
    }

    private static String money(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return "0";
        return value.isNumber() ? value.decimalValue().toPlainString() : value.asText();
    }
}
