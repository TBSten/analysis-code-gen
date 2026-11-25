# testing の API の細部（実装者向け）

決定の理由は ADR-021・ADR-036（共有フォルダ `pm/adr/`）。利用者向けの説明は docs サイトの「テスト」（`docs/site/src/content/docs/testing/`）で、その例は `fixtures/docs/testing/` にある。

- Native Processor は `CodegenTestSpec.native(provider: AnalysisCodegenProcessorProvider)` で指定する（`TestProcessor.Native`）。analysis-code-gen-processor-api の中身が main に入ったら足す。`TestEngine.Ksp2` に Native を渡したら、何を使えばよいかを書いたエラーにする
- `generatedFiles` のキーは、SourceSet が 1 つなら `kotlin/…`・`java/…`・`resources/…`、2 つ以上なら先頭に `<SourceSet>/` を付ける
- `runCodegenScenario` は、`AnalysisCodegen` では同じ作業ディレクトリで続けて実行し（2 回目から増分）、`Ksp2` では毎回クリーンに実行する
- testing はテストフレームワークに依存させない（kotest も JUnit も API に出さない）
