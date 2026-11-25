// ルートは設定を持たない。共通設定は build-logic の convention plugin（acg.jvm / acg.library / acg.gradle-plugin）
// 例外: docs サイトの API reference をまとめる Dokka（ADR-031）。対象は利用者に公開するモジュールだけ
plugins {
    id("acg.dokka-aggregate")
}

dependencies {
    dokka(project(":processor-api"))
    dokka(project(":gradle-plugin"))
    dokka(project(":testing"))
    dokka(project(":ksp-bridge-api"))
    dokka(project(":ksp-bridge-gradle-plugin"))
}

// 例外: フィクスチャ（fixtures/projects/*）が読むローカルリポジトリ build/fixture-repo へ、必要なモジュールを publish する（ADR-027）。
// 対象は acg.fixture-publish を適用したモジュール
tasks.register("publishFixtureDeps") {
    group = "fixtures"
    description = "フィクスチャが使うテスト用 Processor と Gradle plugin を build/fixture-repo に publish する"
    dependsOn(
        listOf(":project-model", ":gradle-plugin", ":test-processors:core", ":test-processors:ksp", ":test-processors:native")
            .filter { findProject(it) != null }
            .map { "$it:publishAllPublicationsToFixtureRepoRepository" },
    )
}
