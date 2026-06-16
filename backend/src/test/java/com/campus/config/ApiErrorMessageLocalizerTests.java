package com.campus.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiErrorMessageLocalizerTests {

    @Test
    void localizesKnownBusinessMessages() {
        assertThat(ApiErrorMessageLocalizer.localize("account is banned")).isEqualTo("账号已被封禁");
        assertThat(ApiErrorMessageLocalizer.localize("job not found")).isEqualTo("岗位不存在");
    }

    @Test
    void hidesUnknownEnglishMessagesBehindChineseFallback() {
        assertThat(ApiErrorMessageLocalizer.localize("some upstream failure"))
                .isEqualTo("请求失败，请稍后重试");
    }

    @Test
    void keepsAlreadyLocalizedMessages() {
        assertThat(ApiErrorMessageLocalizer.localize("账号已被封禁")).isEqualTo("账号已被封禁");
    }
}
