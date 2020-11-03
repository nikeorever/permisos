plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("com.jakewharton.confundus")
}

apply("$rootDir/gradle/configure-maven-publish.gradle")
