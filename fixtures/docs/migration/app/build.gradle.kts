plugins {
    kotlin("jvm") version "2.4.21"
    id("me.tbsten.analysiscodegen.ksp-bridge") version "0.1.0"
    application
}

dependencies {
    analysisCodeGen(project(":hello-ksp-processor"))
}

analysisCodeGen {
    arg("hello.verbose", "true")
}

application { mainClass = "com.example.MainKt" }
