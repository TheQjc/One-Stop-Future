package com.campus.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.common.UserRole;
import com.campus.common.UserStatus;
import com.campus.common.VerificationStatus;
import com.campus.dto.AdminUserListResponse;
import com.campus.entity.User;
import com.campus.mapper.UserMapper;

@Service
public class AdminUserService {

    private static final int ADMIN_LIST_LIMIT = 100;

    private final UserMapper userMapper;
    private final UserService userService;

    public AdminUserService(UserMapper userMapper, UserService userService) {
        this.userMapper = userMapper;
        this.userService = userService;
    }

    public AdminUserListResponse listUsers(String identity) {
        userService.requireByIdentity(identity);
        List<AdminUserListResponse.UserItem> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .orderByDesc(User::getCreatedAt)
                        .orderByDesc(User::getId)
                        .last("LIMIT " + ADMIN_LIST_LIMIT))
                .stream()
                .map(this::toUserItem)
                .toList();
        return new AdminUserListResponse(
                userMapper.selectCount(new LambdaQueryWrapper<>()).intValue(),
                userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, UserStatus.ACTIVE.name())).intValue(),
                userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, UserStatus.BANNED.name())).intValue(),
                userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getVerificationStatus, VerificationStatus.VERIFIED.name())).intValue(),
                users);
    }

    @Transactional
    public AdminUserListResponse.UserItem banUser(String identity, Long userId) {
        userService.requireByIdentity(identity);
        User target = requireManageableUser(userId);
        if (UserStatus.BANNED.name().equals(target.getStatus())) {
            return toUserItem(target);
        }
        target.setStatus(UserStatus.BANNED.name());
        target.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(target);
        return toUserItem(target);
    }

    @Transactional
    public AdminUserListResponse.UserItem unbanUser(String identity, Long userId) {
        userService.requireByIdentity(identity);
        User target = requireManageableUser(userId);
        if (UserStatus.ACTIVE.name().equals(target.getStatus())) {
            return toUserItem(target);
        }
        target.setStatus(UserStatus.ACTIVE.name());
        target.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(target);
        return toUserItem(target);
    }

    private User requireManageableUser(Long userId) {
        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new BusinessException(404, "user not found");
        }
        if (UserRole.ADMIN.name().equals(target.getRole())) {
            throw new BusinessException(400, "admin account status cannot be changed");
        }
        return target;
    }

    private AdminUserListResponse.UserItem toUserItem(User user) {
        return new AdminUserListResponse.UserItem(
                user.getId(),
                user.getPhone(),
                user.getNickname(),
                user.getRealName(),
                user.getRole(),
                user.getStatus(),
                user.getVerificationStatus(),
                user.getStudentId(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
