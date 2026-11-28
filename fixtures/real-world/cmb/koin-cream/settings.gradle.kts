// RW-CMB: Koin Annotations と cream.kt を同じモジュールに適用するサンプル（ADR-037）。-Pacg.fixture.engine=native|bridge|ksp で切り替える
pluginManagement {
    includeBuild("../../../projects/build-support")
}

plugins {
    id("acg.fixture-settings")
}

rootProject.name = "rw-koin-cream"
