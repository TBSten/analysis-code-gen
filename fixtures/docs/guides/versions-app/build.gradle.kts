plugins {
    // 利用者の Kotlin。analysisCodeGen は同じ版の Analysis API を読み込む
    kotlin("jvm") version "2.4.21"
    id("me.tbsten.analysiscodegen") version "0.1.0"
}

dependencies {
    analysisCodeGen(project(":greeting-processor"))
}

analysisCodeGen {
    // 同じ版の Analysis API が手に入らないとき（dev 版の Kotlin など）だけ指定する
    analysisApiVersion = "2.4.20"
}
