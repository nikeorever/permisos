package cn.nikeo.permisos.gradle

import cn.nikeo.transformer.jar.transformJar
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import java.io.File

class PermisosTransform : com.android.build.api.transform.Transform() {
    override fun getName(): String = "PermisosTransform"

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> =
        TransformManager.CONTENT_CLASS

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> =
        TransformManager.SCOPE_FULL_PROJECT

    override fun isIncremental(): Boolean = true

    override fun transform(invocation: TransformInvocation) {
        if (!invocation.isIncremental) {
            invocation.outputProvider.deleteAll()
        }

        invocation.inputs.forEach { transformInput ->
            transformInput.jarInputs.forEach { jarInput ->
                val jarOutput = invocation.outputProvider.getContentLocation(
                    jarInput.name,
                    jarInput.contentTypes,
                    jarInput.scopes,
                    Format.JAR
                )
                if (invocation.isIncremental) {
                    when (jarInput.status) {
                        Status.ADDED, Status.CHANGED -> {
                            transformJarContents(jarInput.file, jarOutput)
                        }
                        Status.REMOVED -> {
                            jarOutput.delete()
                        }
                        Status.NOTCHANGED -> {
                            // No need to transform.
                        }
                        else -> {
                            error("Unknown status: ${jarInput.status}")
                        }
                    }
                } else {
                    transformJarContents(jarInput.file, jarOutput)
                }
            }

            transformInput.directoryInputs.forEach { directoryInput ->
                val outputDir = invocation.outputProvider.getContentLocation(
                    directoryInput.name,
                    directoryInput.contentTypes,
                    directoryInput.scopes,
                    Format.DIRECTORY
                )
                val classTransformer =
                    createPermisosClassTransformer(
                        invocation.inputs,
                        invocation.referencedInputs,
                        outputDir
                    )
                if (invocation.isIncremental) {
                    directoryInput.changedFiles.forEach { (file, status) ->
                        val outputFile = toOutputFile(outputDir, directoryInput.file, file)
                        when (status) {
                            Status.ADDED, Status.CHANGED ->
                                transformFile(file, outputFile.parentFile, classTransformer)
                            Status.REMOVED -> outputFile.delete()
                            Status.NOTCHANGED -> {
                                // No need to transform.
                            }
                            else -> {
                                error("Unknown status: $status")
                            }
                        }
                    }
                } else {
                    directoryInput.file.walkTopDown().forEach { file ->
                        val outputFile = toOutputFile(outputDir, directoryInput.file, file)
                        transformFile(file, outputFile.parentFile, classTransformer)
                    }
                }
            }
        }

    }

    // Create a transformer given an invocation inputs. Note that since this is a PROJECT scoped
    // transform the actual transformation is only done on project files and not its dependencies.
    private fun createPermisosClassTransformer(
        inputs: Collection<TransformInput>,
        referencedInputs: Collection<TransformInput>,
        outputDir: File
    ): PermisosClassTransformer {
        val classFiles = (inputs + referencedInputs).flatMap { input ->
            (input.directoryInputs + input.jarInputs).map { it.file }
        }
        return PermisosClassTransformer(
            taskName = name,
            allInputs = classFiles,
            sourceRootOutputDir = outputDir,
            copyNonTransformed = true
        )
    }

    private fun toOutputFile(outputDir: File, inputDir: File, inputFile: File) =
        File(outputDir, inputFile.relativeTo(inputDir).path)

    private fun transformJarContents(jarInput: File, jarOutput: File) {
        transformJar(
            jarInput = jarInput,
            jarOutput = jarOutput
        ) { _, outputJarEntryInputStream ->
            outputJarEntryInputStream.readBytes()
        }
    }

    // Transform a single file. If the file is not a class file it is just copied to the output dir.
    private fun transformFile(
        inputFile: File,
        outputDir: File,
        transformer: PermisosClassTransformer
    ) {
        if (inputFile.isClassFile()) {
            transformer.transformFile(inputFile)
        } else if (inputFile.isFile) {
            // Copy all non .class files to the output.
            outputDir.mkdirs()
            val outputFile = File(outputDir, inputFile.name)
            inputFile.copyTo(target = outputFile, overwrite = true)
        }
    }
}
