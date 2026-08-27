package com.vaultx.bidding.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
@Getter
@Setter
public class StorageProperties {
    private String endpoint = "http://localhost:4566";
    private String presignExternalEndpoint = "http://localhost:4566";
    private String publicBaseUrl = "http://localhost:4566/vaultx-media";
    private String region = "us-east-1";
    private String bucket = "vaultx-media";
    private String accessKey = "test";
    private String secretKey = "test";
    private boolean pathStyleAccess = true;
    private long maxImageBytes = 10L * 1024 * 1024;
    private long maxVideoBytes = 100L * 1024 * 1024;
    private int maxImagesPerAuction = 10;
    private int maxVideosPerAuction = 3;
    private long presignTtlSeconds = 900;
}
