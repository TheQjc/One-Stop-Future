package com.campus.common;

import com.campus.dto.AdminJobImportValidationResponse;

public class JobImportValidationException extends RuntimeException {

    private final AdminJobImportValidationResponse response;

    public JobImportValidationException(AdminJobImportValidationResponse response) {
        super("job import validation failed");
        this.response = response;
    }

    public AdminJobImportValidationResponse response() {
        return response;
    }
}
