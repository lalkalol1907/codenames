package com.lalkalol.config

import com.lalkalol.web.security.RateLimitInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val rateLimitInterceptor: RateLimitInterceptor,
) : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns(
                "/api/rooms",
                "/api/rooms/join",
                "/api/rooms/*/join",
                "/api/rooms/*/role",
                "/api/rooms/*/randomize",
                "/api/rooms/*/start",
            )
    }
}
