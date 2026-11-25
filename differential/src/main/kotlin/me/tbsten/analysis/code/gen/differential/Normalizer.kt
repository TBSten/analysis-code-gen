package me.tbsten.analysis.code.gen.differential

/**
 * 比べる前に本質的でない差を消す（pbt-differential-design.md 3.3、ADR-023）。
 *
 * 規則は [rules] の順に当てる。既定（[default]）は次の 5 つ:
 * 1. [NormalizationRules.stripRoots]: 実行ごとの絶対パス（作業ディレクトリなど）を `<ROOT>` に、区切りを `/` にする
 * 2. [NormalizationRules.newlines]: 改行を LF に、行末の空白を消し、末尾の空行を 1 つの改行にまとめる
 * 3. [NormalizationRules.embeddedValues]: 生成日時などの埋め込み値をプレースホルダにする（Processor ごとの規則を足せる）
 * 4. [NormalizationRules.diagnosticLocations]: 診断の位置を `相対パス:行` にする（列は捨てる）
 * 5. [NormalizationRules.sort]: ファイルをパスで、診断を（位置・重さ・文言）で並べる
 *
 * 消さないもの（XD-16 で確かめる）: ファイルの有無・中身の文字（行頭のインデントを含む）・出力の種類（kotlin / java / resources）・
 * 成否・診断の重さ・文言・行番号・ラウンドの区切り。
 */
public class Normalizer(public val rules: List<NormalizationRule>) {
    public fun normalize(output: RunOutput): RunOutput = rules.fold(output) { acc, rule -> rule.apply(acc) }

    public companion object {
        /** 既定の規則。[embedded] に Processor ごとの埋め込み値の規則を足せる。 */
        public fun default(embedded: List<EmbeddedValueRule> = EmbeddedValueRule.defaults): Normalizer =
            Normalizer(
                listOf(
                    NormalizationRules.stripRoots,
                    NormalizationRules.newlines,
                    NormalizationRules.embeddedValues(embedded),
                    NormalizationRules.diagnosticLocations,
                    NormalizationRules.sort,
                ),
            )
    }
}

/** 正規化の規則 1 つ。 */
public interface NormalizationRule {
    public val name: String

    public fun apply(output: RunOutput): RunOutput
}

/**
 * 生成物に埋め込まれる値（生成日時・版など）を [placeholder] に置き換える規則。
 * [processorId] が null の規則はすべての出力に、そうでなければ同じ id の Processor を含む入力だけに当てる想定（今は全部に当てる）。
 */
public data class EmbeddedValueRule(
    val name: String,
    val pattern: Regex,
    val placeholder: String,
    val processorId: String? = null,
) {
    public companion object {
        /**
         * 既定の規則。`@Generated(date = "…")`（javax / jakarta）の日時だけ。
         * 対象 Processor（ADR-019）で日時を埋め込むものは今のところこれだけ。見つけたら Processor ごとに足し、ADR-023 に追記する。
         */
        public val defaults: List<EmbeddedValueRule> =
            listOf(
                EmbeddedValueRule(
                    name = "generated-date",
                    pattern =
                        Regex(
                            """(@(?:javax\.annotation(?:\.processing)?\.|jakarta\.annotation\.)?Generated\([^)]*?date\s*=\s*)"[^"]*"""",
                        ),
                    placeholder = "$1\"<DATE>\"",
                ),
            )
    }
}

/** 既定の規則の実体。単体でも使える（XD-16 のテストが 1 つずつ確かめる）。 */
public object NormalizationRules {
    public const val ROOT: String = "<ROOT>"

    public val stripRoots: NormalizationRule =
        rule("stripRoots") { out ->
            // 長いパスから置き換える（親ディレクトリが先に消えて子が残らないように）
            val roots =
                out.roots
                    .flatMap { listOf(it, it.replace('\\', '/')) }
                    .map { it.trimEnd('/', '\\') }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sortedByDescending { it.length }

            fun String.strip(): String {
                var s = this
                for (root in roots) {
                    s = s.replace("file://$root", ROOT).replace(root, ROOT)
                }
                return s
            }
            out.copy(
                files = out.files.entries.associate { (k, v) -> k.replace('\\', '/').strip() to v.strip() },
                diagnostics =
                    out.diagnostics.map { d ->
                        d.copy(message = d.message.strip(), location = d.location?.strip()?.replace('\\', '/'))
                    },
                rounds = out.rounds?.map { r -> r.copy(generatedFiles = r.generatedFiles.map { it.replace('\\', '/').strip() }) },
                roots = emptyList(),
            )
        }

