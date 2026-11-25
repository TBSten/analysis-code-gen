plugins {
    kotlin("jvm") version "2.4.21"
}

dependencies {
    compileOnly("com.google.devtools.ksp:symbol-processing-api:2.3.14")
    testImplementation("me.tbsten.analysis.code.gen:analysis-code-gen-testing:0.1.0")
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }
