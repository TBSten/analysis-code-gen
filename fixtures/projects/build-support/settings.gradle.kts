// フィクスチャ共通の settings plugin（acg.fixture-settings）だけを持つ included build（ADR-027）
dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "build-support"
