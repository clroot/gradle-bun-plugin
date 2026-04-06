package io.clroot.gradle.bun.platform

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PlatformTest {

    @Nested
    inner class OsDetection {
        @Test
        fun `detects macOS from system property`() {
            assertThat(Os.from("Mac OS X")).isEqualTo(Os.DARWIN)
        }

        @Test
        fun `detects Linux from system property`() {
            assertThat(Os.from("Linux")).isEqualTo(Os.LINUX)
        }

        @Test
        fun `detects Windows from system property`() {
            assertThat(Os.from("Windows 11")).isEqualTo(Os.WINDOWS)
        }

        @Test
        fun `throws on unsupported OS`() {
            assertThatThrownBy { Os.from("FreeBSD") }
                .isInstanceOf(UnsupportedOperationException::class.java)
        }
    }

    @Nested
    inner class ArchDetection {
        @Test
        fun `detects x64 from amd64`() {
            assertThat(Arch.from("amd64")).isEqualTo(Arch.X64)
        }

        @Test
        fun `detects x64 from x86_64`() {
            assertThat(Arch.from("x86_64")).isEqualTo(Arch.X64)
        }

        @Test
        fun `detects aarch64 from aarch64`() {
            assertThat(Arch.from("aarch64")).isEqualTo(Arch.AARCH64)
        }

        @Test
        fun `detects aarch64 from arm64`() {
            assertThat(Arch.from("arm64")).isEqualTo(Arch.AARCH64)
        }

        @Test
        fun `throws on unsupported arch`() {
            assertThatThrownBy { Arch.from("mips") }
                .isInstanceOf(UnsupportedOperationException::class.java)
        }
    }

    @Nested
    inner class PlatformProperties {
        @Test
        fun `identifier for macOS arm64`() {
            val platform = Platform(Os.DARWIN, Arch.AARCH64)
            assertThat(platform.identifier).isEqualTo("bun-darwin-aarch64")
        }

        @Test
        fun `identifier for Linux x64`() {
            val platform = Platform(Os.LINUX, Arch.X64)
            assertThat(platform.identifier).isEqualTo("bun-linux-x64")
        }

        @Test
        fun `archive file name includes zip extension`() {
            val platform = Platform(Os.DARWIN, Arch.AARCH64)
            assertThat(platform.archiveFileName).isEqualTo("bun-darwin-aarch64.zip")
        }

        @Test
        fun `executable name is bun on Unix`() {
            val platform = Platform(Os.LINUX, Arch.X64)
            assertThat(platform.executableName).isEqualTo("bun")
        }

        @Test
        fun `executable name is bun_exe on Windows`() {
            val platform = Platform(Os.WINDOWS, Arch.X64)
            assertThat(platform.executableName).isEqualTo("bun.exe")
        }

        @Test
        fun `current detects running platform`() {
            val platform = Platform.current()
            assertThat(platform.os).isNotNull()
            assertThat(platform.arch).isNotNull()
        }
    }
}
