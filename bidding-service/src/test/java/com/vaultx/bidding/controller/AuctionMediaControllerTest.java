package com.vaultx.bidding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultx.bidding.dto.AuctionMediaResponse;
import com.vaultx.bidding.dto.MediaUploadRequest;
import com.vaultx.bidding.dto.PresignResponse;
import com.vaultx.bidding.model.AuctionMedia.AuctionMediaType;
import com.vaultx.bidding.security.JwtAuthenticationFilter;
import com.vaultx.bidding.security.JwtTokenProvider;
import com.vaultx.bidding.service.AuctionMediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuctionMediaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuctionMediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuctionMediaService mediaService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final UUID testSellerId = UUID.randomUUID();
    private final UUID auctionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testSellerId, null, List.of()));
    }

    @Test
    void createUpload_ShouldReturn201() throws Exception {
        MediaUploadRequest request = buildUploadRequest();
        PresignResponse response = buildPresignResponse();

        when(mediaService.create(eq(auctionId), eq(testSellerId), any(MediaUploadRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/auctions/{id}/media", auctionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaId").value(response.getMediaId().toString()))
                .andExpect(jsonPath("$.uploadUrl").value(response.getUploadUrl()));
    }

    @Test
    void createUpload_MissingFileName_ShouldReturn400() throws Exception {
        MediaUploadRequest request = buildUploadRequest();
        request.setFileName("");

        mockMvc.perform(post("/api/auctions/{id}/media", auctionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_ShouldReturn200() throws Exception {
        AuctionMediaResponse media = buildMediaResponse();
        when(mediaService.list(auctionId)).thenReturn(List.of(media));

        mockMvc.perform(get("/api/auctions/{id}/media", auctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(media.getId().toString()))
                .andExpect(jsonPath("$[0].mediaType").value("IMAGE"));
    }

    @Test
    void complete_ShouldReturn200() throws Exception {
        UUID mediaId = UUID.randomUUID();
        AuctionMediaResponse media = buildMediaResponse();
        media.setId(mediaId);
        media.setStatus("UPLOADED");

        when(mediaService.complete(eq(auctionId), eq(testSellerId), eq(mediaId)))
                .thenReturn(media);

        mockMvc.perform(post("/api/auctions/{id}/media/{mediaId}/complete", auctionId, mediaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"));
    }

    @Test
    void setCover_ShouldReturn200() throws Exception {
        UUID mediaId = UUID.randomUUID();
        AuctionMediaResponse media = buildMediaResponse();
        media.setId(mediaId);
        media.setCover(true);

        when(mediaService.setCover(eq(auctionId), eq(testSellerId), eq(mediaId)))
                .thenReturn(media);

        mockMvc.perform(put("/api/auctions/{id}/media/{mediaId}/cover", auctionId, mediaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cover").value(true));
    }

    @Test
    void delete_ShouldReturn204() throws Exception {
        UUID mediaId = UUID.randomUUID();

        mockMvc.perform(delete("/api/auctions/{id}/media/{mediaId}", auctionId, mediaId))
                .andExpect(status().isNoContent());
    }

    private MediaUploadRequest buildUploadRequest() {
        MediaUploadRequest request = new MediaUploadRequest();
        request.setContentType("image/jpeg");
        request.setFileName("photo.jpg");
        request.setFileSizeBytes(10L * 1024 * 1024);
        return request;
    }

    private PresignResponse buildPresignResponse() {
        return PresignResponse.builder()
                .mediaId(UUID.randomUUID())
                .auctionId(auctionId)
                .mediaType(AuctionMediaType.IMAGE)
                .objectKey("auctions/" + auctionId + "/a.jpg")
                .contentType("image/jpeg")
                .sizeBytes(10L * 1024 * 1024)
                .uploadUrl("http://localhost:4566/vaultx-media/auctions/" + auctionId + "/a.jpg")
                .headers(Map.of("Content-Type", "image/jpeg"))
                .expiresInSeconds(900)
                .build();
    }

    private AuctionMediaResponse buildMediaResponse() {
        return AuctionMediaResponse.builder()
                .id(UUID.randomUUID())
                .auctionId(auctionId)
                .mediaType(AuctionMediaType.IMAGE)
                .contentType("image/jpeg")
                .sizeBytes(10L * 1024 * 1024)
                .url("http://localhost:4566/vaultx-media/auctions/" + auctionId + "/a.jpg")
                .cover(false)
                .sortOrder(0)
                .status("UPLOADED")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
