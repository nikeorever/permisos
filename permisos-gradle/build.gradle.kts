plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("com.jakewharton.confundus")
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("permisos") {
            id = "cn.nikeo.permisos"
            displayName = "" // TODO WHAT?
            description = "Easy to check and request dangerous permissions on Android."
            implementationClass = "cn.nikeo.permisos.gradle.PermisosPlugin"
        }
    }
}

sourceSets {
    main {
        java {
            srcDir("$buildDir/gen")
        }
    }
}

val generateVersionsTask = tasks.register("generateVersions") {
    val outputDir = file("$buildDir/gen")

    inputs.property("permisosVersion", project.property("VERSION_NAME"))
    outputs.dir(outputDir)

    doLast {
        val versionFile = file("$outputDir/cn/nikeo/permisos/gradle/Versions.kt")
        versionFile.parentFile.mkdirs()
        versionFile.writeText(
            """
            // Generated file. Do not edit!
            package cn.nikeo.permisos.gradle
            
            // Version of permisos
            internal const val VERSION_PERMISOS = "${inputs.properties["permisosVersion"]}"
            
            """.trimIndent()
        )
    }
}

tasks.getByName("compileKotlin").dependsOn(generateVersionsTask)

dependencies {
    compileOnly(Dependencies.Android.gradlePlugin)
    compileOnly(gradleApi())
    compileOnly(kotlin("gradle-plugin"))
    implementation(Dependencies.AspectJ.jrt)
    implementation(Dependencies.AspectJ.tools)
}

apply("$rootDir/gradle/configure-maven-publish.gradle")
