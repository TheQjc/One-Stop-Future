package com.campus.config;

import com.campus.service.SearchIndexSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.search-sync.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class SearchIndexInitializer implements ApplicationRunner {

    private final SearchIndexSyncService syncService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("Initializing search index on application startup...");
            syncService.fullReindex();
            log.info("Search index initialization completed successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize search index on startup. "
                    + "Search may return no results until next sync.", e);
        }
    }
}
