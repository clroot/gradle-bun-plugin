package io.clroot.gradle.bun.platform

enum class Os(val classifier: String) {
    DARWIN("darwin"),
    LINUX("linux"),
    WINDOWS("windows");

    companion object {
        fun from(osName: String): Os {
            val name = osName.lowercase()
            return when {
                "mac" in name || "darwin" in name -> DARWIN
                "linux" in name -> LINUX
                "windows" in name -> WINDOWS
                else -> throw UnsupportedOperationException("Unsupported OS: $osName")
            }
        }

        fun current(): Os = from(System.getProperty("os.name"))
    }
}

enum class Arch(val classifier: String) {
    X64("x64"),
    AARCH64("aarch64");

    companion object {
        fun from(archName: String): Arch {
            val arch = archName.lowercase()
            return when (arch) {
                "amd64", "x86_64" -> X64
                "aarch64", "arm64" -> AARCH64
                else -> throw UnsupportedOperationException("Unsupported architecture: $archName")
            }
        }

        fun current(): Arch = from(System.getProperty("os.arch"))
    }
}

data class Platform(val os: Os, val arch: Arch) {
    val identifier: String get() = "bun-${os.classifier}-${arch.classifier}"
    val archiveFileName: String get() = "$identifier.zip"
    val executableName: String get() = if (os == Os.WINDOWS) "bun.exe" else "bun"

    companion object {
        fun current(): Platform = Platform(Os.current(), Arch.current())
    }
}
