plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.lokalapps.lens"
    compileSdk = 37

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/lokal-app/lens-android-sdk")
            credentials {
                username =
                    System.getenv("GITHUB_ACTOR")
                        ?: providers.gradleProperty("GITHUB_USERNAME").orNull
                password =
                    System.getenv("GITHUB_TOKEN")
                        ?: providers.gradleProperty("GITHUB_TOKEN").orNull
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = project.property("LENS_GROUP") as String,
        artifactId = "lens-noop",
        version = project.property("LENS_VERSION") as String,
    )

    pom {
        name.set("Lens No-Op")
        description.set("No-op stubs for Lens Android debug SDK. Use in release builds for zero overhead.")
        inceptionYear.set("2026")
        url.set("https://github.com/lokal-app/lens-android-sdk")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("lokal-app")
                name.set("Lokal")
                url.set("https://github.com/lokal-app")
            }
        }

        scm {
            url.set("https://github.com/lokal-app/lens-android-sdk")
            connection.set("scm:git:git://github.com/lokal-app/lens-android-sdk.git")
            developerConnection.set("scm:git:ssh://git@github.com/lokal-app/lens-android-sdk.git")
        }
    }
}