package cn.nikeo.permisos.compiler

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import java.io.IOException
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.TypeMirror

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
        Generators.addPermissionsCheckerAndImplementation(metadata.androidType, builder)
        Generators.addPermissionConfiguration(builder)
        Generators.addOnRequestPermissionsResultFunOverride(builder)

        Generators.copyLintAnnotations(metadata.element, builder)
        Generators.copyConstructors(metadata.baseElement, builder)

        metadata.baseElement.typeParameters.stream()
            .map { element: TypeParameterElement ->
                TypeVariableName(
                    name = element.simpleName.toString(),
                    bounds = element.bounds.asSequence().map { mirror: TypeMirror ->
                        @Suppress("DEPRECATION")
                        mirror.asTypeName()
                    }.filter { typeName: TypeName ->
                        typeName != ANY && typeName != ClassNames.JAVA_OBJECT
                    }.ifEmpty {
                        sequenceOf(ANY.copy(nullable = true))
                    }.toList()
                )
            }.forEachOrdered { typeVariable: TypeVariableName ->
                builder.addTypeVariable(
                    typeVariable
                )
            }

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
