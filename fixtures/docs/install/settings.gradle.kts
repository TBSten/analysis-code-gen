pluginManagement {
    repositories { gradlePluginPortal(); mavenCentral() }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // Analysis API と IntelliJ Platform の置き場（Maven Central に無いため）
        maven("https://redirector.kotlinlang.org/maven/kotlin-ide-plugin-dependencies")
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    }
}

rootProject.name = "install"
include(":jvm-app", ":kmp-lib", ":bridge-app", ":hello-processor")
