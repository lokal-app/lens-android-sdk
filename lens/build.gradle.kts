plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.lokalapps.lens"
    compileSdk = 37

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


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
        artifactId = "lens",
        version = project.property("LENS_VERSION") as String,
    )

    pom {
        name.set("Lens")
        description.set(
            "On-device debug toolkit for Android apps. Network inspector, environment switcher, feature flags, and more.")
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

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Lens API (pure-Kotlin interfaces, shared with lens-noop)
    api(project(":lens-api"))

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.coroutines)
    implementation(libs.timber)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.lifecycle.viewmodel.compose)
    debugImplementation(libs.compose.ui.tooling)

    // OkHttp (for network interceptor)
    implementation(libs.okhttp)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

