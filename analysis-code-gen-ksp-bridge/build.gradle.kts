plugins {
    id("acg.library")
    id("acg.analysis-api")
    id("acg.fixture-publish")
}

// ADR-012: analysis-code-gen-ksp-bridge は analysis-code-gen-engine-spi だけを通してエンジンに参加する。
// analysis-code-gen-processing-engine / analysis-code-gen-analysis-engine に依存しない
dependencies {
    api(project(":analysis-code-gen-ksp-bridge-api"))
    implementation(project(":analysis-code-gen-engine-spi"))
    implementation(project(":analysis-code-gen-processor-api"))
    implementation(libs.ksp.api)
}
