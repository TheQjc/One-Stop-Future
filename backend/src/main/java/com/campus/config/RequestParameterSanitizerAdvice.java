package com.campus.config;

import java.beans.PropertyEditorSupport;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import com.campus.web.InputSanitizer;

@ControllerAdvice
public class RequestParameterSanitizerAdvice {

    @InitBinder
    public void sanitizeStringParameters(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new SanitizingStringEditor());
    }

    private static class SanitizingStringEditor extends PropertyEditorSupport {

        @Override
        public void setAsText(String text) {
            setValue(InputSanitizer.sanitize(text));
        }
    }
}
