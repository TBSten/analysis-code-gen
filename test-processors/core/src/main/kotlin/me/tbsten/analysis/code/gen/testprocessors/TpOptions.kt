package me.tbsten.analysis.code.gen.testprocessors

/** TP-Bad の壊れ方（ADR-027）。 */
public enum class BadMode(public val id: String) {
    /** 毎ラウンド新しいファイルを作り続ける（TRM-01）。 */
    InfiniteRounds("infinite-rounds"),

    /** `@BadTarget` を毎ラウンド defer し続け、生成はしない（DEF-04）。 */
    StuckDefer("stuck-defer"),

    /** `@BadTarget` を毎ラウンド defer し続け、毎ラウンド新しいファイルも作る（TRM-02）。 */
    StuckDeferWithProgress("stuck-defer-progress"),

    /** TP-Mapper と同じパス（`<Class>Mapper.kt`）に書く（TRM-03）。 */
    Collide("collide"),

    /** 同じラウンドで同じファイルを 2 回作る（TRM-04）。 */
    Duplicate("duplicate"),

    /** ファイルを 1 つ作ってから例外を投げる（FAIL-01・API-11）。 */
    Throw("throw"),

    /** 出力ルートの外（`../`）へ書こうとする（API-06）。 */
    WriteOutside("write-outside"),

    /** `@BadTarget` の宣言を指して `logger.error` を呼ぶ（API-04）。 */
    LogError("log-error"),
    ;

    public companion object {
        public fun fromId(id: String): BadMode =
            entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("未知の TP-Bad モード: $id（${entries.joinToString { it.id }} のどれか）")
    }
}

/** TP-Probe のログ指定の 1 件。 */
public data class ProbeLog(public val level: String, public val message: String)

/** テスト用 Processor のオプションを読んだもの。KSP 版・Native 版で同じものを使う。 */
public class TpOptions(public val raw: Map<String, String>) {
    public val enabled: Set<TpId> = parseEnabled(raw[TpOptionKeys.PROCESSORS])

    public val registryPackage: String = raw[TpOptionKeys.REGISTRY_PACKAGE] ?: "me.tbsten.analysis.code.gen.tp.generated"

    public val registryName: String = raw[TpOptionKeys.REGISTRY_NAME] ?: "TpRegistry"

    public val badModes: Set<BadMode> =
        raw[TpOptionKeys.BAD_MODES].splitList().map(BadMode::fromId).toSet()

    public val badSafetyLimit: Int = raw[TpOptionKeys.BAD_SAFETY_LIMIT]?.toInt() ?: 1000

    public val probeOut: String? = raw[TpOptionKeys.PROBE_OUT]

    public val probeLogs: List<ProbeLog> =
        raw[TpOptionKeys.PROBE_LOG]
            ?.split(';')
            ?.filter { it.isNotBlank() }
            ?.map { entry ->
                val level = entry.substringBefore(':').trim()
                require(level in setOf("logging", "info", "warn", "error")) { "tp.probe.log のレベルが不正: $entry" }
                ProbeLog(level, entry.substringAfter(':'))
            }.orEmpty()

    public fun isEnabled(id: TpId): Boolean = id in enabled

    private companion object {
        fun parseEnabled(value: String?): Set<TpId> {
            if (value == null) return TpId.defaults
            val items = value.splitList()
            if (items == listOf("all")) return TpId.entries.toSet() - TpId.Bad
            return items.map(TpId::fromId).toSet()
        }

        fun String?.splitList(): List<String> =
            this
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
    }
}
