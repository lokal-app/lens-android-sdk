# Lens

**On-device debug toolkit for Android apps.**

[![CI](https://github.com/lokal-app/lens-android-sdk/actions/workflows/ci.yml/badge.svg)](https://github.com/lokal-app/lens-android-sdk/actions/workflows/ci.yml)
![Min SDK](https://img.shields.io/badge/minSdk-24-green)
![Language](https://img.shields.io/badge/Kotlin-2.0%2B-purple)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

Inspect network traffic, view exceptions, monitor performance, edit SharedPreferences, switch environments, toggle feature flags — all from a floating bubble overlay, with **zero impact on production builds**.

---

## Setup

### 1. Add the GitHub Packages repository

Add to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        // ... your other repositories
        maven {
            url = uri("https://maven.pkg.github.com/lokal-app/lens-android-sdk")
            credentials {
                username = providers.gradleProperty("GITHUB_USERNAME").orNull ?: ""
                password = providers.gradleProperty("GITHUB_TOKEN").orNull ?: ""
            }
            content {
                includeGroup("com.behtar.lens")
            }
        }
    }
}
```

Add credentials to `~/.gradle/gradle.properties` (one-time setup):

```properties
GITHUB_USERNAME=<provided-by-sdk-maintainer>
GITHUB_TOKEN=<provided-by-sdk-maintainer>
```

> **Credentials:** Contact the SDK maintainer (@AnuragJha-AJ) to get the shared read-only token. Do not generate your own — a single shared token is used across the team.

### 2. Add dependencies

#### Application modules (`:app`)

In your `app/build.gradle.kts`, use build-type-specific configurations:

```kotlin
// app/build.gradle.kts
debugImplementation("com.behtar.lens:lens:1.0.0")
releaseImplementation("com.behtar.lens:lens-noop:1.0.0")
```

If you have a `releaseDebug` build type (a release-signed APK with debug tools enabled), include Lens there too:

```kotlin
debugImplementation("com.behtar.lens:lens:1.0.0")
"releaseDebugImplementation"("com.behtar.lens:lens:1.0.0")
releaseImplementation("com.behtar.lens:lens-noop:1.0.0")
```

#### Library modules (`:core`, `:network`, etc.)

If you use Lens APIs (e.g. `Lens.getNetworkInterceptor()`) inside a library module, use a different dependency pattern. **Do not use `implementation(lens-noop)` in library modules** — it puts the noop on all variant runtimes, causing a duplicate-class crash in debug when the full `lens` artifact is also present.

Use `compileOnly` for the noop so it only provides the API surface at compile time:

```kotlin
// core/build.gradle.kts or any library module
compileOnly("com.behtar.lens:lens-noop:1.0.0")   // compile-time API surface only
debugImplementation("com.behtar.lens:lens:1.0.0")
"releaseDebugImplementation"("com.behtar.lens:lens:1.0.0")
releaseImplementation("com.behtar.lens:lens-noop:1.0.0")
```

> **Why `compileOnly`?** In library modules without product flavours, `implementation` adds the dependency to every variant's runtime classpath. When both `lens-noop` (from `implementation`) and `lens` (from `debugImplementation`) land on the debug runtime classpath simultaneously, the AGP `checkDuplicateClasses` task fails. `compileOnly` contributes only to the compile classpath — the runtime artifact is supplied by whichever consuming module (`:app`) resolves the correct variant.

### 3. Initialize

Call `Lens.install()` in your `Application.onCreate()`. It takes an `Application` instance — not a `Context`:

```kotlin
import com.behtar.lens.api.ActivationGesture
import com.behtar.lens.api.Lens

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Lens.install(this) {
            activationGesture = ActivationGesture.FIVE_TAP
            showNotification = true
        }
    }
}
```

### 4. Add the network interceptor

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(Lens.getNetworkInterceptor())
    .build()
```

**That's it.** Tap 5 times anywhere to open the debug dashboard.

---

## Artifacts

