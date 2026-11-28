plugins {
    id("acg.gradle-plugin")
    id("acg.dokka")
    id("acg.fixture-publish")
}

// ADR-012: ビルドスクリプトの classpath に載るのは analysis-code-gen-project-model と Gradle 連携だけ。エンジンと AA は Worker 側で読む
dependencies {
    implementation(project(":analysis-code-gen-project-model"))
    // KGP の型（SourceSet・Compilation）を読む。利用者のビルドにある KGP を使うので同梱しない
    compileOnly(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        // ADR-016
        create("analysisCodegen") {
            id = "me.tbsten.analysis.code.gen"
            implementationClass = "me.tbsten.analysis.code.gen.gradle.AnalysisCodegenPlugin"
        }
    }
}

// plugin が実行時に読む値（plugin の版・AA 一式の座標。ADR-063）を resource に書き出す
val buildInfoDir = layout.buildDirectory.dir("generated/acg-build-info")
// Configuration Cache に catalog の Provider を載せられないので、文字列にしてから渡す
val aaRuntime: String =
    libs.bundles.aa.runtime
        .get()
        .joinToString(",") { "${it.module.group}:${it.module.name}:${it.version}" }
val generateBuildInfo =
    tasks.register<WriteProperties>("generateBuildInfo") {
        destinationFile.set(buildInfoDir.map { it.file("me/tbsten/analysis/code/gen/gradle/acg-build-info.properties") })
        property("version", project.version.toString())
        property(
            "analysisApiVersion",
            libs.versions.analysis.api
                .get(),
        )
        property("aaRuntime", aaRuntime)
    }
sourceSets.main {
    resources.srcDir(generateBuildInfo.map { buildInfoDir.get() })
}

// AnalysisCodegenSourceSets は NamedDomainObjectContainer を委譲で実装する。継承したメンバーまで載せると
// Dokka が生成しないページへのリンク（named・withType の一部の overload）ができるので、自分で書いたメンバーだけを載せる
dokka {
    dokkaPublications.configureEach {
        suppressInheritedMembers.set(true)
    }
}
