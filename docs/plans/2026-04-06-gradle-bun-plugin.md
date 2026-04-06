# gradle-bun-plugin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Gradle 빌드에서 bun을 자동 다운로드/설치/실행하는 플러그인을 개발하고 Gradle Plugin Portal에 퍼블리시한다.

**Architecture:** Single plugin (`io.clroot.gradle-bun`)이 Extension DSL로 설정을 받고, `BunSetupTask`로 바이너리를 다운로드하며, `BunTask`/`BunxTask`/`BunInstallTask`로 명령을 실행한다. 모든 태스크는 `DefaultTask` + `ExecOperations` 패턴으로 Configuration Cache를 완전 지원한다.

**Tech Stack:** Kotlin 2.1, Gradle 8.12 (wrapper), JUnit 5, Gradle TestKit, `com.gradle.plugin-publish` 2.0

**Spec:** `docs/specs/2026-04-06-gradle-bun-plugin-design.md`

---

## File Structure

```
gradle-bun-plugin/
├── build.gradle.kts                                          # 플러그인 빌드 설정
├── settings.gradle.kts                                       # 프로젝트 이름
├── gradle.properties                                         # group, version
├── gradle/libs.versions.toml                                 # 버전 카탈로그
├── .gitignore
├── src/
│   ├── main/kotlin/io/clroot/gradle/bun/
│   │   ├── BunPlugin.kt                                     # 플러그인 진입점
│   │   ├── BunExtension.kt                                  # DSL 정의
│   │   ├── platform/
│   │   │   └── Platform.kt                                  # OS/Arch 모델 + 감지
│   │   └── task/
│   │       ├── AbstractBunExecTask.kt                        # bun 실행 기반 태스크
│   │       ├── BunSetupTask.kt                               # 다운로드 & 설치
│   │       ├── BunTask.kt                                    # 범용 bun 명령
│   │       ├── BunxTask.kt                                   # bunx 명령
│   │       └── BunInstallTask.kt                             # bun install
│   ├── test/kotlin/io/clroot/gradle/bun/
│   │   └── platform/
│   │       └── PlatformTest.kt                               # 플랫폼 감지 단위 테스트
│   └── functionalTest/kotlin/io/clroot/gradle/bun/
│       ├── BunPluginFunctionalTest.kt                        # 플러그인 적용 테스트
│       ├── BunSetupFunctionalTest.kt                         # 다운로드 테스트
│       ├── BunTaskFunctionalTest.kt                          # 명령 실행 테스트
│       ├── BunxTaskFunctionalTest.kt                         # bunx 테스트
│       ├── BunInstallFunctionalTest.kt                       # install 테스트
│       └── ConfigurationCacheFunctionalTest.kt               # CC 호환 테스트
└── .github/workflows/
    ├── ci.yml                                                # PR 검증
    └── publish.yml                                           # 릴리즈 퍼블리시
```

---

### Task 1: Project Scaffolding

**Files:**
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `build.gradle.kts`
- Create: `.gitignore`

- [ ] **Step 1: Git 초기화**

```bash
cd /Users/clroot/Documents/projects/io.clroot/gradle-bun-plugin
git init
```

- [ ] **Step 2: `.gitignore` 작성**

```gitignore
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IDE
.idea/
*.iml
.vscode/

# OS
.DS_Store
```

- [ ] **Step 3: `settings.gradle.kts` 작성**

```kotlin
rootProject.name = "gradle-bun-plugin"
```

- [ ] **Step 4: `gradle.properties` 작성**

```properties
group=io.clroot
version=0.1.0-SNAPSHOT
```

- [ ] **Step 5: `gradle/libs.versions.toml` 작성**

```toml
[versions]
kotlin = "2.1.0"
plugin-publish = "2.0.0"
junit = "5.11.4"
assertj = "3.27.3"

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
plugin-publish = { id = "com.gradle.plugin-publish", version.ref = "plugin-publish" }

[libraries]
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher" }
assertj-core = { module = "org.assertj:assertj-core", version.ref = "assertj" }
```

- [ ] **Step 6: `build.gradle.kts` 작성**

