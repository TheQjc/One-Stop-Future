package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JobPostingFieldNormalizerTests {

    @Autowired
    private JobPostingFieldNormalizer normalizer;

    @Test
    void normalizesEnumsAndDeadlineUsingSharedRules() {
        assertThat(normalizer.normalizeJobType(" full_time ")).isEqualTo("FULL_TIME");
        assertThat(normalizer.normalizeEducationRequirement(" master ")).isEqualTo("MASTER");
        assertThat(normalizer.parseDeadline("2026-06-20 18:00:00"))
                .isEqualTo(LocalDateTime.of(2026, 6, 20, 18, 0));
    }

    @Test
    void rejectsUnsupportedSourceUrlSchemes() {
        assertThatThrownBy(() -> normalizer.normalizeSourceUrl("ftp://partner.example/jobs/1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid source url");
    }

    @Test
    void trimsOptionalTextAndReturnsNullForBlankValues() {
        assertThat(normalizer.optionalText("  summary  ", 300, "summary")).isEqualTo("summary");
        assertThat(normalizer.optionalText("   ", 300, "summary")).isNull();
    }
}
