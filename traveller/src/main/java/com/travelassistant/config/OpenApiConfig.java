package com.travelassistant.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
    @Bean OpenAPI travelApi() {
        return new OpenAPI().info(new Info().title("Travel Assistant API").version("1.0")
                .description("Mobile banking travel readiness, spending and fraud monitoring"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("Opaque banking session token")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
