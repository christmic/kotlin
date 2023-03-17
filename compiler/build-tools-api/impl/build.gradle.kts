plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":build-tools-api"))
    implementation(kotlinStdlib())
}

publish()

standardPublicJars()