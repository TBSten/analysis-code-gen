# AA 解析環境（ADR-009・010・060）

`analysis-code-gen-analysis-engine` の `StandaloneAnalysisEnvironmentFactory` を使う・直すときの決まり。理由は ADR-060（共有フォルダ `pm/adr/`）。

## 守ること

- 環境は実行単位ごとに 1 つ作り、ラウンドをまたいで使う。終わったら `close()` する（MEM-01 が Project の回収を見る）
- モジュールは SourceSet 1 つにつき 1 つ（`ModuleId` = SourceSet 名）。dependsOn は `addDependsOnDependency`
- 生成物は `applyChanges` の `added` で渡す。追加だけなら既存の PSI はそのまま。`modified` / `removed` を含むと環境を作り直すので、前の KtFile・KaSymbol は持ち越さない
- KSP2 から写したファイル（`Ported-From` の行があるもの）を直したら `Modifications` に書き足す
- AA を動かす実行時の一式は bundle `aa-runtime`（`gradle/libs.versions.toml`）だけで管理する。足りない JAR はここに足す
- `kotlinx.coroutines.internal.intellij.IntellijCoroutines` は IntelliJ のための代替クラス。JetBrains 版 coroutines と一緒に載せない
- AA をテストから直接使うモジュールは、`aa-runtime` を実行時クラスパスの先頭に置く（kotlin-compiler-embeddable と `org.jetbrains.kotlin.cli.*` がぶつかる。engine-tests の `build.gradle.kts`）
