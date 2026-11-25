plugins {
    kotlin("jvm") version "2.4.21"
}

dependencies {
    compileOnly("com.google.devtools.ksp:symbol-processing-api:2.3.14")
    // Hybrid API。素の KSP で動かすときもクラスが見つかるよう implementation にする
    implementation("me.tbsten.analysis.code.gen:analysis-code-gen-ksp-bridge-api:0.1.0")
}