```kotlin
plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.plugin.publish)
}

group = property("group") as String
version = property("version") as String

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

gradlePlugin {
    website = "https://github.com/clroot/gradle-bun-plugin"
    vcsUrl = "https://github.com/clroot/gradle-bun-plugin"

    plugins {
        create("bun") {
            id = "io.clroot.gradle-bun"
            displayName = "Gradle Bun Plugin"
            description = "Gradle plugin for managing Bun JavaScript runtime - auto-download, install, and run bun commands"
            tags = listOf("bun", "javascript", "frontend", "build")
            implementationClass = "io.clroot.gradle.bun.BunPlugin"
        }
    }
}

val functionalTest: SourceSet = sourceSets.create("functionalTest")

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

val functionalTestTask = tasks.register<Test>("functionalTest") {
    group = "verification"
    description = "Runs functional tests."
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets(functionalTest)

tasks.check {
    dependsOn(functionalTestTask)
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}
```

- [ ] **Step 7: Gradle Wrapper 생성**

```bash
gradle wrapper --gradle-version 8.12
```

- [ ] **Step 8: 빌드 확인**

Run: `./gradlew tasks --all`
Expected: 빌드 성공, `functionalTest` 태스크가 목록에 존재

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "chore: initialize gradle-bun-plugin project scaffold"
```

---

### Task 2: Platform Model + Unit Tests

**Files:**
- Create: `src/main/kotlin/io/clroot/gradle/bun/platform/Platform.kt`
- Create: `src/test/kotlin/io/clroot/gradle/bun/platform/PlatformTest.kt`

- [ ] **Step 1: Failing test 작성**

`src/test/kotlin/io/clroot/gradle/bun/platform/PlatformTest.kt`:
```kotlin
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
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test`
Expected: FAIL — `Os`, `Arch`, `Platform` 클래스가 없음

- [ ] **Step 3: Platform 모델 구현**

`src/main/kotlin/io/clroot/gradle/bun/platform/Platform.kt`:
```kotlin
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
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test`
Expected: ALL PASSED

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/io/clroot/gradle/bun/platform/Platform.kt \
       src/test/kotlin/io/clroot/gradle/bun/platform/PlatformTest.kt
git commit -m "feat: add Platform model with OS/Arch detection"
```

---

### Task 3: BunExtension + BunPlugin Skeleton

**Files:**
- Create: `src/main/kotlin/io/clroot/gradle/bun/BunExtension.kt`
- Create: `src/main/kotlin/io/clroot/gradle/bun/BunPlugin.kt`
- Create: `src/functionalTest/kotlin/io/clroot/gradle/bun/BunPluginFunctionalTest.kt`

- [ ] **Step 1: Functional test 작성 — 플러그인 적용**

`src/functionalTest/kotlin/io/clroot/gradle/bun/BunPluginFunctionalTest.kt`:
```kotlin
package io.clroot.gradle.bun

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import org.assertj.core.api.Assertions.assertThat

class BunPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    @Test
    fun `plugin applies without error`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id("io.clroot.gradle-bun")
            }
            bun {
                version = "1.2.0"
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group=bun")
            .forwardOutput()
            .build()

        assertThat(result.output).contains("bunSetup")
        assertThat(result.output).contains("bunInstall")
    }

    @Test
    fun `plugin fails when version is not set`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id("io.clroot.gradle-bun")
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunSetup")
            .forwardOutput()
            .buildAndFail()

        assertThat(result.output).contains("version")
        assertThat(result.output).contains("doesn't have a configured value")
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew functionalTest`
Expected: FAIL — `BunPlugin`, `BunExtension` 클래스 없음

- [ ] **Step 3: BunExtension 구현**

`src/main/kotlin/io/clroot/gradle/bun/BunExtension.kt`:
```kotlin
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
```

- [ ] **Step 4: BunPlugin 스켈레톤 구현**

`src/main/kotlin/io/clroot/gradle/bun/BunPlugin.kt`:
```kotlin
package io.clroot.gradle.bun

import io.clroot.gradle.bun.platform.Platform
import io.clroot.gradle.bun.task.BunInstallTask
import io.clroot.gradle.bun.task.BunSetupTask
import io.clroot.gradle.bun.task.AbstractBunExecTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class BunPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("bun", BunExtension::class.java)

        extension.workingDir.convention(project.layout.projectDirectory)
        extension.installDir.convention(project.layout.projectDirectory.dir(".gradle/bun"))

        val platform = Platform.current()

        val bunExecutable = extension.useSystemBun.flatMap { useSystem ->
            if (useSystem) {
                project.providers.provider {
                    val path = resolveSystemBun()
                        ?: throw GradleException(
                            "bun not found on PATH. Install bun or set bun.useSystemBun = false to auto-download."
                        )
                    project.layout.projectDirectory.file(path)
                }
            } else {
                extension.installDir
                    .zip(extension.version) { dir, ver -> dir.dir(ver) }
                    .map { versionDir ->
                        versionDir.dir(platform.identifier).file(platform.executableName)
                    }
            }
        }

        val bunSetup = project.tasks.register("bunSetup", BunSetupTask::class.java) { task ->
            task.group = "bun"
            task.description = "Downloads and installs the Bun binary."
            task.version.set(extension.version)
            task.downloadBaseUrl.set(extension.downloadBaseUrl)
            task.installDir.set(
                extension.installDir.zip(extension.version) { dir, ver -> dir.dir(ver) }
            )
            task.onlyIf {
                !extension.useSystemBun.get()
            }
        }

        project.tasks.register("bunInstall", BunInstallTask::class.java) { task ->
            task.group = "bun"
            task.description = "Runs bun install."
            task.dependsOn(bunSetup)
            task.bunExecutable.set(bunExecutable)
            task.workingDir.set(extension.workingDir)
        }

        // Auto-wire all user-defined BunTask/BunxTask instances
        project.tasks.withType(AbstractBunExecTask::class.java).configureEach { task ->
            task.dependsOn(bunSetup)
            task.bunExecutable.convention(bunExecutable)
            task.workingDir.convention(extension.workingDir)
        }
    }

    private fun resolveSystemBun(): String? {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val cmd = if (isWindows) listOf("where", "bun") else listOf("which", "bun")
        return runCatching {
            ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()
                .inputStream
                .bufferedReader()
                .readLine()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
```

- [ ] **Step 5: BunSetupTask / AbstractBunExecTask / BunInstallTask 빈 클래스 생성**

컴파일을 통과시키기 위해 빈 껍데기만 생성한다. 실제 구현은 이후 태스크에서 한다.

`src/main/kotlin/io/clroot/gradle/bun/task/BunSetupTask.kt`:
```kotlin
package io.clroot.gradle.bun.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class BunSetupTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val downloadBaseUrl: Property<String>

    @get:OutputDirectory
    abstract val installDir: DirectoryProperty

    @TaskAction
    fun setup() {
        // TODO: implement in Task 4
    }
}
```

`src/main/kotlin/io/clroot/gradle/bun/task/AbstractBunExecTask.kt`:
```kotlin
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
```

`src/main/kotlin/io/clroot/gradle/bun/task/BunInstallTask.kt`:
```kotlin
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
```

- [ ] **Step 6: Functional test 통과 확인**

Run: `./gradlew functionalTest`
Expected: ALL PASSED

- [ ] **Step 7: Commit**

```bash
git add src/main/kotlin/io/clroot/gradle/bun/ \
       src/functionalTest/kotlin/io/clroot/gradle/bun/BunPluginFunctionalTest.kt
git commit -m "feat: add BunPlugin skeleton with extension DSL and task registration"
```

---

### Task 4: BunSetupTask — 바이너리 다운로드 & 설치

**Files:**
- Modify: `src/main/kotlin/io/clroot/gradle/bun/task/BunSetupTask.kt`
- Create: `src/functionalTest/kotlin/io/clroot/gradle/bun/BunSetupFunctionalTest.kt`

- [ ] **Step 1: Functional test 작성**

`src/functionalTest/kotlin/io/clroot/gradle/bun/BunSetupFunctionalTest.kt`:
```kotlin
package io.clroot.gradle.bun

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BunSetupFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    private fun writeBuildFile(version: String = "1.2.0") {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id("io.clroot.gradle-bun")
            }
            bun {
                version = "$version"
                installDir = layout.projectDirectory.dir(".gradle/bun")
            }
            """.trimIndent()
        )
    }

    @Test
    fun `bunSetup downloads bun binary`() {
        writeBuildFile()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunSetup")
            .forwardOutput()
            .build()

        assertThat(result.task(":bunSetup")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        // Verify binary exists somewhere under .gradle/bun/1.2.0/
        val bunDir = projectDir.resolve(".gradle/bun/1.2.0")
        assertThat(bunDir).isDirectory()
        val bunFiles = bunDir.walk().filter { it.name == "bun" || it.name == "bun.exe" }.toList()
        assertThat(bunFiles).isNotEmpty()
        assertThat(bunFiles.first().canExecute()).isTrue()
    }

    @Test
    fun `bunSetup is UP-TO-DATE on second run`() {
        writeBuildFile()

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunSetup")
            .forwardOutput()
            .build()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunSetup")
            .forwardOutput()
            .build()

        assertThat(result.task(":bunSetup")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew functionalTest --tests '*BunSetupFunctionalTest*'`
