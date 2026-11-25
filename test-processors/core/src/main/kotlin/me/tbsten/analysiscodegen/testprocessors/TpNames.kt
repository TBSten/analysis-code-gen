package me.tbsten.analysiscodegen.testprocessors

/**
 * テスト用 Processor の識別子。オプション `tp.processors` に書く名前（ADR-027）。
 *
 * @property nativeOnly KSP 版が無い（AA でしかできない解析を使う）もの。
 */
public enum class TpId(public val id: String, public val nativeOnly: Boolean = false) {
    Mapper("Mapper"),
    Registry("Registry"),
    Chain("Chain"),
    Actual("Actual"),
    Body("Body", nativeOnly = true),
    Defer("Defer"),
    Bad("Bad"),
    Resource("Resource"),
    Probe("Probe"),
    ;

    /** ビルド出力や診断に出す名前。例: `TP-Mapper`。 */
    public val displayName: String get() = "TP-$id"

    public companion object {
        public fun fromId(id: String): TpId =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
                ?: throw IllegalArgumentException("未知のテスト用 Processor: $id（${entries.joinToString { it.id }} のどれか）")

        /** `tp.processors` を指定しないときに有効になるもの。TP-Bad と TP-Probe は明示したときだけ動く。 */
        public val defaults: Set<TpId> = setOf(Mapper, Registry, Chain, Actual, Body, Defer, Resource)
    }
}

/** フィクスチャのソースに置くアノテーションの FQN。宣言の正は [TpAnnotationsSource]。 */
public object TpAnnotations {
    public const val PACKAGE: String = "me.tbsten.analysiscodegen.tp"
    public const val MAPPED: String = "$PACKAGE.Mapped"
    public const val REGISTERED: String = "$PACKAGE.Registered"
    public const val CHAIN: String = "$PACKAGE.Chain"
    public const val GENERATE_ACTUAL: String = "$PACKAGE.GenerateActual"
    public const val CALL_GRAPH: String = "$PACKAGE.CallGraph"
    public const val DEFER_UNTIL: String = "$PACKAGE.DeferUntil"
    public const val GENERATE_JAVA: String = "$PACKAGE.GenerateJava"
    public const val BAD_TARGET: String = "$PACKAGE.BadTarget"
    public const val RESOURCE_MARKER: String = "$PACKAGE.TpResourceMarker"
}

/** テスト用 Processor が読むオプションのキー。 */
public object TpOptionKeys {
    /** 有効にする Processor（カンマ区切り）。例: `Mapper,Registry`。`all` で TP-Bad 以外の全部。 */
    public const val PROCESSORS: String = "tp.processors"
    public const val REGISTRY_PACKAGE: String = "tp.registry.package"
    public const val REGISTRY_NAME: String = "tp.registry.name"

    /** TP-Bad の壊れ方（カンマ区切り）。値は [BadMode.id]。 */
    public const val BAD_MODES: String = "tp.bad.modes"

    /** TP-Bad の `infinite-rounds` を KSP2 で動かしたときの安全弁（KSP2 には最大ラウンドが無い）。既定 1000。 */
    public const val BAD_SAFETY_LIMIT: String = "tp.bad.safetyLimit"

    /** TP-Probe が観測結果を JSON Lines で追記するファイルの絶対パス。 */
    public const val PROBE_OUT: String = "tp.probe.out"

    /** TP-Probe がラウンド 1 で出すログ。`warn:文;info:文;error:文` の形。 */
    public const val PROBE_LOG: String = "tp.probe.log"
}
