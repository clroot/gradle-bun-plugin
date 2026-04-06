package io.clroot.gradle.bun.task

abstract class BunxTask : AbstractBunExecTask() {
    init {
        description = "Executes a bunx command."
    }

    override fun getCommand(): List<String> = listOf("x") + args.get()
}
