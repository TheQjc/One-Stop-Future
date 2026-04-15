package com.campus.common;

public enum UserRole {
    USER,
    ADMIN;

    public boolean canSelfRegister() {
        return this == USER;
    }
}
