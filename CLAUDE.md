# analysis-code-gen — Agent 向けの決まり

Kotlin Analysis API（AA）を直接使うコード生成基盤（KSP2 の代替）。実装はほぼすべて AI Agent が行い、人間（てべすてん）は方針とレビューだけを担う。細かい仕様は Agent が決め打ちして ADR に残す（人間待ちで止まらない）。

詳細なルールは `.claude/rules/`、繰り返す手順は `.claude/skills/` にある。迷ったらここ → rules → ADR の順に読む。

## 始めるとき・再開するとき（必ずこの順）

Agent はいつ止まってもよい前提で動く。続きは次の 4 段で拾う（[resume-and-handoff](.claude/rules/resume-and-handoff.md)、skill `resume-work`）。

1. **テストケースの通過状況**で現在地を知る（`quality/` のケース一覧と CI の結果）
2. **作業チケット**（Notion、ID は `ACG-n`）の `## 次の一手` と Next 列を読む
3. **関係する ADR** を読む（共有フォルダ `pm/28-adr.md` の索引 → `pm/adr/ADR-*.md`）
4. 続きから着手する。1 つの作業は数時間で区切れる大きさに分け、小さく commit して早めに push する

## 絶対に守ること

- **テストを弱めない（R-01）**: `@PendingCase`（保留）→ 有効 の一方向だけ。有効なテストを保留に戻す・消す・期待値を緩めるのは禁止。必要なら ADR を書き、てべすてんに報告する（[pending-case](.claude/rules/pending-case.md)）
- **決めたら ADR**: 仕様・設計の決め打ちは 1 決定 1 ファイルの ADR に残す（[adr](.claude/rules/adr.md)）
- **移植したら出所を書く**: KSP2 などから写したコードは、上流ヘッダ + `Ported-From` などの 3 行 + `porting/<id>/port-map.csv`（[porting](.claude/rules/porting.md)）
- **main へは PR で**: branch → PR → CI が緑なら自分で squash マージしてよい。CI を通すためにテストを skip・削除しない（[git-and-pr](.claude/rules/git-and-pr.md)）
- **スコープの変更と重大な問題だけ人間に聞く**。それ以外は決め打ちして ADR

## ビルド

```bash
./gradlew build                 # 全モジュールのビルド + テスト + ktlint
./gradlew :e2e-tests:test       # 1 モジュールだけ
./gradlew ktlintFormat          # 整形
python3 scripts/kotlin-versions.py check   # 対応 Kotlin 一覧の検査（ADR-011）
python3 scripts/processors.py check        # 対象 Processor 一覧の検査（ADR-019）
python3 scripts/lic01/check_provenance.py  # 移植コードの出所検査（ADR-018）
```

- JDK 21 でビルドし、生成物は JVM 17 向け。Gradle 9.9.2（wrapper）
- バージョンは `gradle/libs.versions.toml`、共通設定は `build-logic/` の `acg.jvm` / `acg.library` / `acg.gradle-plugin`
- クラウド環境では SessionStart hook（`.claude/hooks/session-start.sh`）が Maven Central のミラー設定と依存の取得を済ませる

## どこに何があるか

| 場所 | 中身 |
|---|---|
| `README.md` | モジュール一覧と役割 |
| `docs/design/` | 実装者向けの設計メモ（ADR の「守ること」だけを抜き出したもの） |
| `porting/` | 移植元の固定と出所記録（ADR-018） |
| `fixtures/processors/` | 移植・検証する既存 Processor の一覧（ADR-019） |
| `fixtures/projects/`・`test-processors/` | 共通フィクスチャ F-* とテスト用 Processor TP-*（ADR-027。`fixtures/README.md`） |
| `gradle/kotlin-versions.txt` | 対応 Kotlin の唯一の一覧（ADR-011） |
| `quality/` | テストケース一覧・候補置き場（ACG-37 で共有フォルダから移す） |
| 共有フォルダ `pm/` | PM 資料・ADR・P1 計画（クラウドでは `/mnt/project-files/pm/`） |

## 書き方

- ドキュメント・コメント・コミット・PR は**日本語**。識別子は英語
- ドキュメントは短く（1 ページ 1,400 字以下など。[docs-style](.claude/rules/docs-style.md)）
- Kotlin のコードの決まりは [kotlin-code](.claude/rules/kotlin-code.md)
- モジュール名は `analysis-code-gen-xxx`（ディレクトリ名も同じ）、group は `me.tbsten.analysis.code.gen`。検査・テスト用の内部モジュールは例外（[module-naming](docs/design/module-naming.md)、ADR-042）
