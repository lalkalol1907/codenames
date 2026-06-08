plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(ktorLibs.plugins.ktor)
    id("com.github.node-gradle.node") version "7.1.0"
}

group = "com.lalkalol"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(24)
}

node {
    version.set("26.3.0")
    pnpmVersion.set("10.11.0")
    download.set(true)
    nodeProjectDir.set(file("frontend"))
    workDir.set(file("${project.projectDir}/.gradle/nodejs"))
    npmWorkDir.set(file("${project.projectDir}/.gradle/npm"))
}

val skipFrontendBuild = project.hasProperty("skipFrontendBuild")

val viteBuild by tasks.registering(com.github.gradle.node.pnpm.task.PnpmTask::class) {
    dependsOn(tasks.pnpmInstall)
    pnpmCommand.set(listOf("run", "build"))
    onlyIf { !skipFrontendBuild }
}

tasks.named("processResources") {
    if (!skipFrontendBuild) {
        dependsOn(viteBuild)
    }
}

if (skipFrontendBuild) {
    tasks.named("nodeSetup") { enabled = false }
    tasks.named("pnpmSetup") { enabled = false }
    tasks.named("pnpmInstall") { enabled = false }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.di)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.openapi)
    implementation(ktorLibs.server.routingOpenapi)
    implementation(ktorLibs.server.websockets)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.sessions)
    implementation(libs.logback.classic)
    implementation(libs.openfolder.kotlinAsyncapiKtor)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.h2)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.compression)
    implementation(ktorLibs.server.defaultHeaders)
    implementation(ktorLibs.server.rateLimit)
    implementation(ktorLibs.server.statusPages)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
