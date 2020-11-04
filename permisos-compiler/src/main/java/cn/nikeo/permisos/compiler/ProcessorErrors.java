package cn.nikeo.permisos.compiler;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;

/**
 * Static helper methods for throwing errors during code generation.
 */
public final class ProcessorErrors {
    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>e.g. checkState(foo.isABar(), "Failed because of %s is not a bar", foo);
     *
     * @param expression           a boolean expression
     * @param badElement           the element that was at fault
     * @param errorMessageTemplate a template for the exception message should the check fail. The
     *                             message is formed by replacing each {@code %s} placeholder in the template with an
     *                             argument. These are matched by position - the first {@code %s} gets {@code
     *                             errorMessageArgs[0]}, etc. Unmatched arguments will be appended to the formatted message in
     *                             square braces. Unmatched placeholders will be left as-is.
     * @param errorMessageArgs     the arguments to be substituted into the message template. Arguments
     *                             are converted to strings using {@link String#valueOf(Object)}.
     * @throws BadInputException    if {@code expression} is false
     * @throws NullPointerException if the check fails and either {@code errorMessageTemplate} or
     *                              {@code errorMessageArgs} is null (don't let this happen)
     */
    @FormatMethod
    public static void checkState(
            boolean expression,
            Element badElement,
            @Nullable @FormatString String errorMessageTemplate,
            @Nullable Object... errorMessageArgs) {
        Preconditions.checkNotNull(badElement);
        if (!expression) {
            throw new BadInputException(
                    String.format(
                            Optional.ofNullable(errorMessageTemplate).orElse(""),
                            errorMessageArgs
                    ), badElement);
        }
    }

    private ProcessorErrors() {
    }
}
