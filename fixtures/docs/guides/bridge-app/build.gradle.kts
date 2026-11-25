plugins {
    kotlin("jvm") version "2.4.21"
    // Bridge の plugin。本体の me.tbsten.analysis.code.gen も自動で適用される
    id("me.tbsten.analysis.code.gen.ksp-bridge") version "0.1.0"
}

dependencies {
    implementation("com.squareup.moshi:moshi:1.15.2")
    // KSP の Processor と Native の Processor を同じように足せる
    analysisCodeGen("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")
    analysisCodeGen(project(":greeting-processor"))
    analysisCodeGen(project(":hybrid-processor"))
}

analysisCodeGen {
    // ksp { arg("moshi.generated", "...") } の移行先
    arg("moshi.generated", "javax.annotation.processing.Generated")
    kspBridge {
        // KMP で KSP Processor に見せるファイルの範囲。既定は Compatible（KSP2 と同じ）
        processingMode = KspProcessingMode.Compatible
    }
}
