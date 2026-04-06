package io.clroot.gradle.bun

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ConfigurationCacheFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    private fun runner(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*args, "--configuration-cache")
        .forwardOutput()

    @Test
    fun `bunSetup is configuration cache compatible`() {
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

        // First run: stores configuration cache
        runner("bunSetup").build()

        // Second run: reuses configuration cache
        val result = runner("bunSetup").build()

        assertThat(result.output).contains("Reusing configuration cache")
        assertThat(result.task(":bunSetup")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `BunTask is configuration cache compatible`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            import io.clroot.gradle.bun.task.BunTask

            plugins {
                id("io.clroot.gradle-bun")
            }
            bun {
                version = "1.2.0"
            }
            tasks.register<BunTask>("bunVersion") {
                args("--version")
            }
            """.trimIndent()
        )

        runner("bunVersion").build()

        val result = runner("bunVersion").build()

        assertThat(result.output).contains("Reusing configuration cache")
    }

    @Test
    fun `bunInstall is configuration cache compatible`() {
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
        projectDir.resolve("package.json").writeText(
            """
            {
                "name": "test-project",
                "version": "0.0.0"
            }
            """.trimIndent()
        )

        runner("bunInstall").build()

        val result = runner("bunInstall").build()

        assertThat(result.output).contains("Reusing configuration cache")
    }
}
