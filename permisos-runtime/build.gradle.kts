import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main

plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.dokka")
}

apply("$rootDir/gradle/configure-maven-publish.gradle")

android {
    compileSdkVersion(Versions.targetSdk)

    defaultConfig {
        minSdkVersion(Versions.minSdk)
        targetSdkVersion(Versions.targetSdk)
        versionCode(1)
        versionName("1.0")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        buildConfig = false
        viewBinding = false
    }
}

fun ajc(inPath: String, aspectPath: String, d: String, classPath: String, bootClassPath: String) {
    val handler = MessageHandler()
    // see also https://www.eclipse.org/aspectj/doc/released/devguide/ajc-ref.html
    val args = arrayOf(
        "-showWeaveInfo", // Emit messages about weaving
        "-1.5", // Set compliance level to 1.5. This implies -source 1.5 and -target 1.5.
        "-inpath", // Accept as source bytecode any .class files in the .jar files or directories on Path. The output will include these classes, possibly as woven with any applicable aspects. Path is a single argument containing a list of paths to zip files or directories, delimited by the platform-specific path delimiter.
        inPath,
        "-aspectpath", // Weave binary aspects from jar files and directories on path into all sources. The aspects should have been output by the same version of the compiler. When running the output classes, the run classpath should contain all aspectpath entries. Path, like classpath, is a single argument containing a list of paths to jar files, delimited by the platform- specific classpath delimiter.
        aspectPath,
        "-d", // Specify where to place generated .class files. If not specified, Directory defaults to the current working dir.
        d,
        "-classpath", // Specify where to find user class files. Path is a single argument containing a list of paths to zip files or directories, delimited by the platform-specific path delimiter.
        classPath,
        "-bootclasspath", // Override location of VM's bootclasspath for purposes of evaluating types when compiling. Path is a single argument containing a list of paths to zip files or directories, delimited by the platform-specific path delimiter.
        bootClassPath
    )
    val log = project.logger

    log.debug("ajc args: ${args.joinToString(" ")}")
    /*

                      Property	           Default	        Description
        org.aspectj.weaver.Dump.exception	true	Generate an ajcore files when an exception thrown.
        org.aspectj.weaver.Dump.condition	abort	Message kind for which to generate ajcore e.g. error.
        org.aspectj.dump.directory	        none	The directory used for ajcore files.
    */
    org.aspectj.weaver.Dump.setDumpOnException(false)

    Main().run(args, handler)
    handler.getMessages(null, true).forEach { message ->
        when (message.kind) {
            IMessage.ABORT, IMessage.ERROR, IMessage.FAIL -> {
                log.error(message.message, message.thrown)
            }
            IMessage.WARNING -> {
                log.warn(message.message, message.thrown)
            }
            IMessage.INFO -> {
                log.info(message.message, message.thrown)
            }
            IMessage.DEBUG -> {
                log.debug(message.message, message.thrown)
            }
        }
    }
}

android.libraryVariants.all {
    val javaCompile = javaCompileProvider.get()
    javaCompile.doLast {
        ajc(
            inPath = javaCompile.destinationDir.toString(),
            aspectPath = javaCompile.classpath.asPath,
            d = javaCompile.destinationDir.toString(),
            classPath = javaCompile.classpath.asPath,
            bootClassPath = android.bootClasspath.joinToString(File.pathSeparator)
        )
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    doLast {
        ajc(
            inPath = destinationDir.toString(),
            aspectPath = classpath.asPath,
            d = destinationDir.toString(),
            classPath = classpath.asPath,
            bootClassPath = android.bootClasspath.joinToString(File.pathSeparator)
        )
    }
}

dependencies {
    implementation(project(":permisos-annotations"))
    api(Dependencies.AndroidX.annotation)
    implementation(Dependencies.AspectJ.jrt)
    api(Dependencies.AndroidX.Core.runtime)
    api(Dependencies.AndroidX.Core.ktx)
}
