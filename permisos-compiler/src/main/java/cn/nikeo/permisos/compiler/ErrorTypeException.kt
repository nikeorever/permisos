package cn.nikeo.permisos.compiler

import javax.lang.model.element.Element

/**
 * Exception to throw when a required [Element] is or inherits from an error kind.
 *
 * Includes element to point to for the cause of the error
 */
class ErrorTypeException(message: String?, val badElement: Element) : RuntimeException(message)
