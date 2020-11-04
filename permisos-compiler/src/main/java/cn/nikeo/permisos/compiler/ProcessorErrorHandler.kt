package cn.nikeo.permisos.compiler

import com.google.auto.common.MoreElements
import com.google.common.base.Throwables
import java.util.Optional
import java.util.function.Consumer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.util.Elements
import javax.tools.Diagnostic
import kotlin.collections.ArrayList

/**
 * Utility class to handle keeping track of errors during processing.
 */
class ProcessorErrorHandler(env: ProcessingEnvironment) {
    private val messager: Messager by lazy { env.messager }
    private val elements: Elements by lazy { env.elementUtils }
    private val permisosErrors: MutableList<PermisosError> by lazy { ArrayList() }

    /**
     * Records an error message for some exception to the messager. This can be used to handle
     * exceptions gracefully that would otherwise be propagated out of the `process` method. The
     * message is stored in order to allow the build to continue as far as it can. The build will be
     * failed with a [Kind.ERROR] in [.checkErrors] if an error was recorded with this
     * method.
     */
    fun recordError(t: Throwable) {
        // Store messages to allow the build to continue as far as it can. The build will
        // be failed in checkErrors when processing is over.
        when {
            t is BadInputException -> {
                for (element in t.badElements) {
                    permisosErrors.add(PermisosError.of(t.message, element))
                }
            }
            t is ErrorTypeException -> {
                permisosErrors.add(PermisosError.of(t.message, t.badElement))
            }
            t.message != null -> {
                permisosErrors.add(
                    PermisosError.of(
                        t.message + ": " + Throwables.getStackTraceAsString(t)
                    )
                )
            }
            else -> {
                permisosErrors.add(
                    PermisosError.of(
                        t.javaClass.toString() + ": " + Throwables.getStackTraceAsString(t)
                    )
                )
            }
        }
    }

    /** Checks for any recorded errors. This should be called at the end of process every round.  */
    @Suppress("UnstableApiUsage")
    fun checkErrors() {
        if (permisosErrors.isNotEmpty()) {
            permisosErrors.forEach(
                Consumer { hiltError: PermisosError ->
                    if (hiltError.element.isPresent) {
                        var element: Element? = hiltError.element.get()
                        if (MoreElements.isType(element)) {
                            // If the error type is a TypeElement, get a new one just in case it was thrown in a
                            // previous round we can report the correct instance. Otherwise, this leads to
                            // issues in AndroidStudio when linking an error to the proper element.
                            // TODO(bcorso): Consider only allowing TypeElement errors when delaying errors,
                            // or maybe even removing delayed errors altogether.
                            element = elements.getTypeElement(
                                MoreElements.asType(element).qualifiedName.toString()
                            )
                        }
                        messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            hiltError.message,
                            element
                        )
                    } else {
                        messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            hiltError.message
                        )
                    }
                }
            )
            permisosErrors.clear()
        }
    }

    private data class PermisosError(val message: String?, val element: Optional<Element>) {
        companion object {
            fun of(message: String?): PermisosError {
                return of(message, Optional.empty())
            }

            fun of(message: String?, element: Element): PermisosError {
                return of(message, Optional.of(element))
            }

            private fun of(message: String?, element: Optional<Element>): PermisosError {
                return PermisosError(
                    FAILURE_PREFIX + message + FAILURE_SUFFIX,
                    element
                )
            }
        }
    }

    companion object {
        private const val FAILURE_PREFIX = "[Permisos]\n"

        // Special characters to make the tag red and bold to draw attention since
        // this error can get drowned out by other errors resulting from missing
        // symbols when we can't generate code.
        private const val FAILURE_SUFFIX =
            "\n\u001b[1;31m[Permisos] Processing did not complete. See error above for details.\u001b[0m"
    }
}
