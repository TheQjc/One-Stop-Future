package com.campus.controller;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.UserProfile;
import com.campus.dto.VerificationApplyRequest;
import com.campus.service.VerificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "认证", description = "学生身份认证申请")
@Validated
@RestController
@RequestMapping("/api/verifications")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @Operation(summary = "提交认证申请")
    @ApiResponse(responseCode = "200", description = "提交成功")
    @PostMapping
    public Result<UserProfile> apply(Authentication authentication,
            @Validated @RequestBody VerificationApplyRequest request) {
        return Result.success(verificationService.apply(authentication.getName(), request));
    }
}
