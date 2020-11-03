package cn.nikeo.permisos.weaving.internal

import androidx.annotation.RestrictTo
import cn.nikeo.permisos.weaving.RequiredPermissions
import cn.nikeo.permisos.weaving.api.PermissionsChecker
import cn.nikeo.permisos.weaving.api.PermissionsDeniedHandler
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Aspect
class Permisos {

    @Pointcut("execution(@cn.nikeo.permisos.weaving.RequiredPermissions void *(..))")
    fun method() {
    }

    @Around("method()")
    fun checkAndExecute(joinPoint: ProceedingJoinPoint): Any? {
        val requiredPermissions = requireNotNull(
            (joinPoint.signature as MethodSignature).method.getAnnotation(
                RequiredPermissions::class.java
            )
        )

        val `this` = joinPoint.`this`

        check(`this` is PermissionsChecker)
        `this`.checkPermissions(
            requestCode = requiredPermissions.requestCode,
            doOnAllPermissionsGranted = {
                joinPoint.proceed()
            },
            shouldShowRequestPermissionRationale = { deniedPermissions ->
                // TODO Global settings?
                if (`this` is PermissionsDeniedHandler) {
                    `this`.doOnPermissionsDenied(
                        requiredPermissions.requestCode,
                        deniedPermissions
                    )
                }
            },
            *requiredPermissions.permissions
        )
        return null
    }
}
