package cn.nikeo.permisos.compiler

import com.google.auto.service.AutoService
import com.google.common.collect.ImmutableSet
import javax.annotation.processing.Processor
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class RequiredPermissionsCompiler : BaseProcessor() {

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return ImmutableSet.of(ClassNames.REQUIRED_PERMISSIONS.toString())
    }

    override fun delayErrors(): Boolean {
        return true
    }

    override fun processEach(annotation: TypeElement, element: Element) {
        val enclosingElement = element.enclosingElement

        ProcessorErrors.checkState(
            enclosingElement.kind == ElementKind.CLASS,
            element,
            "The method annotated by @%s must be in a class",
            ClassNames.REQUIRED_PERMISSIONS.simpleName
        )

        val typeElement = enclosingElement as TypeElement
        if (Processors.isAssignableFrom(typeElement, AndroidClassNames.ACTIVITY)) {
            ProcessorErrors.checkState(
                Processors.isAssignableFrom(
                    typeElement,
                    AndroidClassNames.COMPONENT_ACTIVITY
                ),
                typeElement,
                "A class that contains methods annotated by @%s must be a subclass of " +
                        "androidx.activity.ComponentActivity. (e.g. FragmentActivity, " +
                        "AppCompatActivity, etc.)",
                ClassNames.REQUIRED_PERMISSIONS.simpleName
            )
        } else {
            ProcessorErrors.checkState(
                Processors.isAssignableFrom(typeElement, AndroidClassNames.FRAGMENT),
                typeElement,
                "A class that contains methods annotated by @%s must be a subclass of " +
                        "androidx.activity.ComponentActivity. (e.g. FragmentActivity, " +
                        "AppCompatActivity, etc.) or Fragment",
                ClassNames.REQUIRED_PERMISSIONS.simpleName
            )
        }
        ProcessorErrors.checkState(
            Processors.hasAnnotation(typeElement, ClassNames.PERMISOS),
            typeElement,
            "A class that contains methods annotated by @%s must be annotated " +
                    "by @%s.",
            ClassNames.REQUIRED_PERMISSIONS.simpleName,
            ClassNames.PERMISOS
        )
    }
}
