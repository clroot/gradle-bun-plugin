package io.clroot.gradle.bun

import io.clroot.gradle.bun.task.BunTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.PathSensitivity
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Turns a project into a frontend module: `bunBuild` is wired into `assemble`, its output
 * is exposed through the consumable `bunDist` configuration, and `clean` removes it.
 */
class BunFrontendPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(BasePlugin::class.java)
        project.pluginManager.apply(BunPlugin::class.java)

        val bun = project.extensions.getByType(BunExtension::class.java)
        val tasks = project.tasks
        val bunInstall = tasks.named(BunPlugin.BUN_INSTALL_TASK_NAME)

        val registerCheckScript: (String) -> Unit = { script ->
            val task = tasks.register(checkTaskName(script), BunTask::class.java) { t ->
                t.group = LifecycleBasePlugin.VERIFICATION_GROUP
                t.description = "Runs 'bun run $script'."
                t.dependsOn(bunInstall)
                t.args("run", script)
            }
            tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { it.dependsOn(task) }
        }

        val extension = project.extensions.create(
            "bunFrontend",
            BunFrontendExtension::class.java,
            registerCheckScript,
        )
        extension.buildScript.convention("build")
        extension.outputDir.convention(bun.workingDir.dir("dist"))

        val objects = project.objects
        val bunBuild = tasks.register(BUN_BUILD_TASK_NAME, BunTask::class.java) { t ->
            t.group = LifecycleBasePlugin.BUILD_GROUP
            t.description = "Runs the frontend build script and produces bunFrontend.outputDir."
            t.dependsOn(bunInstall)
            t.args.set(extension.buildScript.map { listOf("run", it) })

            val workingDir = bun.workingDir.get().asFile
            val outputDir = extension.outputDir.get().asFile
            val sources = objects.fileTree().from(workingDir)
            sources.exclude(DEFAULT_INPUT_EXCLUDES)
            sources.exclude(extension.inputExcludes.get())
            if (outputDir.startsWith(workingDir) && outputDir != workingDir) {
                sources.exclude(outputDir.relativeTo(workingDir).invariantSeparatorsPath + "/**")
            }
            t.inputs.files(sources)
                .withPropertyName("sources")
                .withPathSensitivity(PathSensitivity.RELATIVE)
                .ignoreEmptyDirectories()
            t.outputs.dir(extension.outputDir).withPropertyName("outputDir")
        }

        tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME) { it.dependsOn(bunBuild) }
        tasks.named(BasePlugin.CLEAN_TASK_NAME, Delete::class.java) { it.delete(extension.outputDir) }

        project.configurations.create(BUN_DIST_CONFIGURATION_NAME) { c ->
            c.description = "Frontend build output produced by $BUN_BUILD_TASK_NAME."
            c.isCanBeConsumed = true
            c.isCanBeResolved = false
            c.attributes.attribute(
                Category.CATEGORY_ATTRIBUTE,
                project.objects.named(Category::class.java, BUN_DIST_CATEGORY),
            )
        }
        project.artifacts.add(BUN_DIST_CONFIGURATION_NAME, extension.outputDir) { it.builtBy(bunBuild) }
    }

    companion object {
        const val BUN_BUILD_TASK_NAME = "bunBuild"
        const val BUN_DIST_CONFIGURATION_NAME = "bunDist"
        const val BUN_DIST_CATEGORY = "bun-dist"

        private val DEFAULT_INPUT_EXCLUDES = listOf("node_modules/**", ".gradle/**", "build/**")

        internal fun checkTaskName(script: String): String =
            "bun" + script.split(Regex("[^A-Za-z0-9]+"))
                .filter { it.isNotEmpty() }
                .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    }
}
