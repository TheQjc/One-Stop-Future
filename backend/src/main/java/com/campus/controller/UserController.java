package com.campus.controller;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.BusinessException;
import com.campus.common.FavoriteTargetType;
import com.campus.common.Result;
import com.campus.dto.UpdateProfileRequest;
import com.campus.dto.UserProfile;
import com.campus.service.CommunityService;
import com.campus.service.JobService;
import com.campus.service.UserService;

import java.util.Locale;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CommunityService communityService;
    private final JobService jobService;

    public UserController(UserService userService, CommunityService communityService, JobService jobService) {
        this.userService = userService;
        this.communityService = communityService;
        this.jobService = jobService;
    }

    @GetMapping("/me")
    public Result<UserProfile> me(Authentication authentication) {
        return Result.success(userService.getProfile(authentication.getName()));
    }

    @GetMapping("/me/favorites")
    public Result<Object> favorites(Authentication authentication,
            @RequestParam(defaultValue = "POST") String type) {
        FavoriteTargetType targetType = normalizeFavoriteType(type);
        return switch (targetType) {
            case POST -> Result.success(communityService.listMyPostFavorites(authentication.getName(), targetType.name()));
            case JOB -> Result.success(jobService.listMyJobFavorites(authentication.getName()));
            default -> throw new BusinessException(400, "unsupported favorite type");
        };
    }

    @PutMapping("/me")
    public Result<UserProfile> updateProfile(Authentication authentication,
            @Validated @RequestBody UpdateProfileRequest request) {
        return Result.success(userService.updateProfile(authentication.getName(), request));
    }

    private FavoriteTargetType normalizeFavoriteType(String type) {
        if (type == null || type.isBlank()) {
            return FavoriteTargetType.POST;
        }
        try {
            return FavoriteTargetType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid favorite type");
        }
    }
}
