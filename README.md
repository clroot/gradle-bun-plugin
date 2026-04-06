# gradle-bun-plugin

Gradle plugin that auto-downloads and manages [Bun](https://bun.sh) — no global installation required.

## Features

- Auto-downloads bun binary per project (no system-wide install needed)
- Version pinning for reproducible builds
- Configuration Cache compatible (Gradle 8.0+)
- Incremental build support
- `BunTask`, `BunxTask`, `BunInstallTask` for running any bun command
- macOS, Linux, Windows (x64, arm64)

## Installation

```kotlin
// build.gradle.kts
plugins {
    id("io.clroot.gradle-bun") version "0.1.0"
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
    id("io.clroot.gradle-bun") version "0.1.0"
}

bun {
    version = "1.2.0"
    workingDir = layout.projectDirectory.dir("frontend")
}

tasks.register<BunTask>("buildFrontend") {
    dependsOn("bunInstall")
    args("run", "build")
    inputs.dir("frontend/src")
    outputs.dir("src/main/resources/static")
}

tasks.named("processResources") {
    dependsOn("buildFrontend")
}
```

## Requirements

- Gradle 8.0+
- Java 17+

## License

MIT
