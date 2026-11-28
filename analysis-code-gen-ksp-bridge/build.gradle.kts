import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("acg.library")
    id("acg.analysis-api")
}

// ADR-012: analysis-code-gen-ksp-bridge は analysis-code-gen-engine-spi だけを通してエンジンに参加する。
// analysis-code-gen-processing-engine / analysis-code-gen-analysis-engine に依存しない
dependencies {
    api(project(":analysis-code-gen-ksp-bridge-api"))
    implementation(project(":analysis-code-gen-engine-spi"))
    implementation(project(":analysis-code-gen-processor-api"))
    implementation(libs.ksp.api)
    // KSPConfig と IncrementalContextLoggingOptions は移植せず KSP の公開成果物を使う（ADR-062）
    implementation(libs.ksp.common.deps)

    // 写したコードが触れる AA の実装側（FIR・低レベル API・light classes など）。実行時はエンジンが AA 一式を読む（ADR-062）
    val aaVersion =
        libs.versions.analysis.api
            .get()
    val ijVersion =
        libs.versions.aa.intellij
            .get()
    listOf(
        "analysis-api-k2-for-ide",
        "low-level-api-fir-for-ide",
        "analysis-api-platform-interface-for-ide",
        "symbol-light-classes-for-ide",
        "analysis-api-standalone-for-ide",
        "analysis-api-impl-base-for-ide",
        "kotlin-compiler-fir-for-ide",
        "kotlin-compiler-fe10-for-ide",
        "kotlin-compiler-ir-for-ide",
    ).forEach { compileOnly("org.jetbrains.kotlin:$it:$aaVersion") { isTransitive = false } }
    listOf(
        "com.jetbrains.intellij.java:java-psi-impl",
        "com.jetbrains.intellij.java:java-frontback-psi-impl",
        "com.jetbrains.intellij.platform:util-coroutines",
    ).forEach { compileOnly("$it:$ijVersion") { isTransitive = false } }
    // バイナリの宣言の読み取り（BinaryUtils）。AA の実行時の依存に含まれる
    compileOnly("org.jetbrains.intellij.deps:asm-all:9.0") { isTransitive = false }
}

kotlin {
    // 内部モジュール。KSP2 から写したコードは上流との差分を小さく保つため可視性を書き足さない（ADR-062）
    explicitApi = ExplicitApiMode.Disabled
    compilerOptions {
        // 写したコードの警告は上流の書き方のまま残す
        allWarningsAsErrors.set(false)
        freeCompilerArgs.addAll("-Xcontext-parameters", "-Xsuppress-version-warnings")
        optIn.addAll(
            "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
            "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
            "org.jetbrains.kotlin.analysis.api.KaNonPublicApi",
            "org.jetbrains.kotlin.analysis.api.KaIdeApi",
            "org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
            "org.jetbrains.kotlin.analysis.api.permissions.KaAllowProhibitedAnalyzeFromWriteAction",
            "com.google.devtools.ksp.KspExperimental",
        )
    }
}

// KSP2 から写したコード（common / impl と InternalKSPException）は整形しない（ADR-062）
ktlint {
    filter {
        exclude { element ->
            val path = element.file.invariantSeparatorsPath
            "/ksp/bridge/common/" in path || "/ksp/bridge/impl/" in path || path.endsWith("/ksp/bridge/InternalKSPException.kt")
        }
    }
}
