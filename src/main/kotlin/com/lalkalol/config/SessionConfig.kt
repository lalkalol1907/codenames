package com.lalkalol.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.session.web.http.DefaultCookieSerializer

@Configuration
class SessionConfig(private val appProperties: AppProperties) {

    @Bean
    fun cookieSerializer(): DefaultCookieSerializer =
        DefaultCookieSerializer().apply {
            setCookieName("player_session")
            setHttpOnly(true)
            setSameSite("Lax")
            setUseSecureCookie(appProperties.secureCookies)
            setCookiePath("/")
        }
}
