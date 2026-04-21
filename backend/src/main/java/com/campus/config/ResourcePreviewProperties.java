package com.campus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.resource-preview")
public class ResourcePreviewProperties {

    private String localRoot = ".local-storage/previews";
    private Docx docx = new Docx();

    @Data
    public static class Docx {

        private String sofficeCommand = "soffice";
    }
}
