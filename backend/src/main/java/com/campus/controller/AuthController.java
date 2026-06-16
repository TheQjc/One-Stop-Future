package com.campus.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.AuthResponse;
import com.campus.dto.LoginRequest;
import com.campus.dto.RegisterRequest;
import com.campus.dto.SendCodeRequest;
import com.campus.dto.SendCodeResponse;
import com.campus.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "认证", description = "手机验证码登录/注册/登出")
@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "发送验证码", description = "向指定手机号发送登录或注册用途的短信验证码")
    @ApiResponse(responseCode = "200", description = "验证码已发送")
    @PostMapping("/codes/send")
    public Result<SendCodeResponse> sendCode(@Validated @RequestBody SendCodeRequest request) {
        return Result.success(authService.sendCode(request));
    }

    @Operation(summary = "用户注册", description = "使用手机号、验证码和昵称注册新账户")
    @ApiResponse(responseCode = "200", description = "注册成功，返回 JWT Token")
    @PostMapping("/register")
    public Result<AuthResponse> register(@Validated @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "用户登录", description = "使用手机号和验证码登录，返回 JWT Token")
    @ApiResponse(responseCode = "200", description = "登录成功")
    @PostMapping("/login")
    public Result<AuthResponse> login(@Validated @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "用户登出", description = "退出当前登录状态")
    @ApiResponse(responseCode = "200", description = "登出成功")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
