package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.campus.dto.HomeSummaryResponse;
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
}
