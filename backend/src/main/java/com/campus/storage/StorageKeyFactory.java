package com.campus.storage;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class StorageKeyFactory {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final Clock clock;

    public StorageKeyFactory() {
        this(Clock.systemDefaultZone());
    }

    StorageKeyFactory(Clock clock) {
        this.clock = clock;
    }

    public String newStorageKey(String originalFilename) {
        String extension = extractExtension(originalFilename);
        return DATE_PATH.format(LocalDate.now(clock)) + "/" + UUID.randomUUID() + extension;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(lastDot);
    }
}
