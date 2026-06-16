package com.campus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI campusOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("One-Stop Future 一站式升学管理平台")
                        .description("校园一站式成长平台 API 文档 — 聚合社区帖子、岗位招聘、学习资料、统一搜索、趋势分析、决策测评、通知中心、个人资料管理及管理端看板。")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("One-Stop Future Team")
                                .url("https://github.com/One-Stop-Future"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .addServersItem(new Server().url("/").description("Default"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("输入 JWT Token（通过 /api/auth/login 或 /api/auth/register 获取）")));
    }
}
