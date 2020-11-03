package cn.nikeo.permisos.weaving.api

interface PermissionsDeniedHandler {

    /**
     * Called when the [deniedPermissions] has been denied by the user. [deniedPermissions] must be
     * not empty.
     *
     * @see cn.nikeo.permisos.weaving.RequiredPermissions
     */
    fun doOnPermissionsDenied(requestCode: Int, deniedPermissions: List<String>)
}
