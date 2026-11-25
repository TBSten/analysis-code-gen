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

rootProject.name = "install"
include(":jvm-app", ":kmp-lib", ":bridge-app", ":hello-processor")
