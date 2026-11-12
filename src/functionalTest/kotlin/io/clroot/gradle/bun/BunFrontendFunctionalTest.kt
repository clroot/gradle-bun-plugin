package io.clroot.gradle.bun

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BunFrontendFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private fun runner(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*args)
        .forwardOutput()

    /** A frontend whose build script copies src/index.html into dist/. */
    private fun writeFrontend(dir: File, extraBuild: String = "", scripts: String = "") {
        dir.mkdirs()
        dir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("io.clroot.gradle-bun-frontend")
            }
            bun {
                version = "1.2.0"
            }
            $extraBuild
            """.trimIndent()
        )
        dir.resolve("package.json").writeText(
            """
            {
                "name": "frontend",
                "version": "0.0.0",
                "scripts": {
                    "build": "bun ./build.js"$scripts
                }
            }
            """.trimIndent()
        )
        dir.resolve("build.js").writeText(
            """
            import { mkdirSync, copyFileSync } from "node:fs";
            mkdirSync("dist", { recursive: true });
            copyFileSync("src/index.html", "dist/index.html");
            """.trimIndent()
        )
        dir.resolve("src").mkdirs()
        dir.resolve("src/index.html").writeText("<h1>hello</h1>")
    }

    @Test
    fun `assemble runs bunBuild and produces dist`() {
        projectDir.resolve("settings.gradle.kts").writeText("")
        writeFrontend(projectDir)

        val result = runner("assemble").build()

        assertThat(result.task(":bunInstall")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":bunBuild")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("dist/index.html")).hasContent("<h1>hello</h1>")
    }

    @Test
    fun `bunBuild is up to date until sources change`() {
        projectDir.resolve("settings.gradle.kts").writeText("")
        writeFrontend(projectDir)

        runner("bunBuild").build()
        val second = runner("bunBuild").build()
        assertThat(second.task(":bunBuild")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)

        projectDir.resolve("src/index.html").writeText("<h1>changed</h1>")
        val third = runner("bunBuild").build()
        assertThat(third.task(":bunBuild")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("dist/index.html")).hasContent("<h1>changed</h1>")
    }

    @Test
    fun `clean deletes output directory`() {
        projectDir.resolve("settings.gradle.kts").writeText("")
        writeFrontend(projectDir)

        runner("bunBuild").build()
        assertThat(projectDir.resolve("dist")).exists()

        runner("clean").build()
        assertThat(projectDir.resolve("dist")).doesNotExist()
    }

    @Test
    fun `buildScript and outputDir are configurable`() {
        projectDir.resolve("settings.gradle.kts").writeText("")
        writeFrontend(
            projectDir,
            extraBuild = """
            bunFrontend {
                buildScript = "bundle"
                outputDir = layout.projectDirectory.dir("out")
            }
            """.trimIndent(),
            scripts = """,
                    "bundle": "bun -e \"require('fs').mkdirSync('out', {recursive: true}); require('fs').writeFileSync('out/app.js', 'ok')\""
            """.trimEnd(),
        )

        runner("assemble").build()

        assertThat(projectDir.resolve("out/app.js")).hasContent("ok")
        assertThat(projectDir.resolve("dist")).doesNotExist()
    }

    @Test
    fun `checkScripts are wired into check`() {
        projectDir.resolve("settings.gradle.kts").writeText("")
        writeFrontend(
            projectDir,
            extraBuild = """
            bunFrontend {
                checkScripts("lint", "format:check")
            }
            """.trimIndent(),
            scripts = """,
                    "lint": "bun -e \"console.log('lint-ran')\"",
                    "format:check": "bun -e \"console.log('format-ran')\""
            """.trimEnd(),
        )

        val result = runner("check").build()

        assertThat(result.task(":bunLint")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":bunFormatCheck")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("lint-ran", "format-ran")
    }

    @Test
    fun `failing check script fails the build`() {
        projectDir.resolve("settings.gradle.kts").writeText("")
        writeFrontend(
            projectDir,
            extraBuild = """
            bunFrontend {
                checkScripts("test")
            }
            """.trimIndent(),
            scripts = """,
                    "test": "bun -e \"process.exit(1)\""
            """.trimEnd(),
        )

        val result = runner("check").buildAndFail()

        assertThat(result.task(":bunTest")?.outcome).isEqualTo(TaskOutcome.FAILED)
    }

    private fun writeMultiProject() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "app"
            include(":frontend")
            """.trimIndent()
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                java
            }
            val frontendDist by configurations.creating {
                isCanBeConsumed = false
            }
            dependencies {
                frontendDist(project(path = ":frontend", configuration = "bunDist"))
            }
            tasks.processResources {
                from(frontendDist) { into("static") }
            }
            """.trimIndent()
        )
        writeFrontend(projectDir.resolve("frontend"))
    }

    @Test
    fun `consumer project receives frontend output via bunDist`() {
        writeMultiProject()

        val result = runner("processResources").build()

        assertThat(result.task(":frontend:bunBuild")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/resources/main/static/index.html")).hasContent("<h1>hello</h1>")
    }

    @Test
    fun `multi-project setup is configuration cache compatible`() {
        writeMultiProject()

        runner("processResources", "--configuration-cache").build()
        projectDir.resolve("frontend/src/index.html").writeText("<h1>cached</h1>")
        val result = runner("processResources", "--configuration-cache").build()

        assertThat(result.output).contains("Reusing configuration cache")
        assertThat(result.task(":frontend:bunBuild")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/resources/main/static/index.html")).hasContent("<h1>cached</h1>")
    }
}
