package cn.nikeo.permisos.compiler

import com.google.common.collect.ImmutableList
import javax.lang.model.element.Element

/**
 * Exception to throw when input code has caused an error.
 * Includes elements to point to for the cause of the error
 */
class BadInputException : RuntimeException {
    val badElements: ImmutableList<Element>

    constructor(message: String?, badElement: Element) : super(message) {
        badElements = ImmutableList.of(badElement)
    }

    constructor(message: String?, badElements: Iterable<Element>) : super(message) {
        this.badElements = ImmutableList.copyOf(badElements)
    }
}
