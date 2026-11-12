package io.clroot.gradle.bun

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

abstract class BunFrontendExtension @Inject constructor(
    objects: ObjectFactory,
    private val registerCheckScript: (String) -> Unit,
) {
    /** package.json script executed by `bunBuild` (default: "build"). */
    val buildScript: Property<String> = objects.property(String::class.java)

    /** Directory produced by the build script (default: `<bun.workingDir>/dist`). */
    val outputDir: DirectoryProperty = objects.directoryProperty()

    /**
     * Extra Ant-style patterns, relative to `bun.workingDir`, excluded from `bunBuild` inputs.
     * `node_modules`, `.gradle`, `build` and [outputDir] are always excluded.
     */
    val inputExcludes: SetProperty<String> = objects.setProperty(String::class.java)

    /** Registers a `bun run <script>` task for each script and wires it into `check`. */
    fun checkScripts(vararg scripts: String) {
        scripts.forEach(registerCheckScript)
    }
}
