package io.clroot.gradle.bun.task

import io.clroot.gradle.bun.platform.Os
import io.clroot.gradle.bun.platform.Platform
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import javax.inject.Inject

abstract class BunSetupTask : DefaultTask() {
    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val downloadBaseUrl: Property<String>

    @get:OutputDirectory
    abstract val installDir: DirectoryProperty

    @TaskAction
    fun setup() {
        val platform = Platform.current()
        val targetDir = installDir.get().asFile
        val executableFile = targetDir.resolve(platform.identifier).resolve(platform.executableName)

        if (executableFile.exists() && executableFile.canExecute()) {
            return
        }

        val url = "${downloadBaseUrl.get()}/bun-v${version.get()}/${platform.archiveFileName}"
        val tempFile = temporaryDir.resolve(platform.archiveFileName)

        logger.lifecycle("Downloading bun ${version.get()} from $url")

        URI(url).toURL().openStream().use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        logger.lifecycle("Extracting to ${targetDir.absolutePath}")

        fs.copy { spec ->
            spec.from(archiveOperations.zipTree(tempFile))
            spec.into(targetDir)
        }

        if (platform.os != Os.WINDOWS) {
            executableFile.setExecutable(true)
        }

        tempFile.delete()
    }
}
