package com.campus.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnifiedSearchDocument {

    private String id;
    private String contentType;
    private Long ownerId;
    private String visibility;
    private String status;

    private String title;
    private String content;
    private String summary;
    private List<String> tags;
    private String authorName;
    private Long authorId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private String path;

    @Builder.Default
    private Map<String, Object> extra = new HashMap<>();

    public static UnifiedSearchDocument of(
            String id,
            String contentType,
            Long ownerId,
            String visibility,
            String status,
            String title,
            String content,
            String summary,
            List<String> tags,
            String authorName,
            Long authorId,
            LocalDateTime publishedAt,
            LocalDateTime createdAt,
            String path) {
        return UnifiedSearchDocument.builder()
                .id(id)
                .contentType(contentType)
                .ownerId(ownerId)
                .visibility(visibility)
                .status(status)
                .title(title)
                .content(content)
                .summary(summary)
                .tags(tags)
                .authorName(authorName)
                .authorId(authorId)
                .publishedAt(publishedAt)
                .createdAt(createdAt)
                .path(path)
                .build();
    }
}
