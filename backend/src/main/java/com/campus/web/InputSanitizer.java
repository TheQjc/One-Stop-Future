package com.campus.web;

import java.util.regex.Pattern;

public final class InputSanitizer {

    private static final Pattern SCRIPT_BLOCK = Pattern.compile("(?is)<\\s*script\\b[^>]*>.*?<\\s*/\\s*script\\s*>");
    private static final Pattern UNCLOSED_SCRIPT_BLOCK = Pattern.compile("(?is)<\\s*script\\b[^>]*>.*");
    private static final Pattern EVENT_HANDLER_ATTRIBUTE = Pattern.compile(
            "(?i)\\s+on[a-z0-9_-]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");
    private static final Pattern DANGEROUS_PROTOCOL = Pattern.compile("(?i)\\b(?:javascript|vbscript)\\s*:");
    private static final Pattern HTML_TAG = Pattern.compile("(?is)</?\\s*[a-z][a-z0-9:-]*[^<>]*>");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    private InputSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String sanitized = SCRIPT_BLOCK.matcher(value).replaceAll("");
        sanitized = UNCLOSED_SCRIPT_BLOCK.matcher(sanitized).replaceAll("");
        sanitized = EVENT_HANDLER_ATTRIBUTE.matcher(sanitized).replaceAll("");
        sanitized = DANGEROUS_PROTOCOL.matcher(sanitized).replaceAll("");
        sanitized = HTML_TAG.matcher(sanitized).replaceAll("");
        return CONTROL_CHARS.matcher(sanitized).replaceAll("");
    }
}
