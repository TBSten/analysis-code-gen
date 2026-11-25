plugins {
    kotlin("jvm") version "2.4.21"
}

dependencies {
    // symbol-processing-api と ksp-bridge-api は要らなくなる
    compileOnly("me.tbsten.analysiscodegen:processor-api:0.1.0")
}
