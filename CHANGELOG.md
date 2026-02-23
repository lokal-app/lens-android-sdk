# Changelog

All notable changes to the Lens Android SDK will be documented in this file.

This project follows [Semantic Versioning](https://semver.org/):
- **MAJOR** — Breaking API changes (e.g., renamed interfaces, removed methods)
- **MINOR** — New features, new plugins (backward compatible)
- **PATCH** — Bug fixes, performance improvements

## [1.0.0] — 2026-02-24

### Initial Release

#### Core
- Floating bubble overlay (injected into DecorView, no permissions needed)
- 5-tap and shake activation gestures
- Sticky notification with live request/error counts
- Generic key-value settings store (`getString`/`putString`/`getBoolean`/`putBoolean`)
- Remote activation provider (kill switch without app update)
- Header redaction for sensitive HTTP headers
- HAR 1.2 and JSON data export via share sheet
- ANR detection (5s watchdog with main thread stack trace capture)

#### Built-in Plugins
- Network Inspector (OkHttp interceptor, cURL export, body truncation)
- Global Search (cross-plugin search with 300ms debounce)
- App Info (build, device, session metadata)
- Performance Monitor (FPS via Choreographer, memory via Runtime, jank detection)
- Analytics Inspector (events, user properties, revenue events)
- Exception Tracker (uncaught + handled exceptions, disk persistence)
- Database Inspector (SQLite browser)
- SharedPreferences Editor (view/edit all prefs files)
- Deep Link Tester
- Log Viewer (Timber integration)
- Cache Manager

#### Optional Plugins (Provider-based)
- Environment Switcher (data-driven from `EnvironmentProvider`)
- Feature Flags Editor (from `FeatureFlagProvider`)
- Quick Actions (from `QuickActionsProvider`)

#### Architecture
- Plugin system: `LensPlugin` (base), `ComposableLensPlugin` (Compose), `ViewLensPlugin` (Views)
- Pure-Kotlin `:lens-api` module (zero Android dependencies)
- No-op `:lens-noop` module for release builds
- No Hilt/Dagger — internal service locator
- `@LensExperimental` annotation for unstable APIs
- ProGuard consumer rules bundled
- 116 unit tests
