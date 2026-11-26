plugins {
    kotlin("multiplatform") version "2.4.21"
    id("me.tbsten.analysis.code.gen") version "0.1.0"
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
}

analysisCodeGen {
    arg("greeting.prefix", "Hi")
    sourceSets {
        // commonMain で 1 回だけ生成し、下流の SourceSet から使う
        commonMain { processor(project(":builder-processor")) }
        // iosMain は commonMain の宣言も処理対象にする（出力先は iosMain のまま）
        iosMain {
            processor(project(":greeting-processor"))
            processingScope.includeDependsOn()
        }
        jvmMain {
            processor(project(":greeting-processor"))
            // 実行単位の arg は、グローバルの arg より優先する
            arg("greeting.prefix", "Hello from JVM")
        }
    }
    compilations {
        // 一覧は Compilation ごとに、その Compilation の全ソースから作る
        all { processor(project(":registry-processor")) }
    }
}

dependencies {
    // dependencies { } でも足せる。名前は analysisCodeGen<SourceSet>SourceSet と analysisCodeGen<Compilation>Compilation
    "analysisCodeGenJvmMainSourceSet"(project(":deferred-processor"))
    "analysisCodeGenIosArm64MainCompilation"(project(":deferred-processor"))
}
