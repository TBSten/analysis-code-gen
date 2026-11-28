# KSP Bridge の作り（ADR-017・062）

KSP の Processor を共通エンジンで回す analysis-code-gen-ksp-bridge の決まり。理由は ADR-062（共有フォルダ `pm/adr/`）。

## 守ること

- `ksp/bridge/common/`・`ksp/bridge/impl/` と `InternalKSPException.kt` は KSP2 から写したコード。上流との差分を小さく保つため、整形・可視性の書き足し・作り込みをしない。直したら各ファイルの `Modifications` に追記する（porting ルール）
- 移植したいファイルが増えたら skill `port-from-ksp2` で写し、`porting/ksp/port-map.csv` を更新する
- `ksp/bridge/` 直下（`Ksp*`・`Bridge*`）が自前の Adapter。エンジンとは engine-spi だけでやり取りする（ADR-012。katachi が import を検査する）
- 移植コードはスレッドごとの静的な状態（`ResolverAAImpl.instance`・`ktModule`・`kspConfig`、KS オブジェクトのキャッシュ）を読む。`KspEngineProcessor` が呼び出しの前に入れ、終わったら外す。新しい入口を足すときも同じようにする
- KS オブジェクトはラウンドをまたいで使えない。持ち越すのは defer の pointer（`Restorable`）だけ
- KSPConfig は KSP の `symbol-processing-common-deps` のものを使う。移植コードが読むのは `experimentalPsiResolution` だけ
- AA の実装側の jar は compileOnly。足すときは `analysis-code-gen-ksp-bridge/build.gradle.kts` の一覧に足す

## テストでの動かし方（ADR-065）

- AA を動かすテストモジュールは、AA の一式（bundle `aa-runtime`）をテストの classpath の先頭に置く
- `TestEngine.Ksp2`（kct + KSP2）は testing が AA を除いた ClassLoader で動かす。共有するのはテストの型・processor-api・KSP の公開 API だけ
