@file:JvmName("Deps")

object Versions {
    const val targetSdk = 30
    const val minSdk = 21
}

object Dependencies {
    object Kotlin {
        private fun kotlin(module: String, version: String) =
            "org.jetbrains.kotlin:kotlin-$module:$version"

        fun reflect(version: String) = kotlin("reflect", version)
        val gradlePlugin = kotlin("gradle-plugin", "_")

        object Stdlib {
            fun common(version: String) = kotlin("stdlib-common", version)
            fun jdk8(version: String) = kotlin("stdlib-jdk8", version)
            fun jdk7(version: String) = kotlin("stdlib-jdk7", version)
            fun jdk6(version: String) = kotlin("stdlib", version)
        }
    }

    object Kotlinx {
        private fun kotlinx(module: String, version: String) =
            "org.jetbrains.kotlinx:kotlinx-$module:$version"

        val metadataJvm = kotlinx("metadata-jvm", "_")
    }

    const val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:_"

    object Android {
        const val gradlePlugin = "com.android.tools.build:gradle:_"
    }

    object AndroidX {
        const val annotation = "androidx.annotation:annotation:_"
        const val fragment = "androidx.fragment:fragment:_"
        const val activity = "androidx.activity:activity:_"
        object Core {
            const val runtime = "androidx.core:core:_"
            const val ktx = "androidx.core:core-ktx:_"
        }
    }

    object Google {
        object Auto {
            const val service = "com.google.auto.service:auto-service:_"
            const val common = "com.google.auto:auto-common:_"
            const val value = "com.google.auto.value:auto-value:_"
        }
    }

    object AspectJ {
        const val jrt = "org.aspectj:aspectjrt:_"
        const val tools = "org.aspectj:aspectjtools:_"
    }

    object Squareup {
        object Kotlinpoet {
            const val runtime = "com.squareup:kotlinpoet:_"
            const val metadata = "com.squareup:kotlinpoet-metadata:_"
            const val metadataSpecs = "com.squareup:kotlinpoet-metadata-specs:_"
        }
    }

    const val mavenPublish = "com.vanniktech:gradle-maven-publish-plugin:_"
    const val ktlint = "org.jlleitschuh.gradle:ktlint-gradle:_"
    const val detekt = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:_"
    const val confundus = "com.jakewharton.confundus:confundus-gradle:_"
    const val jarTransformer = "cn.nikeo.jar-transformer:jar-transformer:_"
    const val javassist = "org.javassist:javassist:_"
}