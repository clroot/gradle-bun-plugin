package io.clroot.gradle.bun

import io.clroot.gradle.bun.platform.Platform
import io.clroot.gradle.bun.task.AbstractBunExecTask
import io.clroot.gradle.bun.task.BunInstallTask
import io.clroot.gradle.bun.task.BunSetupTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class BunPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("bun", BunExtension::class.java)

        extension.workingDir.convention(project.layout.projectDirectory)
        extension.installDir.convention(project.layout.projectDirectory.dir(".gradle/bun"))

        val platform = Platform.current()

        val bunExecutable = extension.useSystemBun.flatMap { useSystem ->
            if (useSystem) {
                project.providers.provider {
                    val path = resolveSystemBun()
                        ?: throw GradleException(
                            "bun not found on PATH. Install bun or set bun.useSystemBun = false to auto-download."
                        )
                    project.layout.projectDirectory.file(path)
                }
            } else {
                extension.installDir
                    .zip(extension.version) { dir, ver -> dir.dir(ver) }
                    .map { versionDir ->
                        versionDir.dir(platform.identifier).file(platform.executableName)
                    }
            }
        }

        val bunSetup = project.tasks.register("bunSetup", BunSetupTask::class.java) { task ->
            task.group = "bun"
            task.description = "Downloads and installs the Bun binary."
            task.version.set(extension.version)
            task.downloadBaseUrl.set(extension.downloadBaseUrl)
            task.installDir.set(
                extension.installDir.zip(extension.version) { dir, ver -> dir.dir(ver) }
            )
            task.onlyIf {
                !extension.useSystemBun.get()
            }
        }

        project.tasks.register("bunInstall", BunInstallTask::class.java) { task ->
            task.group = "bun"
            task.description = "Runs bun install."
            task.dependsOn(bunSetup)
            task.bunExecutable.set(bunExecutable)
            task.workingDir.set(extension.workingDir)
        }

        // Auto-wire all user-defined AbstractBunExecTask instances
        project.tasks.withType(AbstractBunExecTask::class.java).configureEach { task ->
            task.dependsOn(bunSetup)
            task.bunExecutable.convention(bunExecutable)
            task.workingDir.convention(extension.workingDir)
        }
    }

    private fun resolveSystemBun(): String? {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val cmd = if (isWindows) listOf("where", "bun") else listOf("which", "bun")
        return runCatching {
            ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()
                .inputStream
                .bufferedReader()
                .readLine()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
