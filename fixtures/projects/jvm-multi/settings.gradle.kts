// F-JVM-MULTI: :lib と :app のマルチモジュール。:app は :lib の公開宣言と生成物を使う（ADR-027）
pluginManagement {
    includeBuild("../build-support")
}

plugins {
    id("acg.fixture-settings")
}

rootProject.name = "fixture-jvm-multi"
include(":lib", ":app")
