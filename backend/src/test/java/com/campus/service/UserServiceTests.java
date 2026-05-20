package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.campus.dto.UpdateProfileRequest;
import com.campus.dto.UserProfile;
import com.campus.entity.User;
import com.campus.mapper.UserMapper;
import com.campus.config.SearchSyncProperties;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserMapper userMapper;

    @Mock
    private SearchIndexSyncService searchIndexSyncService;

    @Test
    void updateProfileRefreshesSearchDocumentsForNicknameChanges() throws Exception {
        User user = user(2L, "13800000001", "NormalUser");
        when(userMapper.selectById(2L)).thenReturn(user);

        UserService userService = new UserService(userMapper);
        ReflectionTestUtils.setField(userService, "searchIndexSyncService", searchIndexSyncService);

        UserProfile profile = userService.updateProfile("2",
                new UpdateProfileRequest("FutureRunner", "Student Updated"));

        assertThat(profile.nickname()).isEqualTo("FutureRunner");
        assertThat(profile.realName()).isEqualTo("Student Updated");
        verify(searchIndexSyncService).refreshUserDocuments(2L);
    }

    @Test
    void updateProfileStillSucceedsWhenSearchDocumentRefreshFails() throws Exception {
        User user = user(2L, "13800000001", "NormalUser");
        when(userMapper.selectById(2L)).thenReturn(user);
        when(searchIndexSyncService.refreshUserDocuments(2L)).thenThrow(new IOException("boom"));

        UserService userService = new UserService(userMapper);
        ReflectionTestUtils.setField(userService, "searchIndexSyncService", searchIndexSyncService);

        UserProfile profile = userService.updateProfile("2",
                new UpdateProfileRequest("FutureRunner", "Student Updated"));

        assertThat(profile.nickname()).isEqualTo("FutureRunner");
        assertThat(profile.realName()).isEqualTo("Student Updated");
        verify(searchIndexSyncService).refreshUserDocuments(2L);
    }

    @Test
    void updateProfileSkipsSearchDocumentRefreshWhenSearchSyncIsDisabled() throws Exception {
        User user = user(2L, "13800000001", "NormalUser");
        when(userMapper.selectById(2L)).thenReturn(user);

        SearchSyncProperties searchSyncProperties = new SearchSyncProperties();
        searchSyncProperties.setEnabled(false);

        UserService userService = new UserService(userMapper);
        ReflectionTestUtils.setField(userService, "searchIndexSyncService", searchIndexSyncService);
        ReflectionTestUtils.setField(userService, "searchSyncProperties", searchSyncProperties);

        UserProfile profile = userService.updateProfile("2",
                new UpdateProfileRequest("FutureRunner", "Student Updated"));

        assertThat(profile.nickname()).isEqualTo("FutureRunner");
        assertThat(profile.realName()).isEqualTo("Student Updated");
        verify(searchIndexSyncService, org.mockito.Mockito.never()).refreshUserDocuments(2L);
    }

    private User user(Long id, String phone, String nickname) {
        User user = new User();
        user.setId(id);
        user.setPhone(phone);
        user.setNickname(nickname);
        user.setStatus("ACTIVE");
        return user;
    }
}
