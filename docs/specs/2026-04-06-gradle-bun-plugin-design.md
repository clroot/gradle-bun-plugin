# gradle-bun-plugin 설계

> Gradle 빌드 환경에서 bun을 자동 다운로드/관리하는 재사용 가능한 Gradle 플러그인

## 개요

- **플러그인 ID**: `io.clroot.gradle-bun`
- **작성 언어**: Kotlin
- **최소 Gradle**: 8.0+
- **배포**: Gradle Plugin Portal (오픈소스)
- **프로젝트 경로**: `/Users/clroot/Documents/projects/io.clroot/gradle-bun-plugin`

## 배경

bun이 설치되지 않은 빌드 환경(CI, 새 개발자 머신)에서 프론트엔드 빌드가 실패하는 문제를 해결한다. 기존 `m-segreti/gradle-bun-plugin`은 Configuration Cache 미지원, `bunx` 미지원, 단일 메인테이너 방치 상태로 프로덕션에 부적합하다.

`node-gradle/gradle-node-plugin`의 검증된 설계 패턴(Provider API, Gradle TestKit, ValueSource)을 참고하되, bun에 특화된 경량 플러그인으로 설계한다.

## Extension DSL

```kotlin
plugins {
    id("io.clroot.gradle-bun") version "0.1.0"
}

bun {
    // bun 버전 (필수 — 기본값 없음, 미설정 시 빌드 실패)
    version = "1.2.0"

    // package.json이 있는 디렉토리 (기본: 프로젝트 루트)
    workingDir = layout.projectDirectory.dir("frontend")

    // bun 바이너리 설치 경로 (기본: .gradle/bun)
    installDir = layout.projectDirectory.dir(".gradle/bun")

    // 시스템에 설치된 bun 사용 여부 (기본: false → 항상 다운로드)
    useSystemBun = false

    // 다운로드 URL 베이스 (사내 미러 지원)
    downloadBaseUrl = "https://github.com/oven-sh/bun/releases/download"
}
```

**설계 결정:**

- `version`에 기본값 없음 — `"latest"` 기본값은 재현성을 해침. 미설정 시 빌드 실패로 명시적 설정을 강제한다.
- `useSystemBun = true`면 다운로드를 스킵하고 PATH에서 bun을 찾는다.
- `installDir`은 `.gradle/bun`으로 — `.gitignore`에 `.gradle/`이 이미 포함되어 있고, Gradle 캐시 디렉토리와 일관성을 유지한다.
- 모든 프로퍼티는 `Property<T>` / `DirectoryProperty`로 선언하여 Configuration Cache를 지원한다.

## 태스크 구조

### 자동 등록 태스크

| 태스크 | 동작 | 의존 관계 |
|--------|------|-----------|
| `bunSetup` | bun 바이너리 다운로드 & 설치 | - |
| `bunInstall` | `bun install --frozen-lockfile` | `bunSetup` |

### 사용자 정의 태스크 타입

```kotlin
// BunTask — 임의의 bun 명령 실행
tasks.register<BunTask>("bunBuild") {
    args("run", "build")
    inputs.dir("frontend/src")
    outputs.dir("frontend/dist")
}

// BunxTask — bunx로 패키지 실행
tasks.register<BunxTask>("generateRoutes") {
    args("@tanstack/router-cli", "generate")
}

// BunInstallTask — 커스텀 install 설정
tasks.register<BunInstallTask>("bunInstallDev") {
    frozenLockfile = false
}
```

모든 태스크는 `bunSetup`에 자동 의존하므로, bun이 미설치된 환경에서도 태스크를 실행하면 자동으로 설치된다.

### 실행 흐름

```
bunSetup (다운로드) → bunInstall (의존성 설치) → BunTask (사용자 태스크)
```

**설계 결정:**

