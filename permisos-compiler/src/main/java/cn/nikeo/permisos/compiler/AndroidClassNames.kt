package cn.nikeo.permisos.compiler

import com.squareup.kotlinpoet.ClassName

object AndroidClassNames {
    val PERMISOS: ClassName = ClassName("cn.nikeo.permisos.weaving", "Permisos")
    val ACTIVITY: ClassName = ClassName("android.app", "Activity")
    val COMPONENT_ACTIVITY: ClassName = ClassName("androidx.activity", "ComponentActivity")
    val FRAGMENT: ClassName = ClassName("androidx.fragment.app", "Fragment")
    val PACKAGE_MANAGER: ClassName = ClassName("android.content.pm", "PackageManager")
    val CALL_SUPER: ClassName = ClassName("androidx.annotation", "CallSuper")
    val ACTIVITY_COMPAT: ClassName = ClassName("androidx.core.app", "ActivityCompat")
}
