plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.plugin.publish)
}

group = property("group") as String
version = property("version") as String

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

gradlePlugin {
    website = "https://github.com/clroot/gradle-bun-plugin"
    vcsUrl = "https://github.com/clroot/gradle-bun-plugin"

    plugins {
        create("bun") {
            id = "io.clroot.gradle-bun"
            displayName = "Gradle Bun Plugin"
            description = "Gradle plugin for managing Bun JavaScript runtime - auto-download, install, and run bun commands"
            tags = listOf("bun", "javascript", "frontend", "build")
            implementationClass = "io.clroot.gradle.bun.BunPlugin"
        }
    }
}

val functionalTest: SourceSet = sourceSets.create("functionalTest")

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

val functionalTestTask = tasks.register<Test>("functionalTest") {
    group = "verification"
    description = "Runs functional tests."
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets(functionalTest)

tasks.check {
    dependsOn(functionalTestTask)
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}
