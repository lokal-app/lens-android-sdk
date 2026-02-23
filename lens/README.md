# Lens Android SDK

On-device debug toolkit for Android apps. Inspect network traffic, view exceptions, monitor performance, edit SharedPreferences, switch environments, toggle feature flags — all from a floating bubble overlay, with zero code changes in production.

## Quick Start

**1. Add dependencies**

```kotlin
// build.gradle.kts (app module)
debugImplementation("com.behtar.lens:lens:1.0.0")
releaseImplementation("com.behtar.lens:lens-noop:1.0.0")
```

**2. Install in Application.onCreate()**

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Lens.install(this) {
            activationGesture(ActivationGesture.FIVE_TAP)
        }
    }
}
```

**3. Add the network interceptor**

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(Lens.getNetworkInterceptor())
    .build()
```

That's it. Tap 5 times anywhere in your app to open the debug dashboard.

## Modules

| Artifact | Purpose | Size |
|----------|---------|------|
| `lens` | Full SDK with all plugins and UI | ~1.2 MB |
| `lens-noop` | No-op stubs for release builds | ~11 KB |
| `lens-api` | Pure-Kotlin interfaces (zero Android deps) | ~21 KB |

Use `debugImplementation` for `lens` and `releaseImplementation` for `lens-noop`. The no-op variant has identical method signatures that return defaults and do nothing — zero overhead in production.

## Built-in Plugins

| Plugin | Priority | Description |
|--------|----------|-------------|
| Network Inspector | 100 | HTTP request/response viewer with cURL export, HAR export, header redaction |
| Global Search | 99 | Cross-plugin search across all log types |
| App Info | 95 | Build info, device details, session metadata |
| Performance | 85 | Real-time FPS, memory usage, jank frame detection with sparkline graphs |
| Analytics Inspector | 80 | Intercepted analytics events and user properties |
| Exceptions | 78 | Uncaught + handled exception log with stack traces, ANR detection |
| Database Inspector | 75 | Browse SQLite databases and tables |
| SharedPreferences | 70 | View and edit all SharedPreferences files |
| Deep Link Tester | 60 | Test deep links without leaving the app |
| Log Viewer | 55 | Timber log viewer with level filtering |
| Cache Manager | 50 | View and clear app caches |

### Optional Plugins (Provider-based)

These plugins appear only when you supply a provider:

| Plugin | Provider Interface | Description |
|--------|--------------------|-------------|
| Environment Switcher | `EnvironmentProvider` | Switch between server environments (e.g., prod/staging) with app restart |
| Feature Flags | `FeatureFlagProvider` | View and toggle feature flags at runtime |
| Quick Actions | `QuickActionsProvider` | Custom one-tap debugging shortcuts |

## Configuration

```kotlin
Lens.install(this) {
    // Activation
    activationGesture(ActivationGesture.FIVE_TAP)  // FIVE_TAP, SHAKE, or NONE

    // Notification (sticky notification with request/error counts)
    showNotification(true)  // default: true

    // Remote kill switch (disable Lens without an app update)
    remoteActivation(MyRemoteActivationProvider())

    // Security: redact sensitive headers in network logs
    headerRedactor(HeaderRedactor { name ->
        name.equals("Authorization", ignoreCase = true) ||
        name.equals("X-Custom-Secret", ignoreCase = true)
    })

    // Providers for optional plugins
    environmentProvider(MyEnvironmentProvider())
    featureFlagProvider(MyFeatureFlagProvider())
    quickActionsProvider(MyQuickActionsProvider())
}
```

## Custom Plugins

### Compose Plugin

```kotlin
class MyDebugPlugin : ComposableLensPlugin {
    override val id = "my_debug"
    override val name = "My Debug Tool"
    override val icon = R.drawable.ic_my_debug
    override val description = "Custom debugging tool"
    override val priority = 40  // 0-49 for custom plugins

    override fun onInitialize(context: Context) {
        // Called once when Lens initializes
    }

    @Composable
    override fun Content() {
        // Your Compose UI here
        Text("Hello from my plugin!")
    }
}

// Register after Lens.install()
Lens.registerPlugin(MyDebugPlugin())
```

