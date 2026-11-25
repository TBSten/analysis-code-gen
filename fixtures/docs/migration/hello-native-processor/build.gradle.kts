plugins {
    kotlin("jvm") version "2.4.21"
}

dependencies {
    // symbol-processing-api と analysis-code-gen-ksp-bridge-api は要らなくなる
    compileOnly("me.tbsten.analysis.code.gen:analysis-code-gen-processor-api:0.1.0")
}
