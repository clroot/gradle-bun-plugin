package io.clroot.gradle.bun

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BunxTaskFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    @Test
    fun `BunxTask prepends x to args and runs`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            import io.clroot.gradle.bun.task.BunxTask

            plugins {
                id("io.clroot.gradle-bun")
            }
            bun {
                version = "1.2.0"
            }
            tasks.register<BunxTask>("runCowsay") {
                args("cowsay", "hello")
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("runCowsay")
            .forwardOutput()
            .build()

        assertThat(result.task(":runCowsay")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
}
