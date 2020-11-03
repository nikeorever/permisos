package cn.nikeo.permisos.weaving.api

interface PermissionsChecker {

    /**
     * Determine whether <em>you</em> have been granted [permissions]. if [permissions] all are
     * granted, [doOnAllPermissionsGranted] will be invoked, otherwise [shouldShowRequestPermissionRationale]
     * will be invoked and the permissions that have been denied will pass into [shouldShowRequestPermissionRationale].
     */
    fun checkPermissions(
        requestCode: Int,
        doOnAllPermissionsGranted: () -> Unit,
        shouldShowRequestPermissionRationale: (List<String>) -> Unit,
        vararg permissions: String
    )
}
