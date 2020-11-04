package cn.nikeo.permisos.compiler

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.base.Preconditions
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import java.util.Optional
import java.util.stream.Collectors
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import kotlin.collections.LinkedHashSet

/**
 * Metadata class for @[cn.nikeo.permisos.weaving.Permisos] annotated classes.
 */
data class PermisosMetadata(
    val element: TypeElement,
    val baseElement: TypeElement,
    val generatedClassName: ClassName,
    val androidType: AndroidType,
    val baseMetadata: Optional<PermisosMetadata>
) {

    /**
     * The name of the base class given to @Permisos
     */
    @Suppress("DEPRECATION")
    val baseClassName: TypeName
        get() = baseElement.asType().asTypeName()

    /**
     * Modifiers that should be applied to the generated class.
     *
     *
     * Note that the generated class must have public visibility if used by a
     * public @Permisos-annotated kotlin class. See:
     * https://discuss.kotlinlang.org/t/why-does-kotlin-prohibit-exposing-restricted-visibility-types/7047
     */
    fun generatedClassModifiers(): Array<KModifier> {
        return if (isKotlinClass(element) && element.modifiers.contains(Modifier.PUBLIC)) {
            arrayOf(KModifier.ABSTRACT, KModifier.PUBLIC)
        } else {
            arrayOf(KModifier.ABSTRACT)
        }
    }

    /**
     * The type of Permisos element
     */
    private data class Type(
        val androidType: AndroidType,
    ) {
        companion object {

            private val ACTIVITY = Type(AndroidType.ACTIVITY)
            private val FRAGMENT = Type(AndroidType.FRAGMENT)

            fun of(element: TypeElement, baseElement: TypeElement): Type {
                when {
                    Processors.isAssignableFrom(baseElement, AndroidClassNames.ACTIVITY) -> {
                        ProcessorErrors.checkState(
                            Processors.isAssignableFrom(
                                baseElement,
                                AndroidClassNames.COMPONENT_ACTIVITY
                            ),
                            element,
                            "Activities annotated with @Permisos must be a subclass of " +
                                "androidx.activity.ComponentActivity. (e.g. FragmentActivity, " +
                                "AppCompatActivity, etc.)"
                        )
                        return ACTIVITY
                    }
                    Processors.isAssignableFrom(baseElement, AndroidClassNames.FRAGMENT) -> {
                        return FRAGMENT
                    }
                    else -> throw BadInputException(
                        "@Permisos base class must extend ComponentActivity, (support) Fragment.",
                        element
                    )
                }
            }
        }
    }

    companion object {
        fun of(env: ProcessingEnvironment, element: Element): PermisosMetadata {
            return of(env, element, linkedSetOf(element))
        }

        /**
         * Internal implementation for "of" method, checking inheritance cycle utilizing inheritanceTrace
         * along the way.
         */
        @Suppress("UnstableApiUsage")
        private fun of(
            env: ProcessingEnvironment,
            element: Element,
            inheritanceTrace: LinkedHashSet<Element>
        ): PermisosMetadata {
            val permisosAnnotation = permisosAnnotation(element)
            check(permisosAnnotation != null)

            @Suppress("DEPRECATION")
            val annotationClassName =
                MoreTypes.asTypeElement(permisosAnnotation.annotationType).asClassName()

            ProcessorErrors.checkState(
                element.kind == ElementKind.CLASS,
                element,
                "Only classes can be annotated with @%s",
                annotationClassName.simpleName
            )

            val permisosElement = MoreElements.asType(element)
            ProcessorErrors.checkState(
                permisosElement.typeParameters.isEmpty(),
                element,
                "@%s-annotated classes cannot have type parameters.",
                annotationClassName.simpleName
            )

            val baseElement: TypeElement =
                MoreElements.asType(env.typeUtils.asElement(permisosElement.superclass))
            // If this Permisos is a Kotlin class and its base type is also Kotlin and has
            // default values declared in its constructor then error out because for the short-form
            // usage of @Permisos the bytecode transformation will be done incorrectly.
            ProcessorErrors.checkState(
                !KotlinMetadata.of(permisosElement).isPresent ||
                    !KotlinMetadata.of(baseElement)
                        .map(KotlinMetadata::containsConstructorWithDefaultParam)
                        .orElse(false),
                baseElement,
                "The base class, '%s', of the @Permisos, '%s', contains a constructor with " +
                    "default parameters. This is currently not supported by the Gradle plugin.",
                baseElement.qualifiedName,
                permisosElement.qualifiedName
            )
            val generatedClassName: ClassName = generatedClassName(permisosElement)

            val baseMetadata = baseMetadata(
                env,
                permisosElement,
                baseElement,
                inheritanceTrace
            )
            return if (baseMetadata.isPresent) {
                manuallyConstruct(
                    permisosElement,
                    baseElement,
                    generatedClassName,
                    baseMetadata.get().androidType,
                    baseMetadata,
                )
            } else {
                val type: Type = Type.of(permisosElement, baseElement)
                manuallyConstruct(
                    permisosElement,
                    baseElement,
                    generatedClassName,
                    type.androidType,
                    Optional.empty(),
                )
            }
        }

        private fun isKotlinClass(typeElement: TypeElement): Boolean {
            return typeElement.annotationMirrors.asSequence()
                .map { mirror: AnnotationMirror -> mirror.annotationType }
                .any { type: DeclaredType ->
                    @Suppress("DEPRECATION")
                    type.asTypeName() == ClassNames.KOTLIN_METADATA
                }
        }

        /**
         * Returns true if the given element has Permisos metadata.
         */
        private fun hasPermisosMetadata(element: Element): Boolean {
            return permisosAnnotation(element) != null
        }

        private fun permisosAnnotation(element: Element): AnnotationMirror? {
            return element.annotationMirrors
                .firstOrNull { mirror: AnnotationMirror ->
                    @Suppress("DEPRECATION")
                    AndroidClassNames.PERMISOS == mirror.annotationType.asTypeName()
                }
        }

        private fun baseMetadata(
            env: ProcessingEnvironment,
            element: TypeElement,
            baseElement: TypeElement,
            inheritanceTrace: LinkedHashSet<Element>
        ): Optional<PermisosMetadata> {
            ProcessorErrors.checkState(
                inheritanceTrace.add(baseElement),
                element,
                cyclicInheritanceErrorMessage(
                    inheritanceTrace,
                    baseElement
                )
            )
            if (hasPermisosMetadata(baseElement)) {
                val baseMetadata = of(env, baseElement, inheritanceTrace)
                return Optional.of(baseMetadata)
            }
            val superClass = baseElement.superclass
            // None type is returned if this is an interface or Object
            if (superClass.kind != TypeKind.NONE && superClass.kind != TypeKind.ERROR) {
                Preconditions.checkState(superClass.kind == TypeKind.DECLARED)
                return baseMetadata(
                    env,
                    element,
                    MoreTypes.asTypeElement(superClass),
                    inheritanceTrace
                )
            }
            return Optional.empty()
        }

        private fun cyclicInheritanceErrorMessage(
            inheritanceTrace: LinkedHashSet<Element>,
            cycleEntryPoint: TypeElement
        ): String {
            return String.format(
                """
            Cyclic inheritance detected. Make sure the base class of @Permisos is not the annotated class itself or subclass of the annotated class.
            The cyclic inheritance structure: %s --> %s
            
                """.trimIndent(),
                inheritanceTrace.stream()
                    .map { obj: Element -> obj.asType() }
                    .map { obj: TypeMirror -> obj.toString() }
                    .collect(Collectors.joining(" --> ")),
                cycleEntryPoint.asType()
            )
        }

        private fun generatedClassName(element: TypeElement): ClassName {
            val prefix = "Permisos_"
            @Suppress("DEPRECATION")
            return Processors.prepend(
                Processors.getEnclosedClassName(element.asClassName()),
                prefix
            )
        }

        private fun manuallyConstruct(
            element: TypeElement,
            baseElement: TypeElement,
            generatedClassName: ClassName,
            androidType: AndroidType,
            baseMetadata: Optional<PermisosMetadata>
        ): PermisosMetadata {
            return PermisosMetadata(
                element,
                baseElement,
                generatedClassName,
                androidType,
                baseMetadata,
            )
        }
    }
}

/**
 * The Android type of the Permisos element.
 */
enum class AndroidType {
    ACTIVITY, FRAGMENT
}
