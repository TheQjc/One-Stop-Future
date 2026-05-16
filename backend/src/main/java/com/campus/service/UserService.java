package com.campus.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.campus.common.BusinessException;
import com.campus.common.UserStatus;
import com.campus.dto.UpdateProfileRequest;
import com.campus.dto.UserProfile;
import com.campus.entity.User;
import com.campus.mapper.UserMapper;

@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
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
        User user;
        if (identity.matches("^\\d+$")) {
            try {
                user = requireByUserId(Long.parseLong(identity));
                ensureAccountIsActive(user);
                return user;
            } catch (NumberFormatException | DataAccessException ex) {
                throw new BusinessException(404, "user not found");
            }
        }
        user = requireByPhone(identity);
        ensureAccountIsActive(user);
        return user;
    }

    public User findByUsername(String username) {
        return findByPhone(username);
    }

    public User findByIdentity(String identity) {
        if (identity == null || identity.isBlank() || "anonymousUser".equals(identity)) {
            return null;
        }
        try {
            return requireByIdentity(identity);
        } catch (BusinessException e) {
            return null;
        }
    }

    public User requireByUsername(String username) {
        return requireByIdentity(username);
    }

    public UserProfile getProfile(String identity) {
        return toProfile(requireByIdentity(identity));
    }

    public UserProfile updateProfile(String identity, UpdateProfileRequest request) {
        if (request == null) {
            throw new BusinessException(400, "invalid request");
        }
        User user = requireByIdentity(identity);
        if (request.nickname() != null && !request.nickname().isBlank()) {
            user.setNickname(request.nickname().trim());
        }
        if (request.realName() != null && !request.realName().isBlank()) {
            user.setRealName(request.realName().trim());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toProfile(user);
    }

    public UserProfile toProfile(User user) {
        return new UserProfile(user.getId(), user.getPhone(), user.getNickname(), user.getRole(), user.getStatus(),
                user.getVerificationStatus(), user.getRealName(), user.getStudentId());
    }

    private void ensureAccountIsActive(User user) {
        if (UserStatus.BANNED.name().equals(user.getStatus())) {
            throw new BusinessException(403, "account is banned");
        }
    }
}
