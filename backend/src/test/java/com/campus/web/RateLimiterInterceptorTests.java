package com.campus.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.campus.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;

class RateLimiterInterceptorTests {

    @Test
    void loginRateLimitCannotBeBypassedBySpoofingForwardedHeaders() throws Exception {
        RateLimiterInterceptor interceptor = new RateLimiterInterceptor(new ObjectMapper());

        for (int i = 0; i < 10; i++) {
            interceptor.preHandle(loginRequest("203.0.113.8", "198.51.100." + i), new MockHttpServletResponse(), null);
        }

        assertThatThrownBy(() ->
                interceptor.preHandle(loginRequest("203.0.113.8", "198.51.100.99"), new MockHttpServletResponse(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("too many login attempts, please wait");
    }

    private MockHttpServletRequest loginRequest(String remoteAddress, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(remoteAddress);
        request.addHeader("X-Forwarded-For", forwardedFor);
        request.addHeader("X-Real-IP", forwardedFor);
        return request;
    }
}
