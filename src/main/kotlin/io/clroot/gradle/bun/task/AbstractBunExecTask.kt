package io.clroot.gradle.bun.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
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

    @Internal
    protected open fun getCommand(): List<String> = args.get()

    @TaskAction
    fun execute() {
        val commandArgs = getCommand()
        val executable = bunExecutable.get().asFile
        execOperations.exec { spec ->
            spec.executable(executable.absolutePath)
            // Scripts that call `bun` themselves must resolve to this binary, not a global install.
            val pathKey = spec.environment.keys.firstOrNull { it.equals("PATH", ignoreCase = true) } ?: "PATH"
            val currentPath = spec.environment[pathKey]?.toString().orEmpty()
            val bunDir = executable.parentFile.absolutePath
            spec.environment(
                pathKey,
                if (currentPath.isEmpty()) bunDir else bunDir + File.pathSeparator + currentPath,
            )
            spec.args(commandArgs)
            spec.workingDir(this@AbstractBunExecTask.workingDir.get().asFile)
        }
    }
}
