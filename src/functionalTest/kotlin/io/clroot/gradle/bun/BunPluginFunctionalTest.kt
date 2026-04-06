package io.clroot.gradle.bun

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import org.assertj.core.api.Assertions.assertThat

class BunPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    @Test
    fun `plugin applies without error`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id("io.clroot.gradle-bun")
            }
            bun {
                version = "1.2.0"
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group=bun")
            .forwardOutput()
            .build()

        assertThat(result.output).contains("bunSetup")
        assertThat(result.output).contains("bunInstall")
    }

    @Test
    fun `plugin fails when version is not set`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id("io.clroot.gradle-bun")
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunSetup")
            .forwardOutput()
            .buildAndFail()

        assertThat(result.output).contains("version")
        assertThat(result.output).contains("doesn't have a configured value")
    }
}