| Artifact | Purpose | Use with |
|----------|---------|----------|
| `com.behtar.lens:lens` | Full SDK — all plugins, interceptors, and UI | `debugImplementation` |
| `com.behtar.lens:lens-noop` | No-op stubs — identical API surface, zero behavior | `releaseImplementation` / `compileOnly` in library modules |
| `com.behtar.lens:lens-api` | Pure-Kotlin interfaces — no Android dependency | Transitive (pulled automatically) |

---

## Built-in Plugins

| Plugin | Description |
|--------|-------------|
| **Network Inspector** | HTTP request/response viewer with cURL export, HAR export, header redaction |
| **Global Search** | Cross-plugin search across all log types with 300ms debounce |
| **App Info** | Build info, device details, session metadata |
| **Performance Monitor** | Real-time FPS (Choreographer), memory usage, jank detection with sparkline graphs |
| **Analytics Inspector** | Intercepted analytics events and user properties |
| **Exception Tracker** | Uncaught + handled exceptions with stack traces, ANR detection (5s watchdog) |
| **Database Inspector** | Browse and query SQLite databases |
| **SharedPreferences Editor** | View and edit all SharedPreferences files |
| **Deep Link Tester** | Fire deep links without leaving the app |
| **Log Viewer** | Timber log viewer with level filtering |
| **Cache Manager** | View and clear app caches |

### Provider-based Plugins

These appear only when you supply a provider implementation:

| Plugin | Provider Interface |
|--------|--------------------|
| Environment Switcher | `EnvironmentProvider` |
| Feature Flags Editor | `FeatureFlagProvider` |
| Quick Actions | `QuickActionsProvider` |

---

## Configuration

All configuration is done through the DSL passed to `Lens.install()`. Provider-based plugins are
wired via **functions** on the builder — not property assignment:

```kotlin
import com.behtar.lens.api.ActivationGesture
import com.behtar.lens.api.HeaderRedactor
import com.behtar.lens.api.Lens

Lens.install(this) {
    // Activation gesture: THREE_TAP, FIVE_TAP (default), LONG_PRESS, or NONE
    activationGesture = ActivationGesture.FIVE_TAP

    // Shake to open (independent of activationGesture)
    shakeToOpenEnabled = true

    // Sticky notification with live request/error counts
    showNotification = true

    // Remote kill switch — disable Lens without shipping an app update.
    // The lambda receives a callback; invoke it with true to enable, false to disable.
    remoteActivation { callback ->
        val enabled = FirebaseRemoteConfig.getInstance().getBoolean("devtools_enabled")
        callback(enabled)
    }

    // Redact sensitive headers in network logs.
    // Return true for any header name whose value should be replaced with "[REDACTED]".
    headerRedactor(HeaderRedactor { name ->
        name.equals("Authorization", ignoreCase = true)
    })

    // Provider-based plugins — wire via functions, not property assignment
    environments(MyEnvironmentProvider())
    featureFlags(MyFeatureFlagProvider())
    quickActions(MyQuickActionsProvider())
}
```

> **Builder API note:** `remoteActivation`, `headerRedactor`, `environments`, `featureFlags`, and
> `quickActions` are **functions** on the builder, not writable properties. Assigning them as
> properties (e.g. `headerRedactor = ...`) will not compile.

### Activation Methods

| Method | How |
|--------|-----|
| 3-tap | Tap anywhere 3 times quickly (`ActivationGesture.THREE_TAP`) |
| 5-tap | Tap anywhere 5 times quickly (`ActivationGesture.FIVE_TAP`) — default |
| Long press | Long-press anywhere (`ActivationGesture.LONG_PRESS`) |
| Shake | Set `shakeToOpenEnabled = true` in the DSL |
| Programmatic | `Lens.open()` |
| Notification | Tap the sticky notification (requires `showNotification = true`) |
| Floating bubble | Always visible — injected into every Activity's DecorView (no permissions needed) |

---

## WebView & WebSocket

```kotlin
// Capture WebView navigations
webView.webViewClient = Lens.wrapWebViewClient(myWebViewClient)

// Capture WebSocket frames
val listener = Lens.wrapWebSocketListener(myListener)
```