Expected: FAIL — `bunSetup`이 빈 `setup()` 메서드로 바이너리를 다운로드하지 않음

- [ ] **Step 3: BunSetupTask 구현**

`src/main/kotlin/io/clroot/gradle/bun/task/BunSetupTask.kt`를 전체 교체:
```kotlin
package io.clroot.gradle.bun.task

import io.clroot.gradle.bun.platform.Platform
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import javax.inject.Inject

abstract class BunSetupTask : DefaultTask() {
    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val downloadBaseUrl: Property<String>

    @get:OutputDirectory
    abstract val installDir: DirectoryProperty

    @TaskAction
    fun setup() {
        val platform = Platform.current()
        val targetDir = installDir.get().asFile
        val executableFile = targetDir.resolve(platform.identifier).resolve(platform.executableName)

        if (executableFile.exists() && executableFile.canExecute()) {
            return
        }

        val url = "${downloadBaseUrl.get()}/bun-v${version.get()}/${platform.archiveFileName}"
        val tempFile = temporaryDir.resolve(platform.archiveFileName)

        logger.lifecycle("Downloading bun ${version.get()} from $url")

        URI(url).toURL().openStream().use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        logger.lifecycle("Extracting to ${targetDir.absolutePath}")

        fs.copy { spec ->
            spec.from(archiveOperations.zipTree(tempFile))
            spec.into(targetDir)
        }

        if (platform.os != io.clroot.gradle.bun.platform.Os.WINDOWS) {
            executableFile.setExecutable(true)
        }

        tempFile.delete()
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew functionalTest --tests '*BunSetupFunctionalTest*'`
Expected: ALL PASSED (네트워크 다운로드가 필요하므로 시간이 걸릴 수 있음)

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/io/clroot/gradle/bun/task/BunSetupTask.kt \
       src/functionalTest/kotlin/io/clroot/gradle/bun/BunSetupFunctionalTest.kt
