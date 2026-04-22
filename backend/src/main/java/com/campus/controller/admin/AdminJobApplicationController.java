package com.campus.controller.admin;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.AdminJobApplicationListResponse;
import com.campus.preview.ApplicationSnapshotPreviewService;
import com.campus.service.AdminJobApplicationService;
import com.campus.service.AdminJobApplicationService.DownloadedApplicationResume;

@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/applications")
public class AdminJobApplicationController {

    private final AdminJobApplicationService adminJobApplicationService;

    public AdminJobApplicationController(AdminJobApplicationService adminJobApplicationService) {
        this.adminJobApplicationService = adminJobApplicationService;
    }

    @GetMapping
    public Result<AdminJobApplicationListResponse> list() {
        return Result.success(adminJobApplicationService.listApplications());
    }

    @GetMapping("/{id}/resume/download")
    public ResponseEntity<InputStreamResource> downloadResume(@PathVariable Long id) {
        DownloadedApplicationResume download = adminJobApplicationService.downloadResumeSnapshot(id);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new InputStreamResource(download.inputStream()));
    }

    @GetMapping("/{id}/resume/preview")
    public ResponseEntity<InputStreamResource> previewResume(@PathVariable Long id) {
        ApplicationSnapshotPreviewService.PreviewFile preview =
                adminJobApplicationService.previewResumeSnapshot(id);
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
