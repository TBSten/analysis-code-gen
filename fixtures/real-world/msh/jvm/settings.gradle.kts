// RW-MSH: Moshi codegen の JVM サンプル（ADR-037）。-Pacg.fixture.engine=native|bridge|ksp で実行系を切り替える
pluginManagement {
    includeBuild("../../../projects/build-support")
}

plugins {
    id("acg.fixture-settings")
}

rootProject.name = "rw-moshi-jvm"
