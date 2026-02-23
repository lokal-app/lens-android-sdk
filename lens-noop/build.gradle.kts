plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

android {
    namespace = "com.behtar.lens"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    // Lens API (pure-Kotlin interfaces, shared with lens)
    api(project(":lens-api"))

    // Minimal dependencies - only what's needed for interface compatibility
    compileOnly(libs.okhttp)

    // Compose BOM for version management
    compileOnly(platform(libs.compose.bom))

    // For @Composable annotation in DevToolsPlugin interface
    compileOnly(libs.compose.ui)

    // For @DrawableRes annotation
    compileOnly(libs.androidx.annotation)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = project.property("LENS_GROUP") as String
                artifactId = "lens-noop"
                version = project.property("LENS_VERSION") as String

                pom {
                    name.set("Lens No-Op")
                    description.set("No-op stubs for Lens Android debug SDK. Use in release builds for zero overhead.")
                    url.set("https://github.com/behtar/lens-android-sdk")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                }
            }
        }
    }
}