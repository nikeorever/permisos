package cn.nikeo.permisos.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.IOException
import javax.annotation.processing.ProcessingEnvironment

class FragmentGenerator(
    private val env: ProcessingEnvironment,
    private val metadata: PermisosMetadata
) {
    private val generatedClassName: ClassName = metadata.generatedClassName

    @Throws(IOException::class)
    fun generate() {
        val builder = TypeSpec.classBuilder(generatedClassName)
            .addOriginatingElement(metadata.element)
            .superclass(metadata.baseClassName)
            .addKdoc(
                """
                A Fragment with the ability to easily check and request dangerous permissions.
                
                @see [https://developer.android.com/training/permissions/requesting]
                """.trimIndent()
            )
            .addModifiers(*metadata.generatedClassModifiers())

        Generators.addRequireActivityProperty(metadata.androidType, builder)
        Generators.addPermissionsCheckerAndImplementation(builder)
        Generators.addPermissionConfiguration(builder)
        Generators.addOnRequestPermissionsResultFunOverride(builder)

        FileSpec.builder(generatedClassName.packageName, generatedClassName.simpleName)
            .addComment("Generated file. Do not edit!")
            .addImport(
                AndroidClassNames.PACKAGE_MANAGER.packageName,
                AndroidClassNames.PACKAGE_MANAGER.simpleName
            )
            .addImport(
                AndroidClassNames.CALL_SUPER.packageName,
                AndroidClassNames.CALL_SUPER.simpleName
            )
            .addImport(
                AndroidClassNames.ACTIVITY_COMPAT.packageName,
                AndroidClassNames.ACTIVITY_COMPAT.simpleName
            )
            .addImport(
                ClassNames.PERMISSION_TYPE.packageName,
                ClassNames.PERMISSION_TYPE.simpleName
            )
            .addImport(
                ClassNames.GROUP_PERMISSIONS.packageName,
                ClassNames.GROUP_PERMISSIONS.simpleName
            )
            .addType(builder.build())
            .build()
            .writeTo(env.filer)
    }
}
