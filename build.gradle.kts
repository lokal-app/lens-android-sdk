// Top-level build file for the Lens Android SDK
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            ktfmt("0.50")
        }

        java {
            importOrder()
            target("**/*.java")
            googleJavaFormat()
            formatAnnotations()
        }
    }
}

buildscript {
    dependencies {
        classpath(libs.spotless.plugin.gradle)
    }
}