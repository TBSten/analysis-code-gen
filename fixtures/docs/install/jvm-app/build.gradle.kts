plugins {
    kotlin("jvm") version "2.4.21"
    id("me.tbsten.analysis.code.gen") version "0.1.0"
}

dependencies {
    analysisCodeGen(project(":hello-processor"))
}
