package com.campus.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void frameworkExceptionsReturnLocalizedMessages() {
        assertThat(handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("PATCH"))
                .getBody().message()).isEqualTo("请求方法不支持");

        assertThat(handler.handleMissingParam(new MissingServletRequestParameterException("keyword", "String"))
                .message()).isEqualTo("缺少必填参数：keyword");

        MethodArgumentTypeMismatchException typeMismatch = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", null, new NumberFormatException("For input string: abc"));
        assertThat(handler.handleTypeMismatch(typeMismatch).message()).isEqualTo("参数类型不正确：id");

        assertThat(handler.handleMaxUploadSize(new MaxUploadSizeExceededException(1024))
                .getBody().message()).isEqualTo("文件大小超出限制");

        assertThat(handler.handleMediaTypeNotSupported(new HttpMediaTypeNotSupportedException("text/plain"))
                .getBody().message()).isEqualTo("不支持的请求内容类型");

        assertThat(handler.handleUnexpected(new RuntimeException("boom")).message())
                .isEqualTo("服务器开小差了，请稍后再试");
    }
}
