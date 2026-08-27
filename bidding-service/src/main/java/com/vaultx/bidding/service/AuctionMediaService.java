package com.vaultx.bidding.service;

import com.vaultx.bidding.config.MediaTypeValidator;
import com.vaultx.bidding.config.StorageProperties;
import com.vaultx.bidding.dto.AuctionMediaResponse;
import com.vaultx.bidding.dto.MediaUploadRequest;
import com.vaultx.bidding.dto.PresignResponse;
import com.vaultx.bidding.exception.MediaAccessDeniedException;
import com.vaultx.bidding.exception.MediaNotFoundException;
import com.vaultx.bidding.exception.MediaValidationException;
import com.vaultx.bidding.model.Auction;
import com.vaultx.bidding.model.AuctionMedia;
import com.vaultx.bidding.model.AuctionMedia.AuctionMediaType;
import com.vaultx.bidding.repository.AuctionMediaRepository;
import com.vaultx.bidding.repository.AuctionRepository;
import com.vaultx.bidding.util.ObjectKeyBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuctionMediaService {

    private static final int MAGIC_BYTES = 16;

    private final AuctionMediaRepository mediaRepository;
    private final AuctionRepository auctionRepository;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties props;

    @Transactional
    public PresignResponse create(UUID auctionId, UUID sellerId, MediaUploadRequest req) {
        requireOwnedAuction(auctionId, sellerId);

        AuctionMediaType type = MediaTypeValidator.resolveMediaType(req.getContentType(), req.getFileName());
        MediaTypeValidator.assertExtensionMatchesContentType(req.getContentType(), req.getFileName());

        long limit = type == AuctionMediaType.IMAGE
                ? props.getMaxImageBytes() : props.getMaxVideoBytes();
        if (req.getFileSizeBytes() > limit) {
            throw new MediaValidationException("File exceeds max size of " + limit + " bytes for " + type);
        }

        int maxCount = type == AuctionMediaType.IMAGE
                ? props.getMaxImagesPerAuction() : props.getMaxVideosPerAuction();
        long existing = mediaRepository.countByAuctionIdAndMediaTypeAndStatus(auctionId, type, "UPLOADED");
        if (existing >= maxCount) {
            throw new MediaValidationException("Maximum of " + maxCount + " " + type + " per auction reached");
        }

        String objectKey = ObjectKeyBuilder.build(auctionId, req.getFileName());

        AuctionMedia media = new AuctionMedia();
        media.setAuctionId(auctionId);
        media.setMediaType(type);
        media.setObjectKey(objectKey);
        media.setContentType(req.getContentType());
        media.setSizeBytes(req.getFileSizeBytes());
        media.setStatus("PENDING");
        AuctionMedia saved = mediaRepository.save(media);

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(p -> p
                .signatureDuration(Duration.ofSeconds(props.getPresignTtlSeconds()))
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(props.getBucket())
                        .key(objectKey)
                        .contentType(req.getContentType())
                        .build()));

        return PresignResponse.builder()
                .mediaId(saved.getId())
                .auctionId(auctionId)
                .mediaType(type)
                .objectKey(objectKey)
                .contentType(req.getContentType())
                .sizeBytes(req.getFileSizeBytes())
                .uploadUrl(presigned.url().toString())
                .headers(Map.of("Content-Type", req.getContentType()))
                .expiresInSeconds(props.getPresignTtlSeconds())
                .build();
    }

    @Transactional
    public AuctionMediaResponse complete(UUID auctionId, UUID sellerId, UUID mediaId) {
        AuctionMedia media = requireOwnedMedia(auctionId, sellerId, mediaId);

        HeadObjectResponse head = s3Client.headObject(b -> b.bucket(props.getBucket()).key(media.getObjectKey()));
        if (head.contentLength() != media.getSizeBytes()) {
            throw new MediaValidationException("Uploaded size does not match expected " + media.getSizeBytes());
        }
        MediaTypeValidator.assertMagicBytes(media.getContentType(), readFirstBytes(media.getObjectKey()));

        media.setStatus("UPLOADED");
        media.setContentType(head.contentType() != null ? head.contentType() : media.getContentType());
        media.setSizeBytes(head.contentLength());

        if (media.getMediaType() == AuctionMediaType.IMAGE
                && mediaRepository.findFirstByAuctionIdAndCoverTrue(auctionId).isEmpty()) {
            media.setCover(true);
        }
        return toResponse(mediaRepository.save(media));
    }

    @Transactional(readOnly = true)
    public List<AuctionMediaResponse> list(UUID auctionId) {
        return mediaRepository.findByAuctionIdOrderBySortOrderAsc(auctionId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public AuctionMediaResponse setCover(UUID auctionId, UUID sellerId, UUID mediaId) {
        AuctionMedia target = requireOwnedMedia(auctionId, sellerId, mediaId);
        if (target.getMediaType() != AuctionMediaType.IMAGE) {
            throw new MediaValidationException("Only an image can be set as cover");
        }
        mediaRepository.findByAuctionIdOrderBySortOrderAsc(auctionId).forEach(m -> m.setCover(false));
        target.setCover(true);
        return toResponse(mediaRepository.save(target));
    }

    @Transactional
    public void delete(UUID auctionId, UUID sellerId, UUID mediaId) {
        AuctionMedia media = requireOwnedMedia(auctionId, sellerId, mediaId);
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(props.getBucket()).key(media.getObjectKey()).build());
        mediaRepository.delete(media);
    }

    private void requireOwnedAuction(UUID auctionId, UUID sellerId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new MediaNotFoundException("Auction not found: " + auctionId));
        if (!auction.getSellerId().equals(sellerId)) {
            throw new MediaAccessDeniedException("Only the seller can modify this auction's media");
        }
    }

    private AuctionMedia requireOwnedMedia(UUID auctionId, UUID sellerId, UUID mediaId) {
        requireOwnedAuction(auctionId, sellerId);
        AuctionMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found: " + mediaId));
        if (!media.getAuctionId().equals(auctionId)) {
            throw new MediaNotFoundException("Media not found for auction: " + auctionId);
        }
        return media;
    }

    private byte[] readFirstBytes(String objectKey) {
        try (InputStream in = s3Client.getObject(b -> b
                        .bucket(props.getBucket()).key(objectKey)
                        .range("bytes=0-" + (MAGIC_BYTES - 1)),
                ResponseTransformer.toInputStream())) {
            return in.readNBytes(MAGIC_BYTES);
        } catch (Exception e) {
            throw new MediaNotFoundException("Could not read uploaded object: " + objectKey);
        }
    }

    private AuctionMediaResponse toResponse(AuctionMedia m) {
        return AuctionMediaResponse.builder()
                .id(m.getId())
                .auctionId(m.getAuctionId())
                .mediaType(m.getMediaType())
                .contentType(m.getContentType())
                .sizeBytes(m.getSizeBytes())
                .url(props.getPublicBaseUrl() + "/" + m.getObjectKey())
                .cover(m.isCover())
                .sortOrder(m.getSortOrder())
                .status(m.getStatus())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
