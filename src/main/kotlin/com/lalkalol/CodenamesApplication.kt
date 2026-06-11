package com.lalkalol

import com.lalkalol.config.AppProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
@EnableJpaRepositories(basePackages = ["com.lalkalol.db.jpa"])
@EnableScheduling
class CodenamesApplication

fun main(args: Array<String>) {
    runApplication<CodenamesApplication>(*args)
}
