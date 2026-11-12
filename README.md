# gradle-bun-plugin

Gradle plugin that auto-downloads and manages [Bun](https://bun.sh) — no global installation required.

## Features

- Auto-downloads bun binary per project (no system-wide install needed)
- Version pinning for reproducible builds
- Configuration Cache compatible (Gradle 8.0+)
- Incremental build support
- `BunTask`, `BunxTask`, `BunInstallTask` for running any bun command
- `io.clroot.gradle-bun-frontend`: turn a frontend directory into a Gradle module
- macOS, Linux, Windows (x64, arm64)

## Installation

```kotlin
// build.gradle.kts
plugins {
    id("io.clroot.gradle-bun") version "0.2.0"
}

bun {
    version = "1.2.0"
}
```

## Configuration

```kotlin
bun {
    // Required — no default, build fails if not set
    version = "1.2.0"

    // Directory containing package.json (default: project root)
    workingDir = layout.projectDirectory.dir("frontend")

    // Where to install bun binary (default: .gradle/bun)
    installDir = layout.projectDirectory.dir(".gradle/bun")

    // Use system-installed bun instead of downloading (default: false)
    useSystemBun = false

    // Custom download mirror (default: GitHub Releases)
    downloadBaseUrl = "https://github.com/oven-sh/bun/releases/download"
}
```

## Tasks

### Auto-registered

| Task | Description |
|------|-------------|
| `bunSetup` | Downloads and installs the bun binary |
| `bunInstall` | Runs `bun install --frozen-lockfile` |

### Custom tasks

```kotlin
import io.clroot.gradle.bun.task.BunTask
import io.clroot.gradle.bun.task.BunxTask
import io.clroot.gradle.bun.task.BunInstallTask

// Run any bun command
tasks.register<BunTask>("bunBuild") {
    args("run", "build")
    inputs.dir("frontend/src")
    outputs.dir("frontend/dist")
}

// Run bunx
tasks.register<BunxTask>("generateRoutes") {
    args("@tanstack/router-cli", "generate")
}

// Custom install (e.g., without frozen lockfile)
tasks.register<BunInstallTask>("bunInstallDev") {
    frozenLockfile = false
}
```

All custom tasks automatically depend on `bunSetup` — bun is downloaded on first run.

## Example: Spring Boot + Frontend

```kotlin
plugins {
    id("org.springframework.boot") version "3.4.0"
    id("io.clroot.gradle-bun") version "0.2.0"
}

bun {
    version = "1.2.0"
    workingDir = layout.projectDirectory.dir("frontend")
}

val buildFrontend by tasks.registering(BunTask::class) {
    dependsOn("bunInstall")
    args("run", "build")
    inputs.dir("frontend/src")
    outputs.dir("frontend/dist")
}

tasks.processResources {
    from(buildFrontend) { into("static") }
}
```

For a cleaner setup with the frontend as its own Gradle module, see [Frontend module](#frontend-module-multi-project).

## Frontend module (multi-project)

Apply `io.clroot.gradle-bun-frontend` to make a frontend directory a first-class Gradle subproject.
It applies `io.clroot.gradle-bun` and `base`, then:

- registers `bunBuild` (`bun run build`, incremental: sources in, `dist` out) and wires it into `assemble`
- makes `clean` delete the output directory
- exposes the output through a consumable `bunDist` configuration
- optionally registers `bun run <script>` tasks wired into `check`

```kotlin
// settings.gradle.kts
include(":frontend")
```

```kotlin
// frontend/build.gradle.kts
plugins {
    id("io.clroot.gradle-bun-frontend") version "0.2.0"
}

bun {
    version = "1.2.0"
}

bunFrontend {
    buildScript = "build"                          // default: "build"
    outputDir = layout.projectDirectory.dir("dist") // default: <bun.workingDir>/dist
    inputExcludes.add("coverage/**")               // node_modules, .gradle, build, outputDir are always excluded
    checkScripts("lint", "test")                   // registers bunLint, bunTest -> check
}
```

Consume the build output from another project, e.g. a Spring Boot app at the root:

```kotlin
// build.gradle.kts
val frontendDist by configurations.creating {
    isCanBeConsumed = false
}

dependencies {
    frontendDist(project(path = ":frontend", configuration = "bunDist"))
}

tasks.processResources {
    from(frontendDist) { into("static") }
}
```

`processResources` now depends on `:frontend:bunBuild` automatically. No `dependsOn` wiring is needed.

| Task | Description |
|------|-------------|
| `bunBuild` | Runs `bun run <buildScript>`; part of `assemble` |
| `bun<Script>` | Created by `checkScripts(...)` (`format:check` → `bunFormatCheck`); part of `check` |

## Requirements

- Gradle 8.0+
- Java 17+

## License

MIT
