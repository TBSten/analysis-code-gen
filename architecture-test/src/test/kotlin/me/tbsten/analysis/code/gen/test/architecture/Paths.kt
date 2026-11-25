package me.tbsten.analysis.code.gen.test.architecture

/** Kotlin のパッケージの根に当たるディレクトリ（ADR-044） */
const val PACKAGE_DIR = "me/tbsten/analysis/code/gen"

/** モジュールの main の Kotlin ソースのうち、パッケージの根より下 */
fun mainKotlin(module: String, subPackage: String): String = "$module/src/main/kotlin/$PACKAGE_DIR/$subPackage"

/** モジュールの test の Kotlin ソースのうち、パッケージの根より下 */
fun testKotlin(module: String, subPackage: String): String = "$module/src/test/kotlin/$PACKAGE_DIR/$subPackage"

/**
 * e2e-tests・engine-tests の観点別パッケージ。テストケース一覧の ID の接頭辞を小文字にしたもの（ADR-038）。
 * 新しい観点のテストを足すときは、ここにも足す
 */
val CASE_ASPECTS = listOf(
    "ana", "api", "bin", "binc", "bkmp", "brg", "cch", "cli", "def", "det", "doc", "fail", "gcc", "grd", "hyb",
    "inc", "kmp", "lic", "mem", "mix", "out", "pbt", "perf", "rnd", "rw", "trm", "tst", "ver", "xd",
)
