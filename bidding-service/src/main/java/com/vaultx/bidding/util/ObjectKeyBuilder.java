package com.vaultx.bidding.util;

import java.util.Locale;
import java.util.UUID;

/**
 * Builds object storage keys in a non-enumerable, namespaced form:
 *   auctions/{auctionId}/{randomUuid}{extension}
 */
public final class ObjectKeyBuilder {
    private ObjectKeyBuilder() {}

    public static String build(UUID auctionId, String fileName) {
        int dot = fileName.lastIndexOf('.');
        String ext = dot < 0 ? "" : fileName.substring(dot).toLowerCase(Locale.ROOT);
        return "auctions/" + auctionId + "/" + UUID.randomUUID() + ext;
    }
}