### View Plugin (Experimental)

For non-Compose consumers (React Native, Java, legacy View-based apps):

```kotlin
@OptIn(LensExperimental::class)
class LegacyPlugin : ViewLensPlugin {
    override val id = "legacy"
    override val name = "Legacy Tool"
    override val icon = R.drawable.ic_legacy
    override val description = "View-based debug tool"

    override fun createView(context: Context): View {
        return LinearLayout(context).apply {
            addView(TextView(context).apply { text = "Hello from Views" })
        }
    }
}
```

## Provider Implementation Guide

### EnvironmentProvider

```kotlin
class MyEnvironmentProvider : EnvironmentProvider {
    private val envs = listOf(
        Environment("Production", "https://api.example.com"),
        Environment("Staging", "https://staging.example.com"),
    )
    private var current = envs[0]

    override fun getEnvironments() = envs
    override fun getCurrentEnvironment() = current
    override fun setEnvironment(environment: Environment) { current = environment }
    override fun onRestartRequested() {
        // Restart the app to apply new environment
        ProcessPhoenix.triggerRebirth(appContext)
    }

    // Optional: WebView environment presets
    override fun getWebViewPresets(): List<Environment> {
        return listOf(
            Environment("WebView Prod", "https://web.example.com"),
            Environment("WebView Staging", "https://web-staging.example.com"),
        )
    }
}
```

### FeatureFlagProvider

```kotlin
class MyFeatureFlagProvider : FeatureFlagProvider {
    override fun getFlags(): List<FeatureFlag> = listOf(
        FeatureFlag("dark_mode", "Dark Mode", FlagType.BOOLEAN, true),
        FeatureFlag("api_timeout", "API Timeout (ms)", FlagType.NUMBER, 5000),
        FeatureFlag("welcome_msg", "Welcome Message", FlagType.STRING, "Hello!"),
    )

    override fun setFlag(id: String, value: Any) {
        // Persist the flag value
    }
}
```

### RemoteActivationProvider

```kotlin
class FirebaseRemoteActivation(private val key: String) : RemoteActivationProvider {
    override fun isEnabled(callback: (Boolean) -> Unit) {
        FirebaseRemoteConfig.getInstance().fetchAndActivate()
            .addOnCompleteListener {
                callback(FirebaseRemoteConfig.getInstance().getBoolean(key))
            }
    }
}
```

## Key-Value Settings Store

Lens provides a generic key-value store for runtime configuration, usable by custom plugins:

```kotlin
// Write
Lens.putString("my_key", "my_value")
Lens.putBoolean("feature_enabled", true)

// Read
val value = Lens.getString("my_key", default = "fallback")
val enabled = Lens.getBoolean("feature_enabled", default = false)
```

Backed by SharedPreferences in debug builds, no-op in release.

## WebView & WebSocket Support

```kotlin
// Wrap your WebViewClient to capture WebView navigations
webView.webViewClient = Lens.wrapWebViewClient(myWebViewClient)

// Wrap your WebSocketListener to capture WebSocket frames
val listener = Lens.wrapWebSocketListener(myListener)
```

## Data Export

The dashboard toolbar includes a Share button that exports:
- **HAR 1.2** — Network logs in HTTP Archive format (importable into Chrome DevTools)
- **JSON** — All logs (network, exceptions, analytics) as a single JSON file

## Features

### ANR Detection
A background watchdog thread detects main thread blocks exceeding 5 seconds. When triggered, it captures the main thread's full stack trace and logs it as an exception entry — visible in the Exceptions plugin before the system ANR dialog appears.

