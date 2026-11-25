plugins {
    kotlin("jvm") version "2.4.21"
}

dependencies {
    // Analysis API も analysis-code-gen-processor-api 経由でコンパイル時に見える。実行時は analysisCodeGen が用意する
    compileOnly("me.tbsten.analysis.code.gen:analysis-code-gen-processor-api:0.1.0")
}
