Permisos  
===========  
Easy to check and request dangerous permissions on Android.  

This is a library that allows you to check and request permissions by **annotation** or **API** on Android. It supports *Kotlin* and *Java*.

Using this library can greatly reduce the complexity of permission requests. You no longer need to write so much redundant template code. Let the annotations handle these tasks. You can shift your focus to the business code.

**_This project is currently in development and the API subject to breaking 
changes without notice._**  

## Use Annotation
Three easy steps:

 1. In order to let your `Activity` or `Fragment` have the ability to check and request permissions through annotations, please use `@Permisos` to annotate your `Activity` or `Fragment`.
```kotlin
import androidx.appcompat.app.AppCompatActivity  
import androidx.fragment.app.Fragment
import cn.nikeo.permisos.weaving.Permisos  

// For Activity
@Permisos  
class MainActivity : AppCompatActivity() {  
}

// For Fragment
@Permisos  
class MainFragment : Fragment(){  
}
```
2. Annotate `@RequiredPermissions` to a method whose **return type is void(Java) or Unit(Kotlin)**, which means that only the relevant permission is granted, the method body of this method will be executed.
```kotlin
@RequiredPermissions(  
    requestCode = 100,  
    permissions = [  
        Manifest.permission.WRITE_EXTERNAL_STORAGE  
    ]  
)  
fun writeTextToExternalStorage() {  
    if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {  
          
        val documentsFile =  
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)  
          
        if (!documentsFile.exists()) {  
            documentsFile.mkdirs()  
        }  
          
        File(documentsFile, "Permisos.txt").writeText(  
            text = """  
	        Hi, Android, I am permisos!! 
	    """.trimIndent(),  
	    charset = Charsets.UTF_8  
        )  
    }  
}
```
3. In order to receive notifications of permission denied, **please let the `Activity` or `Fragment` annotated `@Permisos` implement the `PermissionsDeniedHandler` interface**. When the requested permission has been denied by the user, the framework will call the `doOnPermissionsDenied` method of this interface. In this method, you can explain to the user that the feature is unavailable because the features requires a permission that the user has denied. At the same time, respect the user's decision. Don't link to system settings in an effort to convince the user to change their decision.
```kotlin
// For Activity
@Permisos  
class MainActivity : AppCompatActivity(), PermissionsDeniedHandler {
	override fun doOnPermissionsDenied(requestCode: Int, deniedPermissions: List<String>) {   
	// Explain to the user that the feature is unavailable because  
	// the features requires a permission that the user has denied.  
	// At the same time, respect the user's decision. Don't link to  
	// system settings in an effort to convince the user to change  
	// their decision.
	}
}

// For Fragment
@Permisos  
class MainFragment : Fragment(), PermissionsDeniedHandler {  
	override fun doOnPermissionsDenied(requestCode: Int, deniedPermissions: List<String>) {   
	// Explain to the user that the feature is unavailable because  
	// the features requires a permission that the user has denied.  
	// At the same time, respect the user's decision. Don't link to  
	// system settings in an effort to convince the user to change  
	// their decision.
	}
}
```

## Use API
Sometimes you need to use permission checking and request functions outside of `Activity` or `Fragment`, don't worry, this library can also be easily satisfied. It only takes two simple steps to complete:

 1. Same as the first step above。
 2. Pass the `Activity` or `Fragment` annotated with `@Permisos` into your external class, and then you can use `asPermissionsChecker()` to convert the `Activity` or `Fragment` to the `PermissionsChecker` interface, So you can check and request permissions by the `checkPermissions` method of `PermissionsChecker`.
 ```kotlin
 class Helper(private val mainActivity: MainActivity) {
 
     fun writeTextToExternalStorage() {
         mainActivity.asPermissionsChecker().checkPermissions(
             requestCode = 100,
             doOnAllPermissionsGranted = {
                 if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
 
                     val documentsFile =
                         Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
 
                     if (!documentsFile.exists()) {
                         documentsFile.mkdirs()
                     }
 
                     File(documentsFile, "Permisos.txt").writeText(
                         text = """
                             Hi, Android, I am permisos!!
                 	 """.trimIndent(),
                         charset = Charsets.UTF_8
                     )
                 }
             },
             shouldShowRequestPermissionRationale = { deniedPermission: List<String> ->
                 // Explain to the user that the feature is unavailable because
                 // the features requires a permission that the user has denied.
                 // At the same time, respect the user's decision. Don't link to
                 // system settings in an effort to convince the user to change
                 // their decision.
             },
             Manifest.permission.WRITE_EXTERNAL_STORAGE
         )
     }
 }
 ```

## Limit

 1. `@Permisos` only supports annotations to derived classes of `androidx.activity.ComponentActivity` or `androidx.fragment.app.Fragment`.
 2. Classes annotated by `@Permisos` cannot have type parameters.
 3. The constructor of the base class of the class annotated by `@Permisos` **cannot** contain default parameters.

## Download

#### Top-level build file
```groovy
buildscript {
    ext.permisos_version = "0.4.0"
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath "cn.nikeo.permisos:permisos-gradle:$permisos_version"
    }
}
  ```
  
#### App-module build file
```groovy
apply plugin: 'com.android.application'
apply plugin: 'kotlin-kapt'
apply plugin: 'cn.nikeo.permisos'

dependencies {
    kapt "cn.nikeo.permisos:permisos-compiler:$permisos_version"
}
```
  
#### Library-module build file
```groovy
apply plugin: 'com.android.library'
apply plugin: 'kotlin-kapt'
apply plugin: 'cn.nikeo.permisos'

dependencies {
    kapt "cn.nikeo.permisos:permisos-compiler:$permisos_version"
}
```

## License  
  
Apache License, Version 2.0, ([LICENSE](https://github.com/nikeorever/permisos/blob/trunk/LICENSE) or [https://www.apache.org/licenses/LICENSE-2.0](https://www.apache.org/licenses/LICENSE-2.0))
