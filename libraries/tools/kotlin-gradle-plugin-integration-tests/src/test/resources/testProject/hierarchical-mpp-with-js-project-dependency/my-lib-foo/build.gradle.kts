plugins {
    kotlin("js")
    `maven-publish`
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "com.example.foo"
version = "1.0"

kotlin {
    js()

    sourceSets {
        js().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        js().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile> {
    kotlinOptions.freeCompilerArgs += "-Xforce-deprecated-legacy-compiler-usage"
}
