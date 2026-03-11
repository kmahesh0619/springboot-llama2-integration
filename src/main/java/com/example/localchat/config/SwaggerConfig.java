package com.example.localchat.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Springdoc / OpenAPI (Swagger) Configuration.
 *
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access OpenAPI JSON at: http://localhost:8080/api-docs
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .info(new Info()
                        .title("AI-Driven Factory Incident & SLA Management Platform")
                        .description("""
                                **Production-grade REST API** for factory incident management powered by AI (Llama-2).
                                
                                ## 🎯 Features
                                
                                - **AI-Powered Classification**: Natural language incident messages classified automatically
                                - **Automated SLA Management**: Severity-based deadlines (CRITICAL: 1m, HIGH: 2m, MEDIUM: 3m, LOW: 5m)
                                - **Department Routing**: Intelligent assignment to responsible teams
                                - **Suggested Actions**: AI-generated mitigation recommendations
                                - **Conversation History**: Full audit trail with comments and status changes
                                - **Real-time Monitoring**: SLA compliance tracking and escalation alerts
                                
                                ## 🚀 Authentication
                                
                                This API is secured with JWT. 
                                1. Register/Login via `/api/v1/auth`
                                2. Copy the token
                                3. Click **Authorize** button above and paste the token
                                """)
                        .version("2.1.0")
                        .contact(new Contact()
                                .name("Factory Incident Management")
                                .email("support@factory.ai"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/factory_incidents")
                                .description("Local Development")
                ));
    }
}
