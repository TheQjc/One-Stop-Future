package com.campus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.resource-storage")
public class ResourceStorageProperties {

    private String type = "local";
    private String localRoot = ".local-storage/resources";
}
