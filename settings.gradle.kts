import de.fayard.refreshVersions.bootstrapRefreshVersions

rootProject.name = "permisos"

include(":permisos-gradle")
include(":permisos-annotations")
include(":permisos-runtime")
include(":permisos-compiler")

buildscript {
    repositories { gradlePluginPortal() }
    dependencies.classpath("de.fayard.refreshVersions:refreshVersions:0.9.5")
}

bootstrapRefreshVersions()
