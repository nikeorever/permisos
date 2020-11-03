package cn.nikeo.permisos.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.BuildType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main

class PermisosPlugin : Plugin<Project> {
    private lateinit var project: Project

    override fun apply(project: Project) {
        this.project = project

        val isApp = project.plugins.hasPlugin(AppPlugin::class.java)
        val isLibrary = project.plugins.hasPlugin(LibraryPlugin::class.java)

        require(isApp || isLibrary) {
            "com.android.application' or 'com.android.library' plugin required."
        }

        val android = project.extensions.getByType(BaseExtension::class.java)
        android.buildTypes.onEach { addDependencies(it) }.whenObjectAdded { addDependencies(it) }

        val variants = when {
            isApp -> project.extensions.getByType(AppExtension::class.java).applicationVariants
            isLibrary -> project.extensions.getByType(LibraryExtension::class.java).libraryVariants
            else -> error("The gradle plugin[cn.nikeo.permisos] just support app or library")
        }

        project.tasks.withType(KotlinCompile::class.java) { kotlinCompile: KotlinCompile ->
            kotlinCompile.doLast {
                ajc(
                    inPath = kotlinCompile.destinationDir.toString(),
                    aspectPath = kotlinCompile.classpath.asPath,
                    d = kotlinCompile.destinationDir.toString(),
                    classPath = kotlinCompile.classpath.asPath,
                    bootClassPath = android.bootClasspath.joinToString(File.pathSeparator)
                )
            }
        }
        variants.onEach { configureVariant(android, it) }
            .whenObjectAdded { configureVariant(android, it) }
    }

    private fun configureVariant(android: BaseExtension, variant: BaseVariant) {
        variant.javaCompileProvider.get().also { javaCompile: JavaCompile ->
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
    }

    private fun ajc(
        inPath: String,
        aspectPath: String,
        d: String,
        classPath: String,
        bootClassPath: String
    ) {
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

    private fun addDependencies(buildType: BuildType) {
        project.rootProject.allprojects { allProject ->
            allProject.repositories.mavenCentral()
        }

        val configuration = if (project.configurations.findByName("api") != null)
            "${buildType.name}Api"
        else
            "${buildType.name}Implementation"

        project.dependencies.add(
            configuration,
            "cn.nikeo.permisos:permisos-runtime:$VERSION_PERMISOS"
        )
        project.dependencies.add(
            configuration,
            "cn.nikeo.permisos:permisos-annotations:$VERSION_PERMISOS"
        )
        project.dependencies.add(
            configuration,
            "org.aspectj:aspectjrt:$VERSION_ASPECTJRT"
        )
    }
}
