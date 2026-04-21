package com.campus.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import com.campus.common.BusinessException;
import com.campus.config.JobSyncProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ThirdPartyJobFeedClient {

    private static final String MESSAGE_JOB_SYNC_UNAVAILABLE = "job sync unavailable";
    private static final String MESSAGE_JOB_SYNC_REQUEST_FAILED = "job sync request failed";
    private static final String MESSAGE_INVALID_JOB_SYNC_FEED = "invalid job sync feed";

    private final JobSyncProperties jobSyncProperties;
    private final ObjectMapper objectMapper;

    public ThirdPartyJobFeedClient(JobSyncProperties jobSyncProperties, ObjectMapper objectMapper) {
        this.jobSyncProperties = jobSyncProperties;
        this.objectMapper = objectMapper;
    }

    public List<ThirdPartyJobFeedItem> fetchJobs() {
        if (!jobSyncProperties.isEnabled() || isBlank(jobSyncProperties.getFeedUrl())
                || isBlank(jobSyncProperties.getSourceName())
                || jobSyncProperties.getConnectTimeoutMs() <= 0
                || jobSyncProperties.getReadTimeoutMs() <= 0) {
            throw new BusinessException(500, MESSAGE_JOB_SYNC_UNAVAILABLE);
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(toUri(jobSyncProperties.getFeedUrl()))
                .timeout(Duration.ofMillis(jobSyncProperties.getReadTimeoutMs()))
                .header("Accept", "application/json")
                .GET();

        String bearerToken = normalizeToken(jobSyncProperties.getBearerToken());
        if (bearerToken != null) {
            requestBuilder.header("Authorization", "Bearer " + bearerToken);
        }

        HttpResponse<String> response;
        try {
            response = httpClient().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, MESSAGE_JOB_SYNC_REQUEST_FAILED);
        } catch (IOException exception) {
            throw new BusinessException(500, MESSAGE_JOB_SYNC_REQUEST_FAILED);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException(500, MESSAGE_JOB_SYNC_REQUEST_FAILED);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BusinessException(500, MESSAGE_INVALID_JOB_SYNC_FEED);
        }

        JsonNode jobsNode = root == null ? null : root.get("jobs");
        if (jobsNode == null || !jobsNode.isArray()) {
            throw new BusinessException(500, MESSAGE_INVALID_JOB_SYNC_FEED);
        }

        return StreamSupport.stream(jobsNode.spliterator(), false)
                .map(this::toItem)
                .toList();
    }

    private HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(jobSyncProperties.getConnectTimeoutMs()))
                .build();
    }

    private URI toUri(String feedUrl) {
        try {
            return URI.create(feedUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(500, MESSAGE_JOB_SYNC_UNAVAILABLE);
        }
    }

    private String normalizeToken(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            return null;
        }
        return bearerToken.trim();
    }

    private ThirdPartyJobFeedItem toItem(JsonNode node) {
        return new ThirdPartyJobFeedItem(
                text(node, "title"),
                text(node, "companyName"),
                text(node, "city"),
                text(node, "jobType"),
                text(node, "educationRequirement"),
                text(node, "sourceUrl"),
                text(node, "summary"),
                text(node, "content"),
                text(node, "deadlineAt"));
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
