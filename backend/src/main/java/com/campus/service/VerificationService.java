package com.campus.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campus.common.BusinessException;
import com.campus.common.UserRole;
import com.campus.common.VerificationStatus;
import com.campus.dto.UserProfile;
import com.campus.dto.VerificationApplyRequest;
import com.campus.entity.User;
import com.campus.entity.VerificationApplication;
import com.campus.mapper.UserMapper;
import com.campus.mapper.VerificationApplicationMapper;

@Service
public class VerificationService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final VerificationApplicationMapper verificationApplicationMapper;

    public VerificationService(UserService userService, UserMapper userMapper,
            VerificationApplicationMapper verificationApplicationMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.verificationApplicationMapper = verificationApplicationMapper;
    }

    @Transactional
    public UserProfile apply(String identity, VerificationApplyRequest request) {
        User user = userService.requireByIdentity(identity);
        if (!UserRole.USER.name().equals(user.getRole())) {
            throw new BusinessException(403, "only normal users can apply for verification");
        }
        if (VerificationStatus.VERIFIED.name().equals(user.getVerificationStatus())) {
            throw new BusinessException(400, "user is already verified");
        }
        if (verificationApplicationMapper.selectPendingByUserId(user.getId()) != null
                || VerificationStatus.PENDING.name().equals(user.getVerificationStatus())) {
            throw new BusinessException(400, "pending verification application already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        VerificationApplication application = new VerificationApplication();
        application.setUserId(user.getId());
        application.setRealName(request.realName().trim());
        application.setStudentId(request.studentId().trim());
        application.setStatus(VerificationStatus.PENDING.name());
        application.setCreatedAt(now);
        application.setUpdatedAt(now);
        verificationApplicationMapper.insert(application);

        user.setRealName(request.realName().trim());
        user.setVerificationStatus(VerificationStatus.PENDING.name());
        user.setUpdatedAt(now);
        userMapper.updateById(user);

        return userService.toProfile(user);
    }
}
