package com.vaultx.bidding.config;

import com.vaultx.bidding.exception.MediaValidationException;
import com.vaultx.bidding.model.AuctionMedia.AuctionMediaType;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validates media MIME types (whitelist), extension consistency and content by
 * inspecting the object's magic bytes (file signature sniffing) rather than
 * trusting the client-supplied Content-Type header.
 */
public final class MediaTypeValidator {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/quicktime", "video/webm");

    private static final Map<String, String> EXT_TO_TYPE = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"),
            Map.entry("gif", "image/gif"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("webm", "video/webm"));

    private MediaTypeValidator() {}

    public static AuctionMediaType resolveMediaType(String contentType, String fileName) {
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (IMAGE_TYPES.contains(ct)) return AuctionMediaType.IMAGE;
        if (VIDEO_TYPES.contains(ct)) return AuctionMediaType.VIDEO;

        String mapped = EXT_TO_TYPE.get(extension(fileName));
        if (mapped != null) {
            if (IMAGE_TYPES.contains(mapped)) return AuctionMediaType.IMAGE;
            if (VIDEO_TYPES.contains(mapped)) return AuctionMediaType.VIDEO;
        }
        throw new MediaValidationException("Unsupported media type: " + contentType);
    }

    public static void assertExtensionMatchesContentType(String contentType, String fileName) {
        String ext = extension(fileName);
        String mapped = EXT_TO_TYPE.get(ext);
        if (mapped == null || !mapped.equals(contentType.toLowerCase(Locale.ROOT))) {
            throw new MediaValidationException(
                    "File extension '" + ext + "' does not match content type '" + contentType + "'");
        }
    }

    public static void assertMagicBytes(String contentType, byte[] magic) {
        String ct = contentType.toLowerCase(Locale.ROOT);
        boolean ok = switch (ct) {
            case "image/jpeg" -> startsWith(magic, new int[]{0xFF, 0xD8, 0xFF});
            case "image/png" -> startsWith(magic, new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "image/webp" -> startsWith(magic, new int[]{0x52, 0x49, 0x46, 0x46}) && offset(magic, 8) == 0x57454250;
            case "image/gif" -> startsWith(magic, new int[]{0x47, 0x49, 0x46});
            case "video/mp4", "video/quicktime" -> offset(magic, 4) == 0x66747970;
            case "video/webm" -> startsWith(magic, new int[]{0x1A, 0x45, 0xDF, 0xA3});
            default -> false;
        };
        if (!ok) {
            throw new MediaValidationException(
                    "File signature does not match declared content type: " + contentType);
        }
    }

    private static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new MediaValidationException("File has no valid extension");
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean startsWith(byte[] data, int[] sig) {
        if (data == null || data.length < sig.length) return false;
        for (int i = 0; i < sig.length; i++) {
            if ((data[i] & 0xFF) != sig[i]) return false;
        }
        return true;
    }

    private static int offset(byte[] data, int index) {
        if (data == null || data.length < index + 4) return -1;
        return ((data[index] & 0xFF) << 24) | ((data[index + 1] & 0xFF) << 16)
                | ((data[index + 2] & 0xFF) << 8) | (data[index + 3] & 0xFF);
    }
}
