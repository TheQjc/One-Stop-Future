package com.campus.config;

import java.io.IOException;

import org.springframework.boot.jackson.JsonComponent;

import com.campus.web.InputSanitizer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

@JsonComponent
public class JacksonInputSanitizerConfig {

    public static class SanitizingStringDeserializer extends JsonDeserializer<String> {

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return InputSanitizer.sanitize(parser.getValueAsString());
        }
    }
}
