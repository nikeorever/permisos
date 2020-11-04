@file:JvmName("PermissionsHelper")

package cn.nikeo.permisos.weaving.api

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Enumerate all permission types.
 */
enum class PermissionType {
    /**
     * Represents the permission that has been granted.
     */
    GRANTED,

    /**
     * Represents the permission that has been requested but denied by the user.
     */
    DENIED,

    /**
     * Represents the permission that has not been requested.
     */
    NOT_REQUESTED_YET
}

/**
 * Group [permissions] by [PermissionType].
 */
fun groupPermissions(
    activity: Activity,
    vararg permissions: String
): Map<PermissionType, List<String>> {
    return permissions.groupBy { permission ->
        when {
            ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> PermissionType.GRANTED
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                permission
            ) -> PermissionType.DENIED
            else -> PermissionType.NOT_REQUESTED_YET
        }
    }
}
