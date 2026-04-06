package io.clroot.gradle.bun.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class AbstractBunExecTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Internal
    abstract val bunExecutable: RegularFileProperty

    @get:Internal
    abstract val workingDir: DirectoryProperty

    @get:Input
    abstract val args: ListProperty<String>

    fun args(vararg values: String) {
        args.addAll(values.toList())
    }

    protected open fun getCommand(): List<String> = args.get()

    @TaskAction
    fun execute() {
        val commandArgs = getCommand()
        execOperations.exec { spec ->
            spec.executable(bunExecutable.get().asFile.absolutePath)
            spec.args(commandArgs)
            spec.workingDir(this@AbstractBunExecTask.workingDir.get().asFile)
        }
    }
}
