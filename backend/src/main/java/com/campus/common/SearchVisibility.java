package com.campus.common;

public enum SearchVisibility {

    PUBLIC("public"),
    PRIVATE("private"),
    ADMIN("admin");

    private final String value;

    SearchVisibility(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static SearchVisibility fromStatus(String status, SearchContentType contentType) {
        if (contentType.isPublic()) {
            return PUBLIC;
        }
        if (contentType.isPrivate()) {
            return PRIVATE;
        }
        return ADMIN;
    }
}