---

## Custom Plugins

### Compose Plugin

```kotlin
import com.behtar.lens.api.ComposableLensPlugin
import com.behtar.lens.api.Lens

class MyDebugPlugin : ComposableLensPlugin {
    override val id = "my_debug"
    override val name = "My Debug Tool"
    override val icon = R.drawable.ic_my_debug
    override val description = "Custom debugging tool"
    override val priority = 40

    @Composable
    override fun Content() {
        Text("Hello from my plugin!")
    }
}

// Register after Lens.install()
Lens.registerPlugin(MyDebugPlugin())
```

### View Plugin (Experimental)

For non-Compose consumers (React Native native modules, Java apps):

```kotlin
import com.behtar.lens.api.LensExperimental
import com.behtar.lens.api.ViewLensPlugin

@OptIn(LensExperimental::class)
class LegacyPlugin : ViewLensPlugin {
    override val id = "legacy"
    override val name = "Legacy Tool"
    override val icon = R.drawable.ic_legacy
    override val description = "View-based debug tool"

    override fun createView(context: Context): View {
        return TextView(context).apply { text = "Hello from Views" }
    }
}
```

---

## Key-Value Settings Store

Runtime configuration store usable by custom plugins:

```kotlin
Lens.putString("my_key", "my_value")
Lens.putBoolean("feature_enabled", true)

val value = Lens.getString("my_key", default = "fallback")
val enabled = Lens.getBoolean("feature_enabled", default = false)
```

Backed by SharedPreferences in debug builds, no-op in release.

---

## Data Export

Share button on the dashboard toolbar exports:
- **HAR 1.2** — importable into Chrome DevTools, Charles Proxy
- **JSON** — all logs (network, exceptions, analytics) as a single file

---

## Architecture

```
lens-api          Pure Kotlin module — interfaces, data classes, annotations
lens              Android library — full implementation, all plugins, Compose UI
lens-noop         Android library — no-op stubs matching the public API surface
```

**No Hilt, no Dagger, no reflection.** Lens uses a lightweight internal service locator with zero impact on your app's DI graph.

---

## ProGuard / R8

Consumer rules are bundled — no manual configuration needed.

---

## Comparison

| Feature | Lens | Chucker | Flipper | Hyperion |
|---------|------|---------|---------|----------|
| Network inspector | Yes | Yes | Yes | Yes |
| SharedPreferences editor | Yes | No | Yes | Yes |
| Exception viewer + ANR | Yes | No | No | Yes* |
| FPS / Memory monitoring | Yes | No | No | No |
| Environment switcher | Yes | No | No | No |
| Feature flag editor | Yes | No | No | No |
| Analytics inspector | Yes | No | No | No |
| Database inspector | Yes | No | Yes | No |
| Global search | Yes | No | No | No |
| Custom plugin API | Yes | No | Yes | Yes |
| HAR export | Yes | No | No | No |
| No-op release variant | Yes | Yes | N/A | Yes |
| No external tools needed | Yes | Yes | No (ADB) | Yes |
| No DI framework required | Yes | Yes | Yes | No |

---

## Requirements

| | Version |
|---|---------|
| Min SDK | 24 (Android 7.0) |
| Compile SDK | 36 |
| Kotlin | 2.0+ |
| Jetpack Compose | BOM-managed |

---

## Local Development

To iterate on the SDK locally without publishing:

```bash
cd lens-android-sdk
./gradlew publishToMavenLocal
```

Then temporarily add `mavenLocal()` **above** the GitHub Packages repository in your app's `settings.gradle.kts`. Maven Local takes priority, so your local build will be used.

```kotlin
// settings.gradle.kts — local development only, do not commit
repositories {
    mavenLocal()  // must be first
    maven {
        url = uri("https://maven.pkg.github.com/lokal-app/lens-android-sdk")
        // ...
    }
}
```

---

## License

```
Copyright 2024 Behtar Technologies

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```