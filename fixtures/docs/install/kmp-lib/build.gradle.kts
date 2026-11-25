plugins {
    kotlin("multiplatform") version "2.4.21"
    id("me.tbsten.analysis.code.gen") version "0.1.0"
}

kotlin {
    jvm()
    iosArm64()
}

analysisCodeGen {
    sourceSets {
        commonMain { processor(project(":hello-processor")) }
    }
}
