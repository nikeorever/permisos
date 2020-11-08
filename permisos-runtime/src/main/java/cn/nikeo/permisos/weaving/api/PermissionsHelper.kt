@file:JvmName("PermissionsHelper")

package cn.nikeo.permisos.weaving.api

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

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

/**
 * A convenient method is used to safely convert the Activity annotated with @Permisos to
 * [PermissionsChecker], so that permissions can be checked and requested by Permisos
 * outside of the Activity.
 */
fun ComponentActivity.asPermissionsChecker(): PermissionsChecker {
    check(this is PermissionsChecker) {
        "$this must be annotated with the @Permisos."
    }
    return this
}

/**
 * A convenient method is used to safely convert the Fragment annotated with @Permisos to
 * [PermissionsChecker], so that permissions can be checked and requested by Permisos
 * outside of the Fragment.
 */
fun Fragment.asPermissionsChecker(): PermissionsChecker {
    check(this is PermissionsChecker) {
        "$this must be annotated with the @Permisos."
    }
    return this
}
