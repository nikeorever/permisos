package cn.nikeo.permisos.compiler

import com.google.auto.common.MoreElements
import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * Implements default configurations for Processors, and provides structure for exception handling.
 *
 *
 * By default #process() will do the following:
 *
 *
 *  1.  #preRoundProcess()
 *  1.  foreach element:
 *  *  #processEach()
 *
 *  1.  #postRoundProcess()
 *  1.  #claimAnnotation()
 *
 *
 *
 * #processEach() allows each element to be processed, even if exceptions are thrown. Due to the
 * non-deterministic ordering of the processed elements, this is needed to ensure a consistent set
 * of exceptions are thrown with each build.
 */
abstract class BaseProcessor : AbstractProcessor() {
    /**
     * Stores the state of processing for a given annotation and element.
     * [annotationClassName] represents the class name of the annotation.
     * [elementClassName] represents the annotated element to process.
     */
    private data class ProcessingState(
        val annotationClassName: ClassName,
        val elementClassName: ClassName
    ) {
        /** Returns the annotation that triggered the processing.  */
        fun annotation(elements: Elements): TypeElement {
            return elements.getTypeElement(elementClassName.toString())
        }

        /** Returns the annotated element to process.  */
        fun element(elements: Elements): TypeElement {
            return elements.getTypeElement(annotationClassName.toString())
        }

        companion object {
            @Suppress("UnstableApiUsage")
            fun of(annotation: TypeElement, element: Element): ProcessingState {
                // We currently only support TypeElements directly annotated with the annotation.
                // TODO(bcorso): Switch to using BasicAnnotationProcessor if we need more than this.
                // Note: Switching to BasicAnnotationProcessor is currently not possible because of cyclic
                // references to generated types in our API. For example, an @AndroidEntryPoint annotated
                // element will indefinitely defer its own processing because it extends a generated type
                // that it's responsible for generating.
                check(MoreElements.isType(element))
                @Suppress("DEPRECATION")
                check(
                    Processors.hasAnnotation(
                        element,
                        annotation.asClassName()
//                        annotation::class.toImmutableKmClass().name.asKotlinpoetClassName()
                    )
                )
                @Suppress("DEPRECATION")
                return ProcessingState(
                    annotation.asClassName(),
                    MoreElements.asType(element).asClassName()
//                    annotation::class.toImmutableKmClass().name.asKotlinpoetClassName(),
//                    MoreElements.asType(element)::class.toImmutableKmClass().name.asKotlinpoetClassName()
                )
            }
        }
    }

    private val stateToReprocess: MutableSet<ProcessingState> by lazy { LinkedHashSet() }
    lateinit var elementUtils: Elements
    lateinit var typeUtils: Types
    lateinit var messager: Messager

    /** @return the error handle for the processor.
     */
    lateinit var errorHandler: ProcessorErrorHandler

    /** Used to perform initialization before each round of processing.  */
    protected open fun preRoundProcess(roundEnv: RoundEnvironment) {}

    /**
     * Called for each element in a round that uses a supported annotation.
     *
     * Note that an exception can be thrown for each element in the round. This is usually preferred
     * over throwing only the first exception in a round. Only throwing the first exception in the
     * round can lead to flaky errors that are dependent on the non-deterministic ordering that the
     * elements are processed in.
     */
    @Throws(Exception::class)
    protected open fun processEach(annotation: TypeElement, element: Element) {
    }

    /**
     * Used to perform post processing at the end of a round. This is especially useful for handling
     * additional processing that depends on aggregate data, that cannot be handled in #processEach().
     *
     *
     * Note: this will not be called if an exception is thrown during #processEach() -- if we have
     * already detected errors on an annotated element, performing post processing on an aggregate
     * will just produce more (perhaps non-deterministic) errors.
     */
    @Throws(Exception::class)
    protected open fun postRoundProcess(roundEnv: RoundEnvironment) {
    }

    /** @return true if you want to claim annotations after processing each round. Default false.
     */
    protected open fun claimAnnotations(): Boolean {
        return false
    }

    /**
     * @return true if you want to delay errors to the last round. Useful if the processor
     * generates code for symbols used a lot in the user code. Delaying allows as much code to
     * compile as possible for correctly configured types and reduces error spam.
     */
    protected open fun delayErrors(): Boolean {
        return false
    }

    @Synchronized
    override fun init(processingEnvironment: ProcessingEnvironment) {
        super.init(processingEnvironment)
        messager = processingEnv.messager
        elementUtils = processingEnv.elementUtils
        typeUtils = processingEnv.typeUtils
        errorHandler = ProcessorErrorHandler(processingEnvironment)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    /**
     * This should not be overridden, as it defines the order of the processing.
     */
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        preRoundProcess(roundEnv)
        var roundError = false

        // Gather the set of new and deferred elements to process, grouped by annotation.
        val elementMultiMap: SetMultimap<TypeElement, Element> = LinkedHashMultimap.create()
        for (processingState in stateToReprocess) {
            elementMultiMap.put(
                processingState.annotation(elementUtils),
                processingState.element(elementUtils)
            )
        }
        for (annotation in annotations) {
            elementMultiMap.putAll(annotation, roundEnv.getElementsAnnotatedWith(annotation))
        }

        // Clear the processing state before reprocessing.
        stateToReprocess.clear()
        for ((annotation, value) in elementMultiMap.asMap()) {
            for (element in value) {
                try {
                    processEach(annotation, element)
                } catch (e: Exception) {
                    if (e is ErrorTypeException && !roundEnv.processingOver()) {
                        // Allow an extra round to reprocess to try to resolve this type.
                        stateToReprocess.add(ProcessingState.of(annotation, element))
                    } else {
                        errorHandler.recordError(e)
                        roundError = true
                    }
                }
            }
        }
        if (!roundError) {
            try {
                postRoundProcess(roundEnv)
            } catch (e: Exception) {
                errorHandler.recordError(e)
            }
        }
        if (!delayErrors() || roundEnv.processingOver()) {
            errorHandler.checkErrors()
        }
        return claimAnnotations()
    }
}