### Performance Monitoring
Real-time metrics via Choreographer frame callbacks and Runtime memory polling:
- Current FPS with 60-second sparkline history
- Jank frame counter (frames > 16.67ms)
- JVM heap usage (used/total/max) with history graph
- Native heap tracking via `Debug.getNativeHeapAllocatedSize()`

### Sticky Notification
When enabled, shows a persistent notification with live request count and error count. Tap to open the dashboard; "Clear" action resets all logs.

### Header Redaction
Network logs automatically redact sensitive headers (`Authorization`, `Cookie`, `Set-Cookie`, `X-Api-Key`, `Proxy-Authorization`). Customize via `HeaderRedactor` in config.

## Architecture

```
lens-api (Pure Kotlin, JVM)
  ├── LensConfig, LensPlugin, ComposableLensPlugin, ViewLensPlugin
  ├── EnvironmentProvider, FeatureFlagProvider, QuickActionsProvider
  ├── HeaderRedactor, RemoteActivationProvider
  └── AnalyticsEventListener

lens (Android Library)
  ├── api/       — Lens singleton, LensApi interface
  ├── internal/
  │   ├── core/          — LensImpl, PluginRegistry
  │   ├── di/            — LensServiceLocator (no Hilt)
  │   ├── data/          — Repositories + models (network, exceptions, analytics, websocket)
  │   ├── interceptors/  — OkHttp, WebView, WebSocket, Exception, Timber, Analytics
  │   ├── plugins/       — 11 built-in plugins + performance package
  │   ├── presentation/  — Dashboard, bubble, search screens
  │   ├── notification/  — Sticky notification manager
  │   └── export/        — HAR + JSON export

lens-noop (Android Library)
  └── api/       — No-op stubs mirroring lens public API
```

**No Hilt, no Dagger, no reflection** — Lens uses a lightweight internal service locator. It has zero impact on your app's DI graph.

## Activation Methods

| Method | Config | Description |
|--------|--------|-------------|
| 5-tap | `ActivationGesture.FIVE_TAP` | Tap anywhere 5 times quickly |
| Shake | `ActivationGesture.SHAKE` | Shake the device |
| Programmatic | `Lens.open()` | Open from code (e.g., a hidden settings button) |
| Notification | `showNotification(true)` | Tap the sticky notification |
| Floating bubble | Always on | Injected into every Activity's DecorView — no permissions needed |

## ProGuard / R8

Consumer ProGuard rules are bundled — no manual configuration needed. The rules keep:
- All public API classes and interfaces
- Plugin implementations (`ComposableLensPlugin`, `ViewLensPlugin`)
- Provider implementations
- Internal notification receiver

## Comparison

| Feature | Lens | Chucker | Flipper | Hyperion |
|---------|------|---------|---------|----------|
| Network inspector | Yes | Yes | Yes | Yes |
| SharedPreferences editor | Yes | No | Yes | Yes |
| Exception viewer | Yes | No | No | Yes |
| ANR detection | Yes | No | No | No |
| FPS/Memory monitoring | Yes | No | No | No |
| Environment switcher | Yes | No | No | No |
| Feature flag editor | Yes | No | No | No |
| Analytics inspector | Yes | No | No | No |
| Global search | Yes | No | No | No |
| Custom plugins | Yes | No | Yes | Yes |
| No-op release variant | Yes | Yes | N/A | Yes |
| No permissions needed | Yes | Yes | No (ADB) | Yes |
| HAR export | Yes | No | No | No |
| No Hilt/Dagger required | Yes | Yes | Yes | No |
| View-based plugin API | Yes | N/A | N/A | Yes |

## Requirements

- **Min SDK**: 24 (Android 7.0)
- **Compile SDK**: 36
- **Kotlin**: 2.0+
- **Jetpack Compose**: BOM-managed (for `lens` module; `lens-noop` has no Compose dependency)

## License

```
Copyright 2024 Behtar Technologies

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
