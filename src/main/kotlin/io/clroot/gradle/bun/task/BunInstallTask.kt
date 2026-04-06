package io.clroot.gradle.bun.task

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

abstract class BunInstallTask : AbstractBunExecTask() {
    @get:Input
    abstract val frozenLockfile: Property<Boolean>

    init {
        frozenLockfile.convention(true)
    }

    override fun getCommand(): List<String> = buildList {
        add("install")
        if (frozenLockfile.get()) {
            add("--frozen-lockfile")
        }
    }
}
