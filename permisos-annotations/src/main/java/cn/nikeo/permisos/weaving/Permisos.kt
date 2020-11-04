package cn.nikeo.permisos.weaving

/**
 * Denote that the current Activity or Fragment supports permission checks and requests
 * through annotations (@[RequiredPermissions])
 *
 * Note that: Only supports annotation to an Activity or Fragment.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Permisos()
