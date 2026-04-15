package com.campus.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.campus.common.BusinessException;
import com.campus.dto.ChangePasswordRequest;
import com.campus.dto.UpdateProfileRequest;
import com.campus.dto.UserProfile;
import com.campus.entity.User;
import com.campus.mapper.UserMapper;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public User findByPhone(String phone) {
        return userMapper.selectByPhone(phone);
    }

    public User requireByPhone(String phone) {
        User user = findByPhone(phone);
        if (user == null) {
            throw new BusinessException(404, "user not found");
        }
        return user;
    }

    public User findByUserId(Long userId) {
        return userMapper.selectById(userId);
    }

    public User requireByUserId(Long userId) {
        User user = findByUserId(userId);
        if (user == null) {
            throw new BusinessException(404, "user not found");
        }
        return user;
    }

    public User requireByIdentity(String identity) {
        if (identity == null || identity.isBlank()) {
            throw new BusinessException(401, "unauthorized");
        }
        if (identity.matches("^\\d+$")) {
            try {
                return requireByUserId(Long.parseLong(identity));
            } catch (NumberFormatException | DataAccessException ex) {
                throw new BusinessException(404, "user not found");
            }
        }
        return requireByPhone(identity);
    }

    public User findByUsername(String username) {
        return findByPhone(username);
    }

    public User requireByUsername(String username) {
        return requireByIdentity(username);
    }

    public UserProfile getProfile(String identity) {
        return toProfile(requireByIdentity(identity));
    }

    public UserProfile updateProfile(String identity, UpdateProfileRequest request) {
        User user = requireByIdentity(identity);
        user.setRealName(request.realName());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toProfile(user);
    }

    public void changePassword(String identity, ChangePasswordRequest request) {
        User user = requireByIdentity(identity);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(400, "old password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public UserProfile toProfile(User user) {
        return new UserProfile(user.getId(), user.getPhone(), user.getNickname(), user.getRole(), user.getStatus(),
                user.getVerificationStatus(), user.getRealName(), user.getStudentId());
    }
}
