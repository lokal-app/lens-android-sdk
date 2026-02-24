plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/lokal-app/lens-android-sdk")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("GITHUB_USERNAME").orNull
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("GITHUB_TOKEN").orNull
            }
        }
    }
    publications {
        create<MavenPublication>("release") {
            from(components["java"])

            groupId = project.property("LENS_GROUP") as String
            artifactId = "lens-api"
            version = project.property("LENS_VERSION") as String

            pom {
                name.set("Lens API")
                description.set("Pure-Kotlin interfaces for the Lens Android debug SDK. Zero Android dependencies.")
                url.set("https://github.com/lokal-app/lens-android-sdk")

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