- `bunTest` 같은 전용 태스크는 만들지 않음 — `BunTask`로 `args("test")` 하면 되고, 불필요한 태스크 타입을 줄인다.
- `BunxTask`를 별도 타입으로 분리 — `bunx`는 `bun`과 실행 바이너리가 같지만 의미가 다르고, 기존 플러그인에서 빠진 부분이다.
- 모든 태스크가 `bunSetup`에 자동 의존 — 사용자가 의존 관계를 신경 쓸 필요 없다.

## 바이너리 다운로드 & 플랫폼 감지

### 플랫폼 매핑

| OS | Arch | bun 릴리즈 식별자 |
|----|------|-------------------|
| macOS | x64 | `bun-darwin-x64` |
| macOS | arm64 | `bun-darwin-aarch64` |
| Linux | x64 | `bun-linux-x64` |
| Linux | arm64 | `bun-linux-aarch64` |
| Windows | x64 | `bun-windows-x64` |
| Windows | arm64 | `bun-windows-aarch64` |

JVM의 `os.name` / `os.arch`로 감지하되, ARM 계열은 `uname -m` fallback으로 보완한다 (JVM이 ARM variant를 정확히 구분하지 못하는 경우 대응).

### 다운로드 URL 패턴

```
{downloadBaseUrl}/bun-v{version}/bun-{os}-{arch}.zip
```

- 모든 플랫폼이 ZIP 포맷 (bun은 tar.gz를 제공하지 않음)
- `downloadBaseUrl` 설정으로 사내 미러 교체 가능

### 설치 흐름

1. 플랫폼 감지
2. `installDir/{version}/{platform}/` 이미 존재하면 스킵 (up-to-date)
3. ZIP 다운로드 → `installDir/{version}/{platform}/`에 압축 해제
4. Unix: `chmod +x` 설정
5. 실행 경로를 하위 태스크들에 Provider로 전파

### 캐싱 전략

- **Gradle inputs/outputs**: `version` + 플랫폼을 input, 설치 디렉토리를 output → 버전 불변이면 자동 스킵
- **CI 캐시**: `.gradle/bun/` 디렉토리를 Gradle build cache와 함께 캐시하면 재다운로드 방지

**설계 결정:**

- SHA-256 체크섬 검증은 v0.1.0에서 생략 — bun 공식 릴리즈가 체크섬 파일을 별도 제공하지 않아서, GitHub Releases의 HTTPS 전송 무결성에 의존한다. 추후 GitHub API로 릴리즈 asset 해시를 가져오는 방식을 고려한다.
- 버전별 디렉토리 분리 (`installDir/{version}/`) — 여러 프로젝트에서 다른 버전을 써도 충돌 없다.

## Configuration Cache & Incremental Build

### Configuration Cache 호환

핵심 원칙: configuration phase에서 외부 상태(파일시스템, 환경변수, 프로세스)를 읽지 않는다.

```kotlin
// Bad — configuration phase에서 시스템 호출
val bunPath = Runtime.getRuntime().exec("which bun")  // ❌ 캐시 불가

// Good — Provider API로 지연 평가
val bunPath: Provider<String> = providers.provider {
    resolveBunExecutable()  // execution phase에서만 실행
}
```

**적용 방식:**

- Extension 프로퍼티는 모두 `Property<T>` / `DirectoryProperty` 사용
- 플랫폼 감지는 `ValueSource`로 래핑 — configuration cache가 입력값을 추적 가능
- 태스크 간 경로 전파는 `Provider`로 연결

### Incremental Build

| 태스크 | Inputs | Outputs |
|--------|--------|---------|
| `bunSetup` | `version`, 플랫폼 | `installDir/{version}/{platform}/` |
| `bunInstall` | `package.json`, `bun.lock` | `node_modules/` |
| `BunTask` | 사용자 정의 | 사용자 정의 |

**설계 결정:**

- `bunInstall`의 inputs에 `bun.lock` 포함 — lockfile 변경 시에만 재실행
- `BunTask`는 inputs/outputs를 강제하지 않음 — 사용자가 자유롭게 설정, 미설정 시 매번 실행 (Gradle 기본 동작)

## Lockfile 무결성 검증

