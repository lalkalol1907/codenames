package com.lalkalol.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
class JacksonConfig {
    @Bean
    @Primary
    fun objectMapper(builder: Jackson2ObjectMapperBuilder): ObjectMapper =
        builder
            .modules(kotlinModule())
            .build<ObjectMapper>()
            .apply {
                setDefaultPropertyInclusion(
                    com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS,
                )
            }
}
