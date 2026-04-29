package com.campus.config;

import java.io.IOException;
import java.util.stream.Collectors;

import org.apache.http.ConnectionClosedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.campus.common.BusinessException;
import com.campus.common.JobImportValidationException;
import com.campus.common.Result;
import com.campus.dto.AdminJobImportValidationResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        return Result.error(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(JobImportValidationException.class)
    public Result<AdminJobImportValidationResponse> handleJobImportValidationException(
            JobImportValidationException exception) {
        return Result.error(400, "job import validation failed", exception.response());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException exception) {
        String msg = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return Result.error(400, msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleBadJson(HttpMessageNotReadableException exception) {
        return Result.error(400, "invalid request");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException exception) {
        return Result.error(400, exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(403, "forbidden"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error(404, "not found"));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpected(Exception exception) {
        return Result.error(500, "internal server error");
    }

    @ExceptionHandler(IOException.class)
    public Result<Void> handleIOException(IOException exception) {
        log.error("Elasticsearch connection error, search will fallback to MySQL", exception);
        return Result.error(503, "search service temporarily unavailable");
    }
}
