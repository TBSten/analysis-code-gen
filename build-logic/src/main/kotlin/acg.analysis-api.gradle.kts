// Analysis API / IntelliJ Platform を compileOnly で使うモジュール（ADR-011・012）。
// AA は実行時に利用者の Kotlin と同じ版を Worker で読むため、成果物には同梱しない。
// analysis-code-gen-processor-api など公開 API に AA の型が出るモジュールは、この上で compileOnlyApi を自分で宣言する。
plugins {
    id("acg.jvm")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
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
        "compileOnly"(libs.findLibrary(alias).get()) { isTransitive = false }
        "testImplementation"(libs.findLibrary(alias).get()) { isTransitive = false }
    }
}
