package io.clroot.gradle.bun

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BunTaskFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    @Test
    fun `BunTask runs bun command and prints version`() {
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

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunVersion")
            .forwardOutput()
            .build()

        assertThat(result.task(":bunVersion")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("1.2.0")
    }

    @Test
    fun `BunTask automatically depends on bunSetup`() {
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

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunVersion")
            .forwardOutput()
            .build()

        assertThat(result.task(":bunSetup")?.outcome).isIn(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE)
        assertThat(result.task(":bunVersion")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `bun invoked from a package script resolves to the managed binary`() {
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
            tasks.register<BunTask>("nested") {
                args("run", "nested")
            }
            """.trimIndent()
        )
        projectDir.resolve("package.json").writeText(
            """
            {
                "name": "test-project",
                "version": "0.0.0",
                "scripts": {
                    "nested": "bun -e \"console.log('nested-bun=' + process.execPath)\""
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("nested")
            .forwardOutput()
            .build()

        val managedDir = projectDir.resolve(".gradle/bun/1.2.0").canonicalPath
        val execPath = result.output.lines().first { it.startsWith("nested-bun=") }.removePrefix("nested-bun=")
        assertThat(File(execPath).canonicalPath).startsWith(managedDir)
    }
}
