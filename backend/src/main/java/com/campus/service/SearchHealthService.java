package com.campus.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHealthService {

    private final ElasticsearchClient esClient;

    public record HealthStatus(
            boolean elasticsearchConnected,
            String clusterName,
            String clusterStatus,
            String numberOfNodes,
            LocalDateTime checkedAt
    ) {
    }

    public HealthStatus checkElasticsearchHealth() {
        boolean connected = false;
        String clusterName = "unknown";
        String clusterStatus = "unavailable";
        String numberOfNodes = "0";

        try {
            connected = esClient.ping().value();

            if (connected) {
                HealthResponse health = esClient.cluster().health();
                clusterName = health.clusterName();
                clusterStatus = health.status().jsonValue();
                numberOfNodes = String.valueOf(health.numberOfNodes());
            }
        } catch (Exception e) {
            log.error("Failed to check Elasticsearch health", e);
        }

        return new HealthStatus(connected, clusterName, clusterStatus, numberOfNodes, LocalDateTime.now());
    }
}
