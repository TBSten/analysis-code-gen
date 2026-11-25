package me.tbsten.analysiscodegen.testprocessors

/**
 * フィクスチャのソースに置く `TpAnnotations.kt` の正しい中身（ADR-027）。
 *
 * テスト用 Processor は成果物としてのアノテーションを持たず、フィクスチャが自分のソースにこのファイルを置く。
 * KMP の commonMain にもそのまま置ける（JVM 専用の API を使わない）。フィクスチャ内のコピーがこれと一致することは
 * `TpAnnotationsSourceTest` が検査する。
 */
public object TpAnnotationsSource {
    public const val FILE_NAME: String = "TpAnnotations.kt"

    public val content: String =
        """
        |// テスト用 Processor（TP-*）が読むアノテーション。正は test-processors/core の TpAnnotationsSource（ADR-027）
        |package me.tbsten.analysiscodegen.tp
        |
        |/** TP-Mapper: プライマリコンストラクタの val / var から `<Class>Mapper` を作る。 */
        |@Target(AnnotationTarget.CLASS)
        |annotation class Mapped
        |
        |/** TP-Registry: 付けたクラスの FQN を集めて 1 つの Registry を作る。 */
        |@Target(AnnotationTarget.CLASS)
        |annotation class Registered
        |
        |/** TP-Chain: depth が 0 になるまで、ラウンドごとに `<origin>Chain<step>` を 1 段ずつ作る。 */
        |@Target(AnnotationTarget.CLASS)
        |annotation class Chain(val depth: Int, val step: Int = 0, val origin: String = "")
        |
        |/** TP-Actual: `expect fun name(): String` に、プラットフォーム名を返す `actual` を作る。 */
        |@Target(AnnotationTarget.FUNCTION)
        |annotation class GenerateActual
        |
        |/** TP-Body: 関数本体の呼び出し先を解決して `<Name>CallGraph` を作る（Native 版だけ）。 */
        |@Target(AnnotationTarget.FUNCTION)
        |annotation class CallGraph
        |
        |/** TP-Defer: target（クラスの FQN）が解決できるまで defer し、できたら `<Class>Resolved` を作る。 */
        |@Target(AnnotationTarget.CLASS)
        |annotation class DeferUntil(val target: String)
        |
        |/** TP-Resource: Java ソース `<Class>Java`・リソース・次のラウンドで Kotlin の `<Class>JavaRef` を作る。 */
        |@Target(AnnotationTarget.CLASS)
        |annotation class GenerateJava
        |
        |/** TP-Bad が壊れた振る舞いの対象にする宣言。 */
        |@Target(AnnotationTarget.CLASS)
        |annotation class BadTarget
        |
        |/** TP-Resource が `META-INF/services` に並べるクラスが実装する印。 */
        |interface TpResourceMarker
        |
        """.trimMargin()
}
