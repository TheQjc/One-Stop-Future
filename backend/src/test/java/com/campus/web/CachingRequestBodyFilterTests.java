package com.campus.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

class CachingRequestBodyFilterTests {

    private final CachingRequestBodyFilter filter = new CachingRequestBodyFilter();

    @Test
    void wrapsOnlySmsCodeJsonRequests() throws Exception {
        CapturingFilterChain smsChain = new CapturingFilterChain();
        MockHttpServletRequest smsRequest = jsonPost("/api/auth/codes/send");

        filter.doFilter(smsRequest, new MockHttpServletResponse(), smsChain);

        assertThat(smsChain.request).isInstanceOf(CachedBodyHttpServletRequest.class);

        CapturingFilterChain loginChain = new CapturingFilterChain();
        MockHttpServletRequest loginRequest = jsonPost("/api/auth/login");

        filter.doFilter(loginRequest, new MockHttpServletResponse(), loginChain);

        assertThat(loginChain.request).isSameAs(loginRequest);
    }

    private MockHttpServletRequest jsonPost(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("{\"phone\":\"13800000000\"}".getBytes());
        return request;
    }

    private static class CapturingFilterChain implements FilterChain {
        private ServletRequest request;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            this.request = request;
        }
    }
}
