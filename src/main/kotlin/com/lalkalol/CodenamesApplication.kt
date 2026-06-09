package com.lalkalol

import com.lalkalol.config.AppProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class CodenamesApplication

fun main(args: Array<String>) {
    runApplication<CodenamesApplication>(*args)
}