`bunInstall` 태스크의 `frozenLockfile` 프로퍼티로 제어한다.

```kotlin
bunInstall {
    frozenLockfile = true   // 기본값: true
}
```

- `frozenLockfile = true` → `bun install --frozen-lockfile` 실행
- `frozenLockfile = false` → `bun install` 실행 (lockfile 갱신 허용)

별도의 검증 태스크(`bunLockfileCheck`)는 만들지 않는다 — `--frozen-lockfile`이 bun 자체적으로 제공하는 기능이고, 이를 감싸는 것만으로 충분하다.

## Workspace 지원

bun workspace(monorepo) 환경에서는 bun 자체의 `--filter` 플래그에 위임한다.

```kotlin
bun {
    version = "1.2.0"
    workingDir = layout.projectDirectory  // monorepo 루트
}

tasks.register<BunTask>("buildApp") {
    args("run", "--filter", "app", "build")
}
```

**설계 결정:**

- 플러그인이 workspace를 특별 취급하지 않음 — bun의 `--filter` 플래그에 위임
- `workingDir`을 monorepo 루트로 설정하면 workspace가 자연스럽게 동작
- Gradle multi-module과 bun workspace는 별개 개념이므로 억지로 매핑하지 않음

## 프로젝트 구조

```
gradle-bun-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── src/
│   ├── main/kotlin/io/clroot/gradle/bun/
│   │   ├── BunPlugin.kt              # 플러그인 진입점 (apply)
│   │   ├── BunExtension.kt           # DSL 정의
│   │   ├── task/
│   │   │   ├── BunSetupTask.kt       # 다운로드 & 설치
│   │   │   ├── BunInstallTask.kt     # bun install
│   │   │   ├── BunTask.kt            # 범용 bun 명령
│   │   │   └── BunxTask.kt           # bunx 명령
│   │   └── platform/
│   │       ├── Platform.kt           # OS/Arch 데이터 클래스
│   │       └── PlatformResolver.kt   # 플랫폼 감지 (ValueSource)
│   └── functionalTest/kotlin/io/clroot/gradle/bun/
│       ├── BunSetupTest.kt
│       ├── BunInstallTest.kt
│       ├── BunTaskTest.kt
│       ├── BunxTaskTest.kt
│       ├── ConfigurationCacheTest.kt
│       └── fixtures/
│           └── simple-project/
│               ├── build.gradle.kts
│               ├── package.json
│               └── bun.lock
└── .github/workflows/
    └── ci.yml
```

## 테스트 전략

| 레이어 | 도구 | 범위 |
|--------|------|------|
| **Unit** | JUnit 5 | PlatformResolver, Extension 기본값 |
| **Functional** | Gradle TestKit | 실제 Gradle 빌드로 태스크 실행, CC 검증 |

Functional Test가 핵심이다. Gradle 플러그인은 Gradle 런타임과 결합이 강해서, TestKit으로 실제 빌드를 돌리는 것이 가장 신뢰성 높다.

**설계 결정:**

- `src/functionalTest/` 분리 — Gradle 플러그인 개발 관례, unit test와 classpath를 분리한다.
- fixture 프로젝트는 최소한으로 — `package.json` + `build.gradle.kts`만 있는 심플 프로젝트
- Configuration Cache 테스트 전용 클래스 — `--configuration-cache` 플래그로 2회 실행 후 캐시 히트를 검증한다.

## 지원 플랫폼 요약

| 플랫폼 | v0.1.0 | 비고 |
|--------|--------|------|
| macOS x64 | O | |
| macOS arm64 | O | |
| Linux x64 | O | |
| Linux arm64 | O | |
| Windows x64 | O | |
| Windows arm64 | O | |

## v0.1.0 이후 로드맵

- SHA-256 체크섬 검증 (GitHub API 기반)
- Gradle build cache 연동 (remote cache)
- `.bun-version` 파일 자동 감지
- Gradle task rule (`bun_<script>` 동적 태스크 생성)
