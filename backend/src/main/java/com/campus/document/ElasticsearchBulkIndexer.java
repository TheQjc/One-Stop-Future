package com.campus.document;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class ElasticsearchBulkIndexer {

    private final ElasticsearchClient esClient;

    public ElasticsearchBulkIndexer(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public record BulkIndexResult(int success, int failed, List<String> errors) {}

    public BulkIndexResult bulkIndex(String indexName, List<UnifiedSearchDocument> documents) throws IOException {
        if (documents == null || documents.isEmpty()) {
            return new BulkIndexResult(0, 0, List.of());
        }

        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

        for (UnifiedSearchDocument doc : documents) {
            bulkBuilder.operations(op -> op
                    .index(idx -> idx
                            .index(indexName)
                            .id(doc.getId())
                            .document(doc)
                    )
            );
        }

        BulkResponse response = esClient.bulk(bulkBuilder.build());

        int success = 0;
        int failed = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (BulkResponseItem item : response.items()) {
            if (item.error() == null) {
                success++;
            } else {
                failed++;
                errors.add("[" + item.id() + "] " + item.error().reason());
                log.warn("Failed to index document {}: {}", item.id(), item.error().reason());
            }
        }

        return new BulkIndexResult(success, failed, errors);
    }
}
