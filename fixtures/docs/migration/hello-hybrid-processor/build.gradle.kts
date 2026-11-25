plugins {
    kotlin("jvm") version "2.4.21"
}

dependencies {
    compileOnly("com.google.devtools.ksp:symbol-processing-api:2.3.14")
    // withAnalysis など。素の KSP でも読み込めるよう Processor に同梱する
    implementation("me.tbsten.analysis.code.gen:analysis-code-gen-ksp-bridge-api:0.1.0")
}
