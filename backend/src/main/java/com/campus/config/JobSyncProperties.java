package com.campus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "platform.integrations.job-sync")
public class JobSyncProperties {

    private boolean enabled = false;
    private String feedUrl = "";
    private String sourceName = "Partner Feed";
    private String bearerToken = "";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;
}
