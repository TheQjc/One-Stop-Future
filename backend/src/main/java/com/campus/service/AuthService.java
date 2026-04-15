package com.campus.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.common.JwtUtil;
import com.campus.common.UserStatus;
import com.campus.common.VerificationStatus;
import com.campus.common.UserRole;
import com.campus.dto.AuthResponse;
import com.campus.dto.LoginRequest;
import com.campus.dto.RegisterRequest;
import com.campus.dto.SendCodeRequest;
import com.campus.dto.SendCodeResponse;
import com.campus.entity.User;
import com.campus.entity.VerificationCode;
import com.campus.mapper.UserMapper;
import com.campus.mapper.VerificationCodeMapper;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long CODE_EXPIRE_SECONDS = 300L;
    private static final String PURPOSE_REGISTER = "REGISTER";
    private static final String PURPOSE_LOGIN = "LOGIN";

    private final UserMapper userMapper;
    private final VerificationCodeMapper verificationCodeMapper;
    private final JwtUtil jwtUtil;
    private final JdbcTemplate jdbcTemplate;
    private final boolean mockSmsMode;

    public AuthService(UserMapper userMapper, VerificationCodeMapper verificationCodeMapper, JwtUtil jwtUtil,
            JdbcTemplate jdbcTemplate, @Value("${platform.sms.mock-mode:true}") boolean mockSmsMode) {
        this.userMapper = userMapper;
        this.verificationCodeMapper = verificationCodeMapper;
        this.jwtUtil = jwtUtil;
        this.jdbcTemplate = jdbcTemplate;
        this.mockSmsMode = mockSmsMode;
    }

    public SendCodeResponse sendCode(SendCodeRequest request) {
        String purpose = normalizePurpose(request.purpose());
        String code = generateVerificationCode();
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setPhone(request.phone());
        verificationCode.setPurpose(purpose);
        verificationCode.setCode(code);
        verificationCode.setExpiresAt(LocalDateTime.now().plusSeconds(CODE_EXPIRE_SECONDS));
        verificationCode.setCreatedAt(LocalDateTime.now());
        verificationCodeMapper.insert(verificationCode);
        if (mockSmsMode) {
            log.info("Mock SMS code sent: phone={}, purpose={}, code={}", request.phone(), purpose, code);
            return new SendCodeResponse(purpose, code, CODE_EXPIRE_SECONDS);
        }
        return new SendCodeResponse(purpose, null, CODE_EXPIRE_SECONDS);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        consumeValidCode(request.phone(), PURPOSE_REGISTER, request.verificationCode());
        if (userMapper.selectByPhone(request.phone()) != null) {
            throw new BusinessException(400, "phone already registered");
        }
        User user = new User();
        user.setPhone(request.phone());
        user.setNickname(request.nickname());
        user.setRole(UserRole.USER.name());
        user.setStatus(UserStatus.ACTIVE.name());
        user.setVerificationStatus(VerificationStatus.UNVERIFIED.name());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        insertWelcomeNotification(user.getId());
        return toAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        consumeValidCode(request.phone(), PURPOSE_LOGIN, request.verificationCode());
        User user = userMapper.selectByPhone(request.phone());
        if (user == null) {
            throw new BusinessException(400, "phone or verification code is incorrect");
        }
        if (UserStatus.BANNED.name().equals(user.getStatus())) {
            throw new BusinessException(403, "account is banned");
        }
        return toAuthResponse(user);
    }

    private String normalizePurpose(String purpose) {
        try {
            String normalized = purpose.toUpperCase(Locale.ROOT);
            if (!PURPOSE_REGISTER.equals(normalized) && !PURPOSE_LOGIN.equals(normalized)) {
                throw new IllegalArgumentException("unsupported purpose");
            }
            return normalized;
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessException(400, "invalid purpose");
        }
    }

    private String generateVerificationCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
    }

    private void consumeValidCode(String phone, String purpose, String code) {
        VerificationCode verificationCode = verificationCodeMapper.selectOne(new LambdaQueryWrapper<VerificationCode>()
                .eq(VerificationCode::getPhone, phone)
                .eq(VerificationCode::getPurpose, purpose)
                .eq(VerificationCode::getCode, code)
                .isNull(VerificationCode::getConsumedAt)
                .orderByDesc(VerificationCode::getCreatedAt)
                .last("limit 1"));
        if (verificationCode == null) {
            throw new BusinessException(400, "phone or verification code is incorrect");
        }
        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "verification code has expired");
        }
        verificationCode.setConsumedAt(LocalDateTime.now());
        verificationCodeMapper.updateById(verificationCode);
    }

    private void insertWelcomeNotification(Long userId) {
        jdbcTemplate.update(
                "INSERT INTO t_notification (user_id, type, title, content, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                userId, "WELCOME", "Welcome", "Welcome to One-Stop Future.", 0, LocalDateTime.now());
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtUtil.generateToken(String.valueOf(user.getId()), user.getRole());
        return new AuthResponse(token, user.getId(), user.getPhone(), user.getNickname(), user.getRole(),
                user.getStatus(), user.getVerificationStatus());
    }
}
