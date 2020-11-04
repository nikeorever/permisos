package cn.nikeo.permisos.compiler

import com.google.auto.service.AutoService
import com.google.common.collect.ImmutableSet
import javax.annotation.processing.Processor
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class PermisosCompiler : BaseProcessor() {

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return ImmutableSet.of(AndroidClassNames.PERMISOS.toString())
    }

    override fun delayErrors(): Boolean {
        return true
    }

    override fun processEach(annotation: TypeElement, element: Element) {
        val metadata = PermisosMetadata.of(env = processingEnv, element = element)
        when (metadata.androidType) {
            AndroidType.ACTIVITY -> {
                ActivityGenerator(env = processingEnv, metadata = metadata).generate()
            }
            AndroidType.FRAGMENT -> {
                FragmentGenerator(env = processingEnv, metadata = metadata).generate()
            }
        }
    }
}
