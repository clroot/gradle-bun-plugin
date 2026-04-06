package io.clroot.gradle.bun

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class BunExtension @Inject constructor(objects: ObjectFactory) {
    val version: Property<String> = objects.property(String::class.java)

    val workingDir: DirectoryProperty = objects.directoryProperty()

    val installDir: DirectoryProperty = objects.directoryProperty()

    val useSystemBun: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    val downloadBaseUrl: Property<String> = objects.property(String::class.java)
        .convention("https://github.com/oven-sh/bun/releases/download")
}
