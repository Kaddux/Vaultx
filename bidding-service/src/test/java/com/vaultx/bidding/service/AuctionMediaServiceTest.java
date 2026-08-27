package com.vaultx.bidding.service;

import com.vaultx.bidding.config.StorageProperties;
import com.vaultx.bidding.dto.AuctionMediaResponse;
import com.vaultx.bidding.dto.MediaUploadRequest;
import com.vaultx.bidding.dto.PresignResponse;
import com.vaultx.bidding.exception.MediaAccessDeniedException;
import com.vaultx.bidding.exception.MediaValidationException;
import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.model.AuctionMedia;
import com.vaultx.bidding.model.AuctionMedia.AuctionMediaType;
import com.vaultx.bidding.repository.AuctionMediaRepository;
import com.vaultx.bidding.repository.AuctionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionMediaServiceTest {

    @Mock
    private AuctionMediaRepository mediaRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private StorageProperties storageProperties;

    @InjectMocks
    private AuctionMediaService mediaService;

    @Test
    void create_success_returnsPresignedUpload() throws Exception {
        UUID auctionId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Auction auction = buildAuction(auctionId, sellerId);
        MediaUploadRequest request = buildImageRequest();
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(java.net.URI.create("http://localhost:4566/vaultx-media/" + auctionId + "/a.jpg").toURL());
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(mediaRepository.countByAuctionIdAndMediaTypeAndStatus(any(), any(), eq("UPLOADED")))
                .thenReturn(0L);
        when(mediaRepository.save(any(AuctionMedia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(storageProperties.getMaxImageBytes()).thenReturn(10L * 1024 * 1024);
        when(storageProperties.getMaxImagesPerAuction()).thenReturn(10);
        when(s3Presigner.presignPutObject(any(java.util.function.Consumer.class))).thenReturn(presigned);

        PresignResponse response = mediaService.create(auctionId, sellerId, request);

        assertNotNull(response);
        assertEquals(auctionId, response.getAuctionId());
        assertEquals(AuctionMediaType.IMAGE, response.getMediaType());
        assertEquals("image/jpeg", response.getContentType());
        assertTrue(response.getObjectKey().startsWith("auctions/" + auctionId + "/"));
        assertEquals("http://localhost:4566/vaultx-media/" + auctionId + "/a.jpg", response.getUploadUrl());
        assertEquals(10L * 1024 * 1024, response.getSizeBytes());
        verify(s3Presigner).presignPutObject(any(java.util.function.Consumer.class));
    }

    @Test
    void create_denied_whenNotSeller() {
        UUID auctionId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Auction auction = buildAuction(auctionId, UUID.randomUUID());

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        assertThrows(MediaAccessDeniedException.class,
                () -> mediaService.create(auctionId, sellerId, buildImageRequest()));
    }

    @Test
    void create_oversizeImage_throws() {
        UUID auctionId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Auction auction = buildAuction(auctionId, sellerId);
        MediaUploadRequest request = buildImageRequest();
        request.setFileSizeBytes(2L * 1024 * 1024 * 1024);

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        assertThrows(MediaValidationException.class,
                () -> mediaService.create(auctionId, sellerId, request));
    }

    @Test
    void create_unsupportedContentType_throws() {
        UUID auctionId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Auction auction = buildAuction(auctionId, sellerId);
        MediaUploadRequest request = buildImageRequest();
        request.setContentType("text/html");
        request.setFileName("malware.html");

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        assertThrows(MediaValidationException.class,
                () -> mediaService.create(auctionId, sellerId, request));
    }

    @Test
    void create_exceedsImageCount_throws() {
        UUID auctionId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Auction auction = buildAuction(auctionId, sellerId);

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(mediaRepository.countByAuctionIdAndMediaTypeAndStatus(any(), any(), eq("UPLOADED")))
                .thenReturn(10L);
        when(storageProperties.getMaxImageBytes()).thenReturn(10L * 1024 * 1024);
        when(storageProperties.getMaxImagesPerAuction()).thenReturn(10);

        assertThrows(MediaValidationException.class,
                () -> mediaService.create(auctionId, sellerId, buildImageRequest()));
    }

    @Test
    void complete_success_marksUploadedAndSetsCover() {
        UUID auctionId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        Auction auction = buildAuction(auctionId, sellerId);
        AuctionMedia media = new AuctionMedia();
        media.setId(mediaId);
        media.setAuctionId(auctionId);
        media.setMediaType(AuctionMediaType.IMAGE);
        media.setObjectKey("auctions/" + auctionId + "/" + mediaId + ".jpg");
        media.setContentType("image/jpeg");
        media.setSizeBytes(4);
        media.setStatus("PENDING");

        byte[] jpegMagic = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        GetObjectResponse getResp = GetObjectResponse.builder().build();
        ResponseInputStream<GetObjectResponse> stream =
                new ResponseInputStream<>(getResp, AbortableInputStream.create(new ByteArrayInputStream(jpegMagic)));

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media));
        when(s3Client.headObject(any(java.util.function.Consumer.class))).thenReturn(HeadObjectResponse.builder()
                .contentLength(4L).contentType("image/jpeg").build());
        when(s3Client.getObject(any(java.util.function.Consumer.class), any(software.amazon.awssdk.core.sync.ResponseTransformer.class)))
                .thenReturn(stream);
        when(mediaRepository.findFirstByAuctionIdAndCoverTrue(auctionId)).thenReturn(Optional.empty());
        when(mediaRepository.save(any(AuctionMedia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(storageProperties.getPublicBaseUrl()).thenReturn("http://localhost:4566/vaultx-media");

        AuctionMediaResponse response = mediaService.complete(auctionId, sellerId, mediaId);

        assertEquals("UPLOADED", response.getStatus());
        assertTrue(response.isCover());
        assertTrue(response.getUrl().startsWith("http://localhost:4566/vaultx-media/"));
    }

    private MediaUploadRequest buildImageRequest() {
        MediaUploadRequest request = new MediaUploadRequest();
        request.setContentType("image/jpeg");
        request.setFileName("photo.jpg");
        request.setFileSizeBytes(10L * 1024 * 1024);
        return request;
    }

    private Auction buildAuction(UUID id, UUID sellerId) {
        Auction auction = new Auction();
        auction.setId(id);
        auction.setSellerId(sellerId);
        auction.setTitle("Test");
        auction.setStartingPrice(new BigDecimal("100.00"));
        auction.setBidIncrement(new BigDecimal("1.00"));
        auction.setStatus("PENDING");
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(LocalDateTime.now().plusDays(1));
        return auction;
    }
}
