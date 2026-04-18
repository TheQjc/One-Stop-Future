package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.campus.dto.HomeSummaryResponse;
import com.campus.entity.User;
import com.campus.mapper.VerificationApplicationMapper;

@ExtendWith(MockitoExtension.class)
class HomeServiceTests {

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private VerificationApplicationMapper verificationApplicationMapper;

    @Mock
    private DiscoverService discoverService;

    @InjectMocks
    private HomeService homeService;

    @Test
    void discoverPreviewFallsBackToEmptyListWhenDiscoverServiceFails() {
        when(discoverService.previewForHome(4)).thenThrow(new RuntimeException("boom"));

        HomeSummaryResponse response = homeService.getSummary(null);

        assertThat(response.viewerType()).isEqualTo("GUEST");
        assertThat(response.discoverPreview().period()).isEqualTo("WEEK");
        assertThat(response.discoverPreview().items()).isEqualTo(List.of());
    }

    @Test
    void guestSummaryKeepsAssessmentLockedWithLoginRequired() {
        when(discoverService.previewForHome(4)).thenReturn(List.of());

        HomeSummaryResponse response = homeService.getSummary(null);

        HomeSummaryResponse.HomeEntryCard assessment = response.entries().stream()
                .filter(entry -> "assessment".equals(entry.code()))
                .findFirst()
                .orElseThrow();

        assertThat(assessment.enabled()).isFalse();
        assertThat(assessment.badge()).isEqualTo("LOGIN_REQUIRED");

        HomeSummaryResponse.HomeEntryCard analytics = response.entries().stream()
                .filter(entry -> "analytics".equals(entry.code()))
                .findFirst()
                .orElseThrow();
        assertThat(analytics.enabled()).isTrue();
        assertThat(analytics.path()).isEqualTo("/analytics");
        assertThat(analytics.badge()).isNull();
    }

    @Test
    void authenticatedSummaryActivatesAssessmentAndAnalytics() {
        when(discoverService.previewForHome(4)).thenReturn(List.of());
        User user = new User();
        user.setId(2L);
        user.setPhone("13800000001");
        user.setNickname("NormalUser");
        user.setRole("USER");
        user.setVerificationStatus("UNVERIFIED");

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("2");
        when(userService.requireByIdentity("2")).thenReturn(user);
        when(notificationService.countUnreadByUserId(2L)).thenReturn(0);
        when(notificationService.listLatestSnippets(2L, 5)).thenReturn(List.of());

        HomeSummaryResponse response = homeService.getSummary(authentication);

        HomeSummaryResponse.HomeEntryCard assessment = response.entries().stream()
                .filter(entry -> "assessment".equals(entry.code()))
                .findFirst()
                .orElseThrow();
        assertThat(assessment.enabled()).isTrue();
        assertThat(assessment.badge()).isNull();

        HomeSummaryResponse.HomeEntryCard analytics = response.entries().stream()
                .filter(entry -> "analytics".equals(entry.code()))
                .findFirst()
                .orElseThrow();
        assertThat(analytics.enabled()).isTrue();
        assertThat(analytics.badge()).isNull();
    }
}
