plugins {
    id("acg.library")
    id("acg.analysis-api")
    id("acg.dokka")
    id("acg.fixture-publish")
}

// 公開 API に AA の型（KtFile・KaSession など）が出るので、利用者が Processor をコンパイルするときに AA が見える必要がある。
// 実行時は analysisCodeGen が利用者の Kotlin と同じ版の AA を Worker に載せる（ADR-011・063）ので、実行時の依存には入れない。
// そのため AA の一式は apiElements（コンパイル用の variant）にだけ載せて公開する（ACG-67、ADR-067）
val aaCompileApi: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = false
}
configurations.named("apiElements") { extendsFrom(aaCompileApi) }

// acg.analysis-api の compileOnly と同じ一覧
val catalog = versionCatalogs.named("libs")
listOf(
    "aa-api",
    "aa-compiler-common",
    "ij-core",
    "ij-core-impl",
    "ij-util",
    "ij-util-base",
    "ij-util-rt",
    "ij-extensions",
    "ij-java-psi",
    "ij-java-frontback-psi",
).forEach { alias ->
    dependencies { aaCompileApi(catalog.findLibrary(alias).get()) { isTransitive = false } }
}
