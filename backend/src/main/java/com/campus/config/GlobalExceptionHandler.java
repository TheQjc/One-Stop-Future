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
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.campus.common.BusinessException;
import com.campus.common.JobImportValidationException;
import com.campus.common.Result;
import com.campus.dto.AdminJobImportValidationError;
import com.campus.dto.AdminJobImportValidationResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        return Result.error(exception.getCode(), ApiErrorMessageLocalizer.localize(exception.getMessage()));
    }

    @ExceptionHandler(JobImportValidationException.class)
    public Result<AdminJobImportValidationResponse> handleJobImportValidationException(
            JobImportValidationException exception) {
        AdminJobImportValidationResponse response = exception.response();
        AdminJobImportValidationResponse localizedResponse = new AdminJobImportValidationResponse(
                response.fileName(),
                response.totalRows(),
                response.importedCount(),
                response.errors().stream()
                        .map(error -> new AdminJobImportValidationError(
                                error.rowNumber(),
                                error.column(),
                                ApiErrorMessageLocalizer.localize(error.message())))
                        .toList());
        return Result.error(400, "岗位导入校验失败", localizedResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException exception) {
        String msg = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> ApiErrorMessageLocalizer.localize(error.getDefaultMessage()))
                .distinct()
                .collect(Collectors.joining(", "));
        return Result.error(400, msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleBadJson(HttpMessageNotReadableException exception) {
        return Result.error(400, "请求参数无效");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException exception) {
        return Result.error(400, ApiErrorMessageLocalizer.localize(exception.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public Result<Void> handleMissingServletRequestPart(MissingServletRequestPartException exception) {
        return Result.error(400, ApiErrorMessageLocalizer.localize(exception.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(403, "没有权限执行该操作"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error(404, "请求的资源不存在"));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpected(Exception exception) {
        return Result.error(500, "服务器开小差了，请稍后再试");
    }

    @ExceptionHandler(IOException.class)
    public Result<Void> handleIOException(IOException exception) {
        log.error("Elasticsearch connection error, search will fallback to MySQL", exception);
        return Result.error(503, "搜索服务暂时不可用");
    }
}