    public val newlines: NormalizationRule =
        rule("newlines") { out ->
            out.copy(
                files = out.files.mapValues { (_, v) -> normalizeText(v) },
                diagnostics = out.diagnostics.map { it.copy(message = normalizeText(it.message).trimEnd('\n')) },
            )
        }

    public fun embeddedValues(rules: List<EmbeddedValueRule>): NormalizationRule =
        rule("embeddedValues") { out ->
            out.copy(files = out.files.mapValues { (_, v) -> rules.fold(v) { acc, r -> r.pattern.replace(acc, r.placeholder) } })
        }

    /** `<ROOT>/a/b/Foo.kt:12:5` や `file:///…/Foo.kt:12` を `a/b/Foo.kt:12` にする。メッセージ先頭の位置も [DiagnosticRecord.location] に移す。 */
    public val diagnosticLocations: NormalizationRule =
        rule("diagnosticLocations") { out ->
            out.copy(
                diagnostics =
                    out.diagnostics.map { d ->
                        val fromLocation = d.location?.let(::parseLocation)
                        val head = LEADING_LOCATION.find(d.message)
                        when {
                            fromLocation != null -> d.copy(location = fromLocation)
                            head != null -> {
                                val loc = parseLocation(head.groupValues[1])
                                if (loc ==
                                    null
                                ) {
                                    d
                                } else {
                                    d.copy(location = loc, message = d.message.substring(head.range.last + 1).trimStart())
                                }
                            }
                            else -> d
                        }
                    },
            )
        }

    public val sort: NormalizationRule =
        rule("sort") { out ->
            out.copy(
                files = out.files.toSortedMap(),
                diagnostics =
                    out.diagnostics.sortedWith(
                        compareBy({ it.location ?: "" }, { it.severity }, { it.fromProcessor }, { it.message }),
                    ),
                rounds = out.rounds?.map { it.copy(generatedFiles = it.generatedFiles.sorted()) },
            )
        }

    // 位置の書き方: 「パス:行[:列]」。パスは .kt / .java で終わるもの
    private val LOCATION = Regex("""^(?:file://)?(.+?\.(?:kt|java|kts)):(\d+)(?::\d+)?$""")
    private val LEADING_LOCATION = Regex("""^((?:file://)?\S+?\.(?:kt|java|kts):\d+(?::\d+)?):""")

    internal fun parseLocation(raw: String): String? {
        val m = LOCATION.find(raw.trim()) ?: return null
        var path = m.groupValues[1].replace('\\', '/')
        if (path.startsWith("$ROOT/")) {
            // 作業ディレクトリの中の置き場所の違いは本質的でない。K（kct）は入力を <ROOT>/sources/ に、生成物を
            // <ROOT>/ksp/sources/{kotlin,java,resources}/ に置くので、最初の sources/ より後ろ（= ソースルートか生成物のキー）にする。
            // B / N のランナーは同じ形の位置を返す決まり（ADR-023）
            path = path.removePrefix("$ROOT/")
            path = if ("sources/" in path) path.substringAfter("sources/") else path
        }
        return "$path:${m.groupValues[2]}"
    }

    private fun normalizeText(text: String): String {
        if (text.isEmpty()) return text
        val lines =
            text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .split('\n')
                .map { it.trimEnd(' ', '\t') }
        val body = lines.dropLastWhile { it.isEmpty() }
        return if (body.isEmpty()) "" else body.joinToString("\n", postfix = "\n")
    }

    private fun rule(
        name: String,
        block: (RunOutput) -> RunOutput,
    ): NormalizationRule =
        object : NormalizationRule {
            override val name = name

            override fun apply(output: RunOutput) = block(output)

            override fun toString() = "NormalizationRule($name)"
        }
}
