package com.campus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.resource-upload")
public class ResourceUploadProperties {

    private String chunkRoot = ".local-storage/resource-upload-sessions";
    private int defaultChunkSizeBytes = 5 * 1024 * 1024;
    private int maxChunkSizeBytes = 10 * 1024 * 1024;
}
