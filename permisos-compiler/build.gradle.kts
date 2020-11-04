plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.dokka")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xopt-in=com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview"
        )
    }
}

dependencies {
    implementation(project(":permisos-annotations"))
    implementation(Dependencies.Kotlinx.metadataJvm)
    implementation(Dependencies.Google.Auto.common)
    kapt(Dependencies.Google.Auto.service)
    compileOnly(Dependencies.Google.Auto.service)
    implementation(Dependencies.Squareup.Kotlinpoet.runtime)
    implementation(Dependencies.Squareup.Kotlinpoet.metadata)
    implementation(Dependencies.Squareup.Kotlinpoet.metadataSpecs)
    compileOnly(files(org.gradle.internal.jvm.Jvm.current().toolsJar))
}

apply("$rootDir/gradle/configure-maven-publish.gradle")
