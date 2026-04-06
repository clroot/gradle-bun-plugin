package io.clroot.gradle.bun.task

abstract class BunTask : AbstractBunExecTask() {
    init {
        description = "Executes a bun command."
    }
}
