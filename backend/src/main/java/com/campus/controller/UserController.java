package com.campus.controller;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.ChangePasswordRequest;
import com.campus.dto.UpdateProfileRequest;
import com.campus.dto.UserProfile;
import com.campus.service.UserService;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Result<UserProfile> me(Authentication authentication) {
        return Result.success(userService.getProfile(authentication.getName()));
    }

    @PutMapping("/me")
    public Result<UserProfile> updateProfile(Authentication authentication, @RequestBody UpdateProfileRequest request) {
        return Result.success(userService.updateProfile(authentication.getName(), request));
    }

    @PutMapping("/me/password")
    public Result<Void> changePassword(Authentication authentication,
            @Validated @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
        return Result.success();
    }
}
