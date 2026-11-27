package me.tbsten.analysis.code.gen.testfoundation

import io.kotest.core.NamedTag
import io.kotest.core.Tag
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.test.TestScope

/**
 * 全テストの基底クラス（kotest FreeSpec）。種別ごとのサブクラス（[E2ESpec] など）を継承して使う。
 *
 * ```
 * class IncrementalAddFileTest : E2ESpec({
 *     case("INC-01", "ファイルを 1 つ足すと、そのファイルの生成物だけが増える", pending = PendingCase(Milestone.M1)) {
 *         // 期待する結果（アサーション）まで書く
 *     }
 * })
 * ```
 *
 * 決まり（ADR-030）:
 * - [case] はトップレベルにだけ書く。spec を作っただけで宣言が集まり、テストを実行せずに整合チェックができるようにするため
 * - spec の本体（`case` の外）で重い処理をしない。整合チェックが spec を生成するため
 * - ケース ID の付かない補助テストは素の FreeSpec の書き方（`"名前" { }`）で書ける。進捗には数えない
 */
abstract class AcgSpec(
    val kind: CaseKind,
    body: AcgSpec.() -> Unit = {},
) : FreeSpec() {
    private val declarationsInternal = mutableListOf<CaseDeclaration>()

    /** この spec が宣言したケース。整合チェック（`check` パッケージ）が読む。 */
    val caseDeclarations: List<CaseDeclaration> get() = declarationsInternal.toList()

    init {
        body()
    }

    /**
     * ケース ID 付きのテストを宣言する。
     *
     * テスト名は `[ID] title` になり、kotest Tag に ID・種別（`kind-E` など）・保留なら `pending` とマイルストーンが付く。
     * [pending] を渡すと skip される（`-Pacg.runPending=true` のときだけ中身を実行し、結果を記録する）。
     */
    fun case(
        id: String,
        title: String,
        pending: PendingCase? = null,
        test: suspend TestScope.() -> Unit,
    ) {
        require(CaseDeclaration.ID_PATTERN.matches(id)) { "ケース ID の書式が違う: '$id'（例: INC-14, RW-ALL-I1, INC-14a, KSPT-Hello）" }
        require(title.isNotBlank()) { "ケース $id のタイトルが空" }
        val declaration = CaseDeclaration(id, title, kind, pending, this::class.java.name)
        declarationsInternal += declaration

        val tags =
            buildSet<Tag> {
                add(NamedTag(id))
                add(NamedTag("kind-${kind.letter}"))
                if (pending != null) {
                    add(NamedTag("pending"))
                    add(NamedTag("pending-${pending.milestone.name}"))
                }
            }
        val runPending = CaseRuntime.runPending
        val shard = if (runPending) CaseRuntime.pendingShard else null
        val enabled =
            when {
                // 分割して保留ケースだけを流す夜間の job（ADR-057）。有効なケースは別の job が受け持つ
                shard != null -> pending != null && CaseRuntime.inShard(id, shard)
                else -> pending == null || runPending
            }
        val body: suspend TestScope.() -> Unit =
            if (pending == null) {
                test
            } else {
                {
                    val start = System.nanoTime()
                    val error = CaseRuntime.runPendingBody(this, test)
                    CaseRuntime.recordPendingRun(declaration, error, (System.nanoTime() - start) / 1_000_000)
                }
            }
        declaration.testName.config(enabled = enabled, tags = tags, test = body)
    }
}

/** U: クラス単体のテスト。 */
abstract class UnitSpec(body: AcgSpec.() -> Unit = {}) : AcgSpec(CaseKind.Unit, body)

/** I: 実 AA 環境・Gradle なしのテスト。 */
abstract class IntegrationSpec(body: AcgSpec.() -> Unit = {}) : AcgSpec(CaseKind.Integration, body)

/** E: 実 Gradle ビルド / kct fork を使う E2E テスト。フィクスチャや TestKit の補助は ACG-38 で足す。 */
abstract class E2ESpec(body: AcgSpec.() -> Unit = {}) : AcgSpec(CaseKind.E2E, body)

/** D: KSP2 や Reference Engine と出力を比べる差分テスト。三者差分の補助は ACG-42 で足す。 */
abstract class DiffSpec(body: AcgSpec.() -> Unit = {}) : AcgSpec(CaseKind.Diff, body)

/** P: ランダムな入力・履歴で不変条件を確かめる PBT。生成器の補助は ACG-40 で足す。 */
abstract class PbtSpec(body: AcgSpec.() -> Unit = {}) : AcgSpec(CaseKind.Property, body)

/** B: 時間・メモリのベンチマーク。 */
abstract class BenchSpec(body: AcgSpec.() -> Unit = {}) : AcgSpec(CaseKind.Benchmark, body)
