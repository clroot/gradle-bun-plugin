package io.clroot.gradle.bun.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class BunSetupTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val downloadBaseUrl: Property<String>

    @get:OutputDirectory
    abstract val installDir: DirectoryProperty

    @TaskAction
    fun setup() {
        // Stub — real implementation in Task 4
    }
}
