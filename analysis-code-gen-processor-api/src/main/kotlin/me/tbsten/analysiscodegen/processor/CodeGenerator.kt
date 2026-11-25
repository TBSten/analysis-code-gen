package me.tbsten.analysiscodegen.processor

/**
 * 管理された生成 API（ADR-013・014・015）。
 *
 * - 出力ルートの外（`..`、絶対パス）、他の実行単位の出力、ソースへの書き込みは呼び出し時に [IllegalArgumentException]
 * - 書いた内容はラウンドの終わりに検証され、成功すれば次のラウンドから解析対象になる
 * - 文字コードは UTF-8、改行は LF に正規化される（API-13）
 */
public interface CodeGenerator {
    /** `<kotlin 出力ルート>/<packageName を / 区切り>/<fileName>.kt` を生成する。 */
    public fun generateKotlin(
        packageName: String,
        fileName: String,
        dependencies: Dependencies,
        content: String,
    )

    /** `<java 出力ルート>/<packageName を / 区切り>/<fileName>.java` を生成する。 */
    public fun generateJava(
        packageName: String,
        fileName: String,
        dependencies: Dependencies,
        content: String,
    )

    /** `<resources 出力ルート>/<relativePath>` を生成する。例: `META-INF/services/foo.Bar`。 */
    public fun generateResource(
        relativePath: String,
        dependencies: Dependencies,
        content: ByteArray,
    )
}
