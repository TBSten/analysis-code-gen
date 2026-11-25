plugins {
    id("acg.library")
    id("acg.analysis-api")
}

// ADR-012: ksp-bridge は engine-spi だけを通してエンジンに参加する。processing-engine / analysis-engine に依存しない
dependencies {
    api(project(":ksp-bridge-api"))
    implementation(project(":engine-spi"))
    implementation(project(":processor-api"))
    implementation(libs.ksp.api)
}
