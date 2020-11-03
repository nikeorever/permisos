@file:JvmName("Deps")

object Versions {
    const val targetSdk = 30
    const val minSdk = 21
}

object Dependencies {
    object Kotlin {
        private fun kotlin(module: String, version: String) = "org.jetbrains.kotlin:kotlin-$module:$version"
        fun reflect(version: String) = kotlin("reflect", version)
        val gradlePlugin = kotlin("gradle-plugin", "_")

        object Stdlib {
            fun common(version: String) = kotlin("stdlib-common", version)
            fun jdk8(version: String) = kotlin("stdlib-jdk8", version)
            fun jdk7(version: String) = kotlin("stdlib-jdk7", version)
            fun jdk6(version: String) = kotlin("stdlib", version)
        }
    }

    const val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:_"

    object Android {
        const val gradlePlugin = "com.android.tools.build:gradle:_"
    }

    object AndroidX {
        const val annotation = "androidx.annotation:annotation:_"
    }

    object AspectJ {
        const val jrt = "org.aspectj:aspectjrt:_"
        const val tools = "org.aspectj:aspectjtools:_"
    }

    object Squareup {
        const val phrase = "com.squareup.phrase:phrase:_"
        const val kotlinpoet = "com.squareup:kotlinpoet:_"
    }

    const val mavenPublish = "com.vanniktech:gradle-maven-publish-plugin:_"
    const val ktlint = "org.jlleitschuh.gradle:ktlint-gradle:_"
    const val detekt = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:_"
    const val confundus = "com.jakewharton.confundus:confundus-gradle:_"
}