package com.lalkalol.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "app", name = ["expose-api-docs"], havingValue = "true")
class SpringDocConfig {
    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("Codenames")
                .version("1.0.0"),
        )
}
