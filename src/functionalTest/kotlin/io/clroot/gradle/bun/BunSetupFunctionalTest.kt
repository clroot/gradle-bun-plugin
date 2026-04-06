package io.clroot.gradle.bun

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BunSetupFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    private fun writeBuildFile(version: String = "1.2.0") {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id("io.clroot.gradle-bun")
            }
            bun {
                version = "$version"
                installDir = layout.projectDirectory.dir(".gradle/bun")
            }
            """.trimIndent()
        )
    }

    @Test
    fun `bunSetup downloads bun binary`() {
        writeBuildFile()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunSetup")
            .forwardOutput()
            .build()

        assertThat(result.task(":bunSetup")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        // Verify binary exists somewhere under .gradle/bun/1.2.0/
        val bunDir = projectDir.resolve(".gradle/bun/1.2.0")
        assertThat(bunDir).isDirectory()
        val bunFiles = bunDir.walk().filter { it.name == "bun" || it.name == "bun.exe" }.toList()
        assertThat(bunFiles).isNotEmpty()
        assertThat(bunFiles.first().canExecute()).isTrue()
    }

    @Test
    fun `bunSetup is UP-TO-DATE on second run`() {
        writeBuildFile()

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunSetup")
            .forwardOutput()
            .build()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunSetup")
            .forwardOutput()
            .build()

        assertThat(result.task(":bunSetup")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }
}
