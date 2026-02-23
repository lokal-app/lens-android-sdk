# Lens SDK Consumer ProGuard Rules
# These rules are automatically included in consuming apps via consumerProguardFiles.

# ── Public API ─────────────────────────────────────────────
# Keep all public interfaces, data classes, and enums
-keep class com.behtar.lens.api.** { *; }
-keep interface com.behtar.lens.api.** { *; }

# ── Plugin system ──────────────────────────────────────────
# Keep all plugin implementations (base + Compose + View variants)
-keep class * implements com.behtar.lens.api.LensPlugin { *; }
-keep class * implements com.behtar.lens.api.ComposableLensPlugin { *; }
-keep class * implements com.behtar.lens.api.ViewLensPlugin { *; }

# ── Provider implementations ───────────────────────────────
# Keep provider implementations (registered by host app)
-keep class * implements com.behtar.lens.api.EnvironmentProvider { *; }
-keep class * implements com.behtar.lens.api.FeatureFlagProvider { *; }
-keep class * implements com.behtar.lens.api.QuickActionsProvider { *; }
-keep class * implements com.behtar.lens.api.RemoteActivationProvider { *; }
-keep class * implements com.behtar.lens.api.HeaderRedactor { *; }

# ── Internal components ────────────────────────────────────
# Keep the notification receiver (registered in AndroidManifest.xml)
-keep class com.behtar.lens.internal.notification.LensNotificationReceiver { *; }

# Keep the Lens singleton entry point
-keep class com.behtar.lens.api.Lens { *; }
