# docs/site — ドキュメントサイト

Astro Starlight（日本語のみ）。Gradle とは独立していて npm で動かす。決定の理由は ADR-031（共有フォルダ `pm/adr/`）。

```sh
npm ci
npm run dev     # 開発サーバ
npm run build   # dist/ に出力。内部リンク切れがあると失敗する
```

- ページは `src/content/docs/`。置き場所と分量の基準は `../README.md` と ADR-028（`python3 scripts/doclint/doc_lint.py`）
- サイドバーは `astro.config.mjs` に手で書く。ページを足したらここにも足す
- ページ内のリンクは base 込みの絶対パス（`/analysis-code-gen/start/install/`）で書く
- コード例は `fixtures/docs/<ページ>/` の実ファイルから写す。frontmatter に `fixture:`、コードブロックに `title="<相対パス>"` を書くと、`python3 scripts/docsnippets/check_snippets.py` が一致を検査する。E2E（DOC-02）はそのフィクスチャをビルド・実行する。クラスの中から切り出した例は字下げを浅くして載せてよい（共通の字下げは無視して比べる。ADR-036）
- API リファレンスは Dokka（`./gradlew dokkaGenerate` → `build/dokka/html`）。docs ワークフロー（ADR-026）がサイトの `/api/` に置く
- 各ページの冒頭に「レビューで判断してほしいこと」を置き、G0 レビューで消す

## フィードバックモード（dev だけ。ADR-043）

`npm run dev` で開くと、画面下の Dev Toolbar に **Feedback**（電球）が出る。押してページの要素をクリック（または「ページ全体」）し、コメントを書いて「送る」。Esc でやめる。

- 記録はリポジトリ直下の `.local/feedback/<YYMMDD-NN>.md`（git に入らない）。中身は選んだ要素のセレクタ・XPath・URL・ブラウザのログ。スクショは撮らない
- 置き場は `FEEDBACK_DIR=<リポジトリからの相対パス> npm run dev` で変えられる（検証で本物を汚さないとき）
- Agent への依頼は「docs の feedback を対応して」。Agent は `.local/feedback/*.md` を読んでページを直し、済んだ記録を `.local/feedback/archived/` へ移す
- `astro build` には何も入らない。docs ワークフローが `npm run test:feedback`（受け口のテスト）と `npm run check:dist`（dist に痕跡が無いか）で確かめる
- 実装は `src/integrations/feedback/`（feedback-mode skill の Astro 用ひな形。`core/` は skill からのコピーなので直接直さない）
