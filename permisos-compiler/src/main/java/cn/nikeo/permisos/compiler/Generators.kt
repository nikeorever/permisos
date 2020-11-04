package cn.nikeo.permisos.compiler

import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.INT_ARRAY
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.WildcardTypeName

/**
 * Helper class for writing Permisos generators.
 */
object Generators {

    /**
     * Add property - requireActivity
     *
     * For Activity:
     * ```kotlin
     * private val requireActivity: Activity get() = this
     * ```
     * For Fragment:
     * ```kotlin
     * private val requireActivity: Activity get() = requireActivity()
     * ```
     */
    fun addRequireActivityProperty(type: AndroidType, builder: TypeSpec.Builder) {
        builder.addProperty(
            PropertySpec.builder(
                name = "requireActivity",
                type = AndroidClassNames.ACTIVITY,
                modifiers = arrayOf(KModifier.PRIVATE)
            ).mutable(false)
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement(
                            "return %L",
                            if (type == AndroidType.ACTIVITY) "this" else "requireActivity()"
                        )
                        .build()
                )
                .build()
        )
    }

    /**
     * Add superInterface - PermissionsChecker
     */
    fun addPermissionsCheckerAndImplementation(builder: TypeSpec.Builder) {
        builder.addSuperinterface(ClassNames.PERMISSIONS_CHECKER)
        builder.addFunction(
            FunSpec.builder("checkPermissions")
                .addModifiers(KModifier.OVERRIDE, KModifier.PUBLIC)
                .addParameter(
                    name = "requestCode",
                    type = INT,
                )
                .addParameter(
                    name = "doOnAllPermissionsGranted",
                    type = LambdaTypeName.get(returnType = UNIT)
                )
                .addParameter(
                    name = "shouldShowRequestPermissionRationale",
                    type = LambdaTypeName.get(
                        parameters = arrayOf(LIST.parameterizedBy(STRING)),
                        returnType = UNIT
                    )
                )
                .addParameter(
                    ParameterSpec.builder("permissions", STRING)
                        .addModifiers(KModifier.VARARG)
                        .build()
                )
                .addCode(checkPermissionsFunCode(builder))
                .returns(UNIT)
                .build()
        )
    }

    private fun checkPermissionsFunCode(builder: TypeSpec.Builder): CodeBlock {
        // private var permissionConfiguration: PermissionConfiguration? = null
        builder.addProperty(
            PropertySpec.builder(
                name = "permissionConfiguration",
                type = ClassName.bestGuess("PermissionConfiguration").copy(nullable = true),
                modifiers = arrayOf(KModifier.PRIVATE)
            ).mutable(true).initializer("null").build()
        )
        return CodeBlock.of(
            """
        val grouped = groupPermissions(requireActivity, *permissions)
        val grantedPermissions = grouped[PermissionType.GRANTED]
        val deniedPermissions = grouped[PermissionType.DENIED]
        val notRequestedYetPermissions = grouped[PermissionType.NOT_REQUESTED_YET]

        if (grantedPermissions?.size == permissions.size) {
            doOnAllPermissionsGranted()
            return
        }

        if (notRequestedYetPermissions.isNullOrEmpty()) {

            check(!deniedPermissions.isNullOrEmpty())
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            shouldShowRequestPermissionRationale(deniedPermissions)
        } else {

            val task = PermissionConfiguration(
                requestCode,
                doOnAllPermissionsGranted,
                shouldShowRequestPermissionRationale
            )
            if (!deniedPermissions.isNullOrEmpty()) {
                task.deniedPermissions.addAll(deniedPermissions)
            }

            permissionConfiguration = task

            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            // You can directly ask for the permission.
            ActivityCompat.requestPermissions(
                requireActivity,
                notRequestedYetPermissions.toTypedArray(),
                requestCode
            )
        }
            """.trimIndent()
        )
    }

    /**
     * Add inner class - PermissionConfiguration
     *
     * ```kotlin
     * private class PermissionConfiguration(
     *   val requestCode: Int,
     *   val doOnAllPermissionsGranted: () -> Unit,
     *   val shouldShowRequestPermissionRationale: (List<String>) -> Unit) {
     *     val deniedPermissions: MutableList<String> by lazy { mutableListOf() }
     * }
     * ```
     */
    fun addPermissionConfiguration(builder: TypeSpec.Builder) {
        builder.addType(
            TypeSpec.classBuilder("PermissionConfiguration")
                .addModifiers(KModifier.PRIVATE)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter(
                            name = "requestCode",
                            type = INT
                        )
                        .addParameter(
                            name = "doOnAllPermissionsGranted",
                            type = LambdaTypeName.get(returnType = UNIT)
                        )
                        .addParameter(
                            name = "shouldShowRequestPermissionRationale",
                            type = LambdaTypeName.get(
                                parameters = arrayOf(LIST.parameterizedBy(STRING)),
                                returnType = UNIT
                            )
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(
                        name = "requestCode",
                        type = INT,
                        modifiers = arrayOf(KModifier.PUBLIC)
                    ).initializer("requestCode").build()
                )
                .addProperty(
                    PropertySpec.builder(
                        name = "doOnAllPermissionsGranted",
                        type = LambdaTypeName.get(returnType = UNIT),
                        modifiers = arrayOf(KModifier.PUBLIC)
                    ).initializer("doOnAllPermissionsGranted").build()
                )
                .addProperty(
                    PropertySpec.builder(
                        name = "shouldShowRequestPermissionRationale",
                        type = LambdaTypeName.get(
                            parameters = arrayOf(LIST.parameterizedBy(STRING)),
                            returnType = UNIT
                        ),
                        modifiers = arrayOf(KModifier.PUBLIC)
                    ).initializer("shouldShowRequestPermissionRationale").build()
                )
                .addProperty(
                    PropertySpec.builder(
                        name = "deniedPermissions",
                        type = MUTABLE_LIST.parameterizedBy(STRING),
                        modifiers = arrayOf(KModifier.PUBLIC)
                    ).mutable(mutable = false).delegate("lazy { mutableListOf() }").build()
                )
                .build()
        )
    }

    /**
     * onRequestPermissionsResult
     */
    fun addOnRequestPermissionsResultFunOverride(builder: TypeSpec.Builder) {
        builder.addFunction(
            FunSpec.builder("onRequestPermissionsResult")
                .addModifiers(KModifier.OVERRIDE, KModifier.PUBLIC)
                .addAnnotation(AndroidClassNames.CALL_SUPER)
                .addParameter(
                    name = "requestCode",
                    type = INT,
                )
                .addParameter(
                    name = "permissions",
                    type = ARRAY.parameterizedBy(WildcardTypeName.producerOf(STRING))
                )
                .addParameter(
                    name = "grantResults",
                    type = INT_ARRAY
                )
                .addCode(onRequestPermissionsResultFunCode())
                .returns(UNIT)
                .build()
        )
    }

    private fun onRequestPermissionsResultFunCode(): CodeBlock {
        return CodeBlock.of(
            """
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val task = permissionConfiguration
        if (task != null) {
            when (requestCode) {
                task.requestCode -> {
                    // If request is cancelled, the result arrays are empty.
                    val deniedPermissions = if (grantResults.isNotEmpty()) {
                        permissions.withIndex().filter {
                            grantResults[it.index] == PackageManager.PERMISSION_DENIED
                        }.map(IndexedValue<String>::value)
                    } else {
                        permissions.toList()
                    }

                    if (!deniedPermissions.isNullOrEmpty()) {
                        task.deniedPermissions.addAll(deniedPermissions)
                    }

                    if (task.deniedPermissions.isEmpty()) {
                        // All permission is granted. Continue the action or workflow
                        // in your app.
                        task.doOnAllPermissionsGranted()
                    } else {
                        // Explain to the user that the feature is unavailable because
                        // the features requires a permission that the user has denied.
                        // At the same time, respect the user's decision. Don't link to
                        // system settings in an effort to convince the user to change
                        // their decision.
                        task.shouldShowRequestPermissionRationale(task.deniedPermissions)
                    }
                    return
                }

                // Add other 'when' lines to check for other
                // permissions this app might request.
                else -> {
                    // Ignore all other requests.
                }
            }
            permissionConfiguration = null
        }
            """.trimIndent()
        )
    }
}
