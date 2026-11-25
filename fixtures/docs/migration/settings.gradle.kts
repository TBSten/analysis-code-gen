pluginManagement {
    repositories { gradlePluginPortal(); mavenCentral() }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // Analysis API と IntelliJ Platform の置き場（Maven Central に無いため）
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
        maven("https://www.jetbrains.com/intellij-repository/releases")
    }
}

rootProject.name = "migration"
include(":ksp-app", ":app")
include(":hello-ksp-processor", ":hello-hybrid-processor", ":hello-native-processor")
