// ルートは設定を持たない。共通設定は build-logic の convention plugin（acg.jvm / acg.library / acg.gradle-plugin）
// 例外: docs サイトの API reference をまとめる Dokka（ADR-031）。対象は利用者に公開するモジュールだけ
plugins {
    id("acg.dokka-aggregate")
}

dependencies {
    dokka(project(":analysis-code-gen-processor-api"))
    dokka(project(":analysis-code-gen-gradle-plugin"))
    dokka(project(":analysis-code-gen-testing"))
    dokka(project(":analysis-code-gen-ksp-bridge-api"))
}

// 例外: フィクスチャ（fixtures/projects/*）が読むローカルリポジトリ build/fixture-repo へ、必要なモジュールを publish する（ADR-027）。
// 対象は acg.fixture-publish を適用したモジュール
tasks.register("publishFixtureDeps") {
    group = "fixtures"
    description = "フィクスチャが使うテスト用 Processor・Gradle plugin・エンジンを build/fixture-repo に publish する"
    dependsOn(
        listOf(
            ":analysis-code-gen-project-model",
            ":analysis-code-gen-gradle-plugin",
            // Worker が解決するエンジン一式（ADR-063）。plugin は自分と同じ版の analysis-code-gen-processing-engine を取りに行く
            ":analysis-code-gen-processor-api",
            ":analysis-code-gen-engine-spi",
            ":analysis-code-gen-analysis-engine",
            ":analysis-code-gen-incremental-engine",
            ":analysis-code-gen-processing-engine",
            ":analysis-code-gen-ksp-bridge-api",
            ":analysis-code-gen-ksp-bridge",
            ":test-processors:core",
            ":test-processors:ksp",
            ":test-processors:native",
            ":real-world-ports:cream",
        )
            .filter { findProject(it) != null }
            .map { "$it:publishAllPublicationsToFixtureRepoRepository" },
    )
}
