package com.vaultx.assistant.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultx.assistant.config.AssistantProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Read-only client for the Vaultx APIs. Requests go through the API Gateway so
 * the caller's JWT (when provided) is honoured for authenticated endpoints.
 */
@Component
public class GatewayClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public GatewayClient(RestClient.Builder builder, AssistantProperties props, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl(props.getGatewayUrl()).build();
    }

    public List<Map<String, Object>> listAuctions() {
        return getList("/api/auctions");
    }

    public Map<String, Object> getAuction(String auctionId) {
        return getMap("/api/auctions/" + auctionId);
    }

    public List<Map<String, Object>> getBids(String auctionId) {
        return getList("/api/auctions/" + auctionId + "/bids");
    }

    public Map<String, Object> getWallet(String authorization) {
        return restClient.get().uri("/api/wallet")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);
    }

    private List<Map<String, Object>> getList(String uri) {
        JsonNode node = parse(restClient.get().uri(uri).accept(MediaType.APPLICATION_JSON).retrieve().body(String.class));
        return objectMapper.convertValue(node, List.class);
    }

    private Map<String, Object> getMap(String uri) {
        JsonNode node = parse(restClient.get().uri(uri).accept(MediaType.APPLICATION_JSON).retrieve().body(String.class));
        return objectMapper.convertValue(node, Map.class);
    }

    private JsonNode parse(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Vaultx API response", e);
        }
    }
}
