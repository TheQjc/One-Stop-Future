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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.MyJobApplicationListResponse;
import com.campus.preview.ApplicationSnapshotPreviewService;
import com.campus.service.JobApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "岗位申请", description = "我的申请记录管理")
@Validated
@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    public JobApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    @Operation(summary = "获取我的申请列表")
    @GetMapping("/mine")
    public Result<MyJobApplicationListResponse> mine(Authentication authentication) {
        return Result.success(jobApplicationService.listMine(authentication.getName()));
    }

    @Operation(summary = "下载申请简历")
    @GetMapping("/{id}/resume/download")
    public ResponseEntity<InputStreamResource> downloadResume(@PathVariable Long id, Authentication authentication) {
        JobApplicationService.DownloadedApplicationResume download =
                jobApplicationService.downloadSnapshot(authentication.getName(), id);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new InputStreamResource(download.inputStream()));
    }

    @Operation(summary = "预览申请简历")
    @GetMapping("/{id}/resume/preview")
    public ResponseEntity<InputStreamResource> previewResume(@PathVariable Long id, Authentication authentication) {
        ApplicationSnapshotPreviewService.PreviewFile preview =
                jobApplicationService.previewSnapshot(authentication.getName(), id);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(preview.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(preview.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new InputStreamResource(preview.inputStream()));
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
