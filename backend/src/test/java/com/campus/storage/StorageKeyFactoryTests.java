package com.campus.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class StorageKeyFactoryTests {

    @Test
    void generatedKeyKeepsDatePrefixAndOriginalExtension() {
        StorageKeyFactory factory = new StorageKeyFactory(
                Clock.fixed(Instant.parse("2026-04-17T08:00:00Z"), ZoneId.of("Asia/Shanghai")));

        String key = factory.newStorageKey("resume-template-pack.pdf");

        assertThat(key).matches("2026/04/17/[0-9a-f\\-]{36}\\.pdf");
    }
}
