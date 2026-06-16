package com.campus.web;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.campus.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lightweight in-memory rate limiter for auth endpoints.
 * Does not introduce external dependencies.
 */
@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    private static final int SMS_PER_PHONE_MAX = 1;
    private static final int SMS_PER_PHONE_WINDOW_SECONDS = 60;
    private static final int LOGIN_PER_IP_MAX = 10;
    private static final int LOGIN_PER_IP_WINDOW_SECONDS = 60;

    private final Map<String, WindowCounter> smsCounters = new ConcurrentHashMap<>();
    private final Map<String, WindowCounter> loginCounters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimiterInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method) && path.equals("/api/auth/codes/send")) {
            String phone = null;
            if (request instanceof CachedBodyHttpServletRequest cachedRequest) {
                byte[] body = cachedRequest.getCachedBody();
                if (body != null && body.length > 0) {
                    JsonNode jsonNode = objectMapper.readTree(body);
                    if (jsonNode.has("phone")) {
                        phone = jsonNode.get("phone").asText();
                    }
                }
            } else {
                phone = request.getParameter("phone");
            }
            
            if (phone != null && !phone.isBlank()) {
                checkRate(smsCounters, "sms:" + phone, SMS_PER_PHONE_MAX, SMS_PER_PHONE_WINDOW_SECONDS,
                        "verification code sent too frequently, please wait");
            }
        }

        if ("POST".equalsIgnoreCase(method) && path.equals("/api/auth/login")) {
            String ip = getClientIp(request);
            checkRate(loginCounters, "login:" + ip, LOGIN_PER_IP_MAX, LOGIN_PER_IP_WINDOW_SECONDS,
                    "too many login attempts, please wait");
        }

        return true;
    }

    private void checkRate(Map<String, WindowCounter> counters, String key, int max, int windowSeconds,
            String message) {
        Instant now = Instant.now();
        WindowCounter counter = counters.compute(key, (k, v) -> {
            if (v == null || v.windowStart().plusSeconds(windowSeconds).isBefore(now)) {
                return new WindowCounter(now, 1);
            }
            return new WindowCounter(v.windowStart(), v.count() + 1);
        });

        if (counter.count() > max) {
            throw new BusinessException(429, message);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private record WindowCounter(Instant windowStart, int count) {
    }
}
