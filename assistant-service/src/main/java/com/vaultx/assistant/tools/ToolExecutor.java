package com.vaultx.assistant.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.vaultx.assistant.config.AssistantProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Executes the read-only Vaultx tools for the LLM and returns a plain-text/digest
 * summary the model can reason over. Money stays in USD.
 */
@Component
public class ToolExecutor {

    private final GatewayClient client;

    @Autowired
    public ToolExecutor(GatewayClient client) {
        this.client = client;
    }

    /** OpenAI-style tool definitions for the LLM. */
    public List<Map<String, Object>> toolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(function("list_auctions", "Search auctions. Keyword-matches title/description; optional max price and status filter.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "keywords", Map.of("type", "string", "description", "Comma/space separated keywords, e.g. 'gaming laptop'"),
                                "maxPrice", Map.of("type", "number", "description", "Max current bid (USD)"),
                                "status", Map.of("type", "string", "enum", List.of("ACTIVE", "PENDING", "SOLD", "UNSOLD", "AWAITING_PAYMENT"))
                        ),
                        "required", List.of()
                )));
        tools.add(function("get_auction", "Get details for a specific auction by id.",
                Map.of("type", "object", "properties", Map.of("auctionId", Map.of("type", "string")), "required", List.of("auctionId"))));
        tools.add(function("get_auction_bids", "Get bidding summary for an auction (bid count, distinct bidders, top bid, time remaining).",
                Map.of("type", "object", "properties", Map.of("auctionId", Map.of("type", "string")), "required", List.of("auctionId"))));
        tools.add(function("get_wallet", "Get the current user's wallet balance (USD).",
                Map.of("type", "object", "properties", Map.of(), "required", List.of())));
        return tools;
    }

    /** Executes a tool call; returns a JSON string (or human text) to feed back to the LLM. */
    public Object execute(String name, Map<String, Object> args, String authorization) {
        return switch (name) {
            case "list_auctions" -> listAuctions(args);
            case "get_auction" -> client.getAuction(str(args, "auctionId"));
            case "get_auction_bids" -> auctionBids(str(args, "auctionId"));
            case "get_wallet" -> client.getWallet(authorization);
            default -> Map.of("error", "Unknown tool: " + name);
        };
    }

    private Object listAuctions(Map<String, Object> args) {
        List<Map<String, Object>> auctions = client.listAuctions();
        String keywords = args.get("keywords") == null ? null : args.get("keywords").toString();
        Double maxPrice = number(args, "maxPrice");
        String status = args.get("status") == null ? null : args.get("status").toString();

        List<Map<String, Object>> filtered = auctions.stream()
                .filter(a -> matchesKeywords(a, keywords))
                .filter(a -> status == null || status.equalsIgnoreCase(str(a, "status")))
                .filter(a -> maxPrice == null || price(a) <= maxPrice)
                .collect(Collectors.toList());

        List<Map<String, Object>> summary = new ArrayList<>();
        for (Map<String, Object> a : filtered) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("id", a.get("id"));
            s.put("title", a.get("title"));
            s.put("description", a.get("description"));
            s.put("currentBid", a.get("currentBid"));
            s.put("startingPrice", a.get("startingPrice"));
            s.put("status", a.get("status"));
            s.put("endTime", a.get("endTime"));
            s.put("currency", a.get("currency"));
            summary.add(s);
        }
        return Map.of("count", summary.size(), "results", summary);
    }

    private Map<String, Object> auctionBids(String auctionId) {
        Map<String, Object> auction = client.getAuction(auctionId);
        List<Map<String, Object>> bids = client.getBids(auctionId);
        long distinctBidders = bids.stream().map(b -> str(b, "bidderId")).filter(id -> !id.isBlank()).distinct().count();
        Double topBid = bids.stream().mapToDouble(this::price).max().orElse(0.0);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("auctionId", auctionId);
        res.put("title", auction.get("title"));
        res.put("currentBid", auction.get("currentBid"));
        res.put("status", auction.get("status"));
        res.put("bidCount", bids.size());
        res.put("distinctBidders", distinctBidders);
        res.put("topBid", topBid);
        res.put("timeRemainingMinutes", timeRemainingMinutes(str(auction, "endTime")));
        return res;
    }

    private boolean matchesKeywords(Map<String, Object> a, String keywords) {
        if (keywords == null || keywords.isBlank()) return true;
        String text = (str(a, "title") + " " + str(a, "description")).toLowerCase();
        for (String kw : keywords.split("[,\\s]+")) {
            if (!kw.isBlank() && !text.contains(kw.toLowerCase())) return false;
        }
        return true;
    }

    private double price(Map<String, Object> m) {
        Object v = m.get("currentBid");
        if (v == null) v = m.get("startingPrice");
        return v == null ? 0.0 : Double.parseDouble(v.toString());
    }

    private long timeRemainingMinutes(String endTime) {
        if (endTime == null || endTime.isBlank()) return 0;
        try {
            LocalDateTime end = LocalDateTime.parse(endTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return Math.max(0, Duration.between(LocalDateTime.now(ZoneOffset.UTC), end).toMinutes());
        } catch (Exception e) {
            try {
                OffsetDateTime end = OffsetDateTime.parse(endTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                return Math.max(0, Duration.between(java.time.Instant.now(), end.toInstant()).toMinutes());
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    private Map<String, Object> function(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> fn = new LinkedHashMap<>();
        fn.put("name", name);
        fn.put("description", description);
        fn.put("parameters", parameters);
        return Map.of("type", "function", "function", fn);
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "" : v.toString();
    }

    private Double number(Map<String, Object> args, String k) {
        Object v = args.get(k);
        if (v == null) return null;
        return v instanceof Number n ? n.doubleValue() : Double.parseDouble(v.toString());
    }
}
