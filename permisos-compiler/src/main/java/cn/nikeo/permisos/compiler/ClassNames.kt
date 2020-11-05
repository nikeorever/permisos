package cn.nikeo.permisos.compiler

import com.squareup.kotlinpoet.ClassName

/**
 * Holder for commonly used class names.
 */
object ClassNames {

    val PERMISSIONS_CHECKER: ClassName =
        ClassName("cn.nikeo.permisos.weaving.api", "PermissionsChecker")
    val PERMISSION_TYPE: ClassName =
        ClassName("cn.nikeo.permisos.weaving.api", "PermissionType")
    val GROUP_PERMISSIONS: ClassName =
        ClassName("cn.nikeo.permisos.weaving.api", "groupPermissions")
    val PERMISOS: ClassName = ClassName("cn.nikeo.permisos.weaving", "Permisos")
    val REQUIRED_PERMISSIONS: ClassName =
        ClassName("cn.nikeo.permisos.weaving", "RequiredPermissions")

    // Kotlin-specific class names
    val KOTLIN_METADATA: ClassName = ClassName("kotlin", "Metadata")
}
