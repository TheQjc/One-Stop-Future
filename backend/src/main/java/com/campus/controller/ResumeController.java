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
import com.campus.dto.ResumeListResponse;
import com.campus.dto.ResumeRecordResponse;
import com.campus.service.ResumeService;
import com.campus.service.ResumeService.DownloadedResume;

@Validated
@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping
    public Result<ResumeRecordResponse> upload(
            @RequestParam String title,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return Result.success(resumeService.upload(authentication.getName(), title, file));
    }

    @GetMapping("/mine")
    public Result<ResumeListResponse> mine(Authentication authentication) {
        return Result.success(resumeService.listMine(authentication.getName()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id, Authentication authentication) {
        DownloadedResume download = resumeService.download(authentication.getName(), id);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new InputStreamResource(download.inputStream()));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<InputStreamResource> preview(@PathVariable Long id, Authentication authentication) {
        ResumeService.ResumeFileStream preview = resumeService.preview(authentication.getName(), id);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(preview.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(preview.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new InputStreamResource(preview.inputStream()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        resumeService.delete(authentication.getName(), id);
        return Result.success();
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
