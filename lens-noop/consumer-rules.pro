# Lens No-Op Consumer ProGuard Rules
# Keep public API stubs so the host app compiles correctly in release builds.
-keep class com.behtar.lens.api.** { *; }
-keep interface com.behtar.lens.api.** { *; }
