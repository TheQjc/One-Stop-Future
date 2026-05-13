package com.campus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "platform.integrations.elasticsearch")
public class ElasticsearchIntegrationProperties {

    private boolean enabled = true;
    private String uris = "http://114.132.220.42:9200";
    private String username = "elastic";
    private String password = "9ce68K7lOI8_7WgLdD8*";
    private String indexPrefix = "campus-platform";

    private HighlightProperties highlight = new HighlightProperties();

    @Data
    public static class HighlightProperties {
        private String preTag = "<em class=\"search-highlight\">";
        private String postTag = "</em>";
        private String fragmentSize = "150";
        private int numberOfFragments = 3;
    }
}
