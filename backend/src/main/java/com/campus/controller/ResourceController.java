package com.campus.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.campus.common.Result;
import com.campus.dto.MyResourceListResponse;
import com.campus.dto.ResourceDetailResponse;
import com.campus.dto.ResourceListResponse;
import com.campus.service.ResourceService;
import com.campus.service.ResourceService.DownloadedResource;

@Validated
@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public Result<ResourceListResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            Authentication authentication) {
        return Result.success(resourceService.listResources(keyword, category, identityOf(authentication)));
    }

    @GetMapping("/mine")
    public Result<MyResourceListResponse> mine(Authentication authentication) {
        return Result.success(resourceService.listMyResources(authentication.getName()));
    }

    @GetMapping("/{id}")
    public Result<ResourceDetailResponse> detail(@PathVariable Long id, Authentication authentication) {
        return Result.success(resourceService.getResourceDetail(id, identityOf(authentication)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id, Authentication authentication) {
        DownloadedResource download = resourceService.downloadResource(authentication.getName(), id);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new InputStreamResource(download.inputStream()));
    }

    @PostMapping
    public Result<ResourceDetailResponse> upload(
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam String summary,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return Result.success(resourceService.uploadResource(authentication.getName(), title, category, summary,
                description, file));
    }

    @PostMapping("/{id}/favorite")
    public Result<ResourceDetailResponse> favorite(@PathVariable Long id, Authentication authentication) {
        return Result.success(resourceService.favoriteResource(authentication.getName(), id));
    }

    @DeleteMapping("/{id}/favorite")
    public Result<ResourceDetailResponse> unfavorite(@PathVariable Long id, Authentication authentication) {
        return Result.success(resourceService.unfavoriteResource(authentication.getName(), id));
    }

    private String identityOf(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return authentication.getName();
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
