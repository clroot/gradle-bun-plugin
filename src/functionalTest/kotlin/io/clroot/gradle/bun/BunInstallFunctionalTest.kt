package io.clroot.gradle.bun

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BunInstallFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    @Test
    fun `bunInstall runs with frozen lockfile by default`() {
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

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunInstall")
            .forwardOutput()
            .build()

        assertThat(result.task(":bunSetup")?.outcome).isIn(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE)
        assertThat(result.task(":bunInstall")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `bunInstall without frozen lockfile`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            import io.clroot.gradle.bun.task.BunInstallTask

            plugins {
                id("io.clroot.gradle-bun")
            }
            bun {
                version = "1.2.0"
            }
            tasks.named<BunInstallTask>("bunInstall") {
                frozenLockfile = false
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

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunInstall")
            .forwardOutput()
            .build()

        assertThat(result.task(":bunInstall")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
}
