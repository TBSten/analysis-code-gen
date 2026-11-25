plugins {
    kotlin("jvm") version "2.4.21"
    id("com.google.devtools.ksp") version "2.3.14"
}

dependencies {
    ksp(project(":hello-ksp-processor"))
}

ksp {
    arg("hello.verbose", "true")
}

// app と同じソースを使う（移行の前後で生成物を比べる）
kotlin.sourceSets.main { kotlin.srcDir("../app/src/main/kotlin") }
