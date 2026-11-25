plugins {
    kotlin("jvm") version "2.4.21"
    id("me.tbsten.analysis.code.gen.ksp-bridge") version "0.1.0"
}

dependencies {
    implementation("com.squareup.moshi:moshi:1.15.2")
    analysisCodeGen("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")
}

analysisCodeGen {
    arg("moshi.generated", "javax.annotation.Generated")
}
