dependencyResolutionManagement {
    repositories {
        // Kotlin の成果物は Maven Central を先に引く（plugin portal の中継で BOM が見つからないことがあった）
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
