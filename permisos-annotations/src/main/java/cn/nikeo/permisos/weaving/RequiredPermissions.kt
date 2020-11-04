package cn.nikeo.permisos.weaving

/**
 * Denote that the current Function requires permission check and request.
 * When the permission is granted, the method body of this method will be executed.
 *
 * [requestCode] indicates each permission check and request task.
 * If there are multiple tasks in an Activity or Fragment, the requestCode must be unique.
 * [permissions] indicates the permissions to be checked and requested.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiredPermissions(
    val requestCode: Int,
    val permissions: Array<String>
)