git commit -m "feat: implement BunSetupTask with binary download and extraction"
```

---

### Task 5: BunTask — 범용 bun 명령 실행

**Files:**
- Create: `src/main/kotlin/io/clroot/gradle/bun/task/BunTask.kt`
- Create: `src/functionalTest/kotlin/io/clroot/gradle/bun/BunTaskFunctionalTest.kt`

- [ ] **Step 1: Functional test 작성**

`src/functionalTest/kotlin/io/clroot/gradle/bun/BunTaskFunctionalTest.kt`:
```kotlin
package io.clroot.gradle.bun

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BunTaskFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    @Test
    fun `BunTask runs bun command and prints version`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            import io.clroot.gradle.bun.task.BunTask

            plugins {
                id("io.clroot.gradle-bun")
            }
            bun {
                version = "1.2.0"
            }
            tasks.register<BunTask>("bunVersion") {
                args("--version")
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunVersion")
            .forwardOutput()
            .build()

        assertThat(result.task(":bunVersion")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("1.2.0")
    }

    @Test
    fun `BunTask automatically depends on bunSetup`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            import io.clroot.gradle.bun.task.BunTask

            plugins {
                id("io.clroot.gradle-bun")
            }
            bun {
                version = "1.2.0"
            }
            tasks.register<BunTask>("bunVersion") {
                args("--version")
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunVersion")
            .forwardOutput()
            .build()

        assertThat(result.task(":bunSetup")?.outcome).isIn(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE)
        assertThat(result.task(":bunVersion")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew functionalTest --tests '*BunTaskFunctionalTest*'`
Expected: FAIL — `BunTask` 클래스가 없음

- [ ] **Step 3: BunTask 구현**

`src/main/kotlin/io/clroot/gradle/bun/task/BunTask.kt`:
```kotlin
package io.clroot.gradle.bun.task

abstract class BunTask : AbstractBunExecTask() {
    init {
        description = "Executes a bun command."
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew functionalTest --tests '*BunTaskFunctionalTest*'`
Expected: ALL PASSED

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/io/clroot/gradle/bun/task/BunTask.kt \
       src/functionalTest/kotlin/io/clroot/gradle/bun/BunTaskFunctionalTest.kt
git commit -m "feat: add BunTask for running arbitrary bun commands"
```

---

### Task 6: BunxTask — bunx 명령 실행

**Files:**
- Create: `src/main/kotlin/io/clroot/gradle/bun/task/BunxTask.kt`
- Create: `src/functionalTest/kotlin/io/clroot/gradle/bun/BunxTaskFunctionalTest.kt`

- [ ] **Step 1: Functional test 작성**

`src/functionalTest/kotlin/io/clroot/gradle/bun/BunxTaskFunctionalTest.kt`:
```kotlin
package io.clroot.gradle.bun

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BunxTaskFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    @Test
    fun `BunxTask prepends x to args and runs`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            import io.clroot.gradle.bun.task.BunxTask

            plugins {
                id("io.clroot.gradle-bun")
            }
            bun {
                version = "1.2.0"
            }
            tasks.register<BunxTask>("runCowsay") {
                args("cowsay", "hello")
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("runCowsay")
            .forwardOutput()
            .build()

        assertThat(result.task(":runCowsay")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew functionalTest --tests '*BunxTaskFunctionalTest*'`
Expected: FAIL — `BunxTask` 클래스 없음

- [ ] **Step 3: BunxTask 구현**

`src/main/kotlin/io/clroot/gradle/bun/task/BunxTask.kt`:
```kotlin
package io.clroot.gradle.bun.task

abstract class BunxTask : AbstractBunExecTask() {
    init {
        description = "Executes a bunx command."
    }

    override fun getCommand(): List<String> = listOf("x") + args.get()
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew functionalTest --tests '*BunxTaskFunctionalTest*'`
Expected: ALL PASSED

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/io/clroot/gradle/bun/task/BunxTask.kt \
       src/functionalTest/kotlin/io/clroot/gradle/bun/BunxTaskFunctionalTest.kt
git commit -m "feat: add BunxTask for running bunx commands"
```

---

### Task 7: BunInstallTask — bun install + Functional Test

**Files:**
- Modify: `src/main/kotlin/io/clroot/gradle/bun/task/BunInstallTask.kt` (이미 생성됨, 테스트 추가)
- Create: `src/functionalTest/kotlin/io/clroot/gradle/bun/BunInstallFunctionalTest.kt`

- [ ] **Step 1: Functional test 작성**

`src/functionalTest/kotlin/io/clroot/gradle/bun/BunInstallFunctionalTest.kt`:
```kotlin
package io.clroot.gradle.bun

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BunInstallFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    @Test
    fun `bunInstall runs with frozen lockfile by default`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id("io.clroot.gradle-bun")
            }
            bun {
                version = "1.2.0"
            }
            """.trimIndent()
        )
        projectDir.resolve("package.json").writeText(
            """
            {
                "name": "test-project",
                "version": "0.0.0"
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunInstall")
            .forwardOutput()
            .build()

        assertThat(result.task(":bunSetup")?.outcome).isIn(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE)
        assertThat(result.task(":bunInstall")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `bunInstall without frozen lockfile`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            import io.clroot.gradle.bun.task.BunInstallTask

            plugins {
                id("io.clroot.gradle-bun")
            }
            bun {
                version = "1.2.0"
            }
            tasks.named<BunInstallTask>("bunInstall") {
                frozenLockfile = false
            }
            """.trimIndent()
        )
        projectDir.resolve("package.json").writeText(
            """
            {
                "name": "test-project",
                "version": "0.0.0"
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("bunInstall")
            .forwardOutput()
            .build()

        assertThat(result.task(":bunInstall")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
}
```

- [ ] **Step 2: 테스트 통과 확인**

Run: `./gradlew functionalTest --tests '*BunInstallFunctionalTest*'`
Expected: ALL PASSED (BunInstallTask는 이미 Task 3에서 구현됨)

- [ ] **Step 3: Commit**

```bash
git add src/functionalTest/kotlin/io/clroot/gradle/bun/BunInstallFunctionalTest.kt
git commit -m "test: add functional tests for BunInstallTask"
```

---

### Task 8: Configuration Cache Tests

**Files:**
- Create: `src/functionalTest/kotlin/io/clroot/gradle/bun/ConfigurationCacheFunctionalTest.kt`

- [ ] **Step 1: CC functional test 작성**

`src/functionalTest/kotlin/io/clroot/gradle/bun/ConfigurationCacheFunctionalTest.kt`:
```kotlin
package io.clroot.gradle.bun

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ConfigurationCacheFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    private fun runner(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*args, "--configuration-cache")
        .forwardOutput()

    @Test
    fun `bunSetup is configuration cache compatible`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id("io.clroot.gradle-bun")
            }
            bun {
                version = "1.2.0"
            }
            """.trimIndent()
        )

        // First run: stores configuration cache
        runner("bunSetup").build()

        // Second run: reuses configuration cache
        val result = runner("bunSetup").build()

        assertThat(result.output).contains("Reusing configuration cache")
        assertThat(result.task(":bunSetup")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `BunTask is configuration cache compatible`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            import io.clroot.gradle.bun.task.BunTask

            plugins {
                id("io.clroot.gradle-bun")
            }
            bun {
                version = "1.2.0"
            }
            tasks.register<BunTask>("bunVersion") {
                args("--version")
            }
            """.trimIndent()
        )

        runner("bunVersion").build()

        val result = runner("bunVersion").build()

        assertThat(result.output).contains("Reusing configuration cache")
    }

    @Test
    fun `bunInstall is configuration cache compatible`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id("io.clroot.gradle-bun")
            }
            bun {
                version = "1.2.0"
            }
            """.trimIndent()
        )
        projectDir.resolve("package.json").writeText(
            """
            {
                "name": "test-project",
                "version": "0.0.0"
            }
            """.trimIndent()
        )

        runner("bunInstall").build()

        val result = runner("bunInstall").build()

        assertThat(result.output).contains("Reusing configuration cache")
    }
}
```

- [ ] **Step 2: 테스트 통과 확인**

Run: `./gradlew functionalTest --tests '*ConfigurationCacheFunctionalTest*'`
Expected: ALL PASSED

만약 CC 관련 실패가 발생하면:
- 에러 메시지에서 어떤 필드가 직렬화 불가인지 확인
- `@Internal` 대신 `@Transient`로 변경하거나 Provider로 래핑
- `TaskGraph.whenReady` 대신 `TaskExecutionListener`로 전환하는 등의 조치

- [ ] **Step 3: CC 이슈 수정 (필요 시)**

CC 호환 문제가 발견되면 해당 태스크 클래스를 수정한다. 주요 패턴:
- `project` 참조를 제거하고 `@Inject`된 서비스 사용
- `Property<T>` / `Provider<T>`로 모든 설정값 전달
- `@Internal` 또는 `@Transient`로 직렬화 제외할 필드 표시

- [ ] **Step 4: Commit**

```bash
git add src/functionalTest/kotlin/io/clroot/gradle/bun/ConfigurationCacheFunctionalTest.kt
git commit -m "test: add Configuration Cache compatibility tests"
```

---

### Task 9: CI/CD & Publishing Setup

**Files:**
- Create: `.github/workflows/ci.yml`
- Create: `.github/workflows/publish.yml`

- [ ] **Step 1: CI workflow 작성**

`.github/workflows/ci.yml`:
```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - uses: gradle/actions/setup-gradle@v4

      - name: Run tests
        run: ./gradlew check

      - name: Upload test reports
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports-${{ matrix.os }}
          path: build/reports/tests/
```

- [ ] **Step 2: Publish workflow 작성**

`.github/workflows/publish.yml`:
```yaml
name: Publish

on:
  push:
    tags: ["v*"]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - uses: gradle/actions/setup-gradle@v4

      - name: Run tests
        run: ./gradlew check

      - name: Publish to Gradle Plugin Portal
        run: ./gradlew publishPlugins
        env:
          GRADLE_PUBLISH_KEY: ${{ secrets.GRADLE_PUBLISH_KEY }}
          GRADLE_PUBLISH_SECRET: ${{ secrets.GRADLE_PUBLISH_SECRET }}
```

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/ci.yml .github/workflows/publish.yml
git commit -m "ci: add CI and publish workflows"
```

---

## Post-Implementation Checklist

- [ ] `./gradlew check` 전체 통과
- [ ] `./gradlew functionalTest` 전체 통과
- [ ] Configuration Cache 테스트 통과
- [ ] README.md 작성 (사용 예시, 설정 옵션, 요구사항)
- [ ] GitHub 저장소 생성 (`clroot/gradle-bun-plugin`)
- [ ] Gradle Plugin Portal 계정 설정 및 API key 발급
- [ ] `v0.1.0` 태그 push로 첫 릴리즈
