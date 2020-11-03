package cn.nikeo.permisos.weaving

/**
 * TODO
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiredPermissions(
    val requestCode: Int,
    val permissions: Array<String>
)
