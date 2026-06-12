plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    id("com.github.node-gradle.node") version "7.1.0"
}

group = "com.lalkalol"
version = "1.0.0-SNAPSHOT"

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
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.session.data.redis)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql)
    runtimeOnly(libs.h2)
    implementation(libs.springdoc.openapi)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.micrometer.registry.prometheus)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
