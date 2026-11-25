# PoC P-01: Standalone Analysis API で KMP の中間 SourceSet を単独解析する

結論は ADR-009（共有フォルダ `pm/adr/ADR-009-*.md`）。このコードは本番に流用しない。

## 何を確かめたか

`commonMain` → 中間 `jvmAndJsMain` / `webMain` → 固有 `jvmMain` / `jsMain` / `wasmJsMain` の SourceSet を、
Standalone AA の `KaSourceModule` + `addDependsOnDependency` で Module Graph にし、各 SourceSet を単独で解析できるか。

```
commonMain ──┬── jvmAndJsMain ──┬── jvmMain
             │                  └── jsMain ──┐（ひし形）
             └── webMain ───────────────────┴── wasmJsMain は webMain のみ
```

## 構成

| パス | 中身 |
|---|---|
| `sample/` | KGP 2.4.21 の KMP サンプル。`-Pp01.errors=common\|shared\|jvm` でその層にわざとエラーを足す。`dumpModel` で SourceSet 構造と変換済み metadata KLIB をダンプ |
| `sample/oracle.sh` | KGP の実コンパイルで診断を集める（オラクル） |
| `analyzer/` | Standalone AA の CLI。KSP2 の `symbol-processing-aa-embeddable`（Maven Central）を使う |
| `compare.py` | オラクルと AA の診断・解決結果を突き合わせる |
| `run-all.sh` | 上の全部を順に実行し `build/p01-result.md` を出す |

## 実行

```sh
./poc/p01/run-all.sh   # JDK 21 が要る。初回は依存の取得で数分
```

## AA を使うときの落とし穴（本番実装への申し送り）

- `aa-embeddable` は再配置時に `*.kotlin_module` を落としているため、Kotlin からトップレベル関数
  （`buildStandaloneAnalysisAPISession`、`buildKtSourceModule`、`analyze`、`mainReference` など）が見えない。
  ビルダーのコンストラクタ、`KaSessionProvider.getInstance(project).analyze(...)` などメンバーを使う
- アプリケーションサービス 3 つ（`KaAnalysisPermissionRegistry` ほか）が自動登録されないので手で登録する（KSP2 も同じ）
- PSI の reference provider が無く `KtElement.references` が空。解決は `resolveToCall` などの AA API で行う
- `MultiPlatformProjects` を `languageVersionSettings` で有効にしないと expect/actual が全部エラーになる
