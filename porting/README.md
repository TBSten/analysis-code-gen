# 移植コードの出所記録

KSP2 などの上流から写したコードの出所・著作権表示・改変内容を、ここで決めた書式で残す（ADR-018）。LIC-01 の検査 script がこの書式を前提に検査する。

## 置き場所

| パス | 中身 |
|---|---|
| `porting/<id>/UPSTREAM.properties` | 上流のリポジトリ・固定するタグとコミット・NOTICE に書く文字列 |
| `porting/<id>/port-map.csv` | 上流ファイルごとの移植先・方針・状態 |
| `NOTICE`（リポジトリ直下） | 第三者コードの著作権表示 |
| `scripts/lic01/check_provenance.py` | LIC-01 の検査 |

いまの上流は `ksp`（google/ksp、タグ 2.3.14）だけ。KSP 公式テストや他 Processor のテスト入力を取り込むときも、同じ形で `porting/<id>/` を足す。

## ファイルの先頭に書くこと

上流の著作権・ライセンスのヘッダはそのまま残し、その直後に 3 種類の行を足す。

```kotlin
/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * （上流のヘッダを 1 文字も変えずに残す）
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/FileCache.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名を変更。キャッシュの寿命を解析セッション単位に変更。
```

- `Ported-From` は `<owner>/<repo>@<40 桁のコミット SHA>:<上流のパス>`。タグや短縮 SHA は不可。上流の複数ファイルを 1 つにまとめたときは行を複数書く
- `Modifications` は Apache License 2.0 §4(b) の「変更したことの表示」。改変を重ねたら追記する。パッケージ名だけなら「パッケージ名のみ変更」と書く
- 上流のファイルにヘッダが無いとき（KSP では 29 ファイル）は、次の定型を先頭に足す

```kotlin
/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
```

## 移植の手順

1. `port-map.csv` で対象の行を探し、`target_module` と `strategy` を確認する
2. 上流の固定コミットからファイルを写し、パッケージ名を `me.tbsten.analysiscodegen.<モジュール>` 配下へ変える（ADR-012。KSP 本体と同じクラスパスに載っても衝突させない）。モジュールの依存方向（ADR-012）を破る import が残るなら `adapt` として直す
3. 先頭に上の 3 行を足し、改変したら `Modifications` に書く
4. `port-map.csv` の行を `status=ported`、`target_paths` に移植先のパスを書く（分割したら `;` 区切り）
5. `python3 scripts/lic01/check_provenance.py` を通す

`com.intellij.*` を同名で差し替えるパッチ（JetBrains 著作）は、パッケージ名を変えられないため `defer` にしてある。使うときは ADR を足す。

## port-map.csv の列

| 列 | 意味 |
|---|---|
| `upstream_path` | 固定コミットでの上流のパス |
| `size_bytes` / `upstream_copyright` | 上流ファイルの大きさと、ヘッダにある著作権者（`none` はヘッダ無し） |
| `target_module` | 移植先モジュール（ADR-012 の名前。`;` 区切りは分割して複数へ写す） |
| `strategy` | `as-is` ほぼそのまま / `adapt` 構造を保って改変 / `reference` 読むだけで新規に書く / `defer` PoC 後に判断 / `exclude` 使わない |
| `phase` | 移植する時期（P2・P3） |
| `status` | `planned` / `ported` / `not-ported`（reference・exclude） |
| `target_paths` | 移植先のパス。`ported` のときだけ書く |
| `notes` | 移植先のパッケージ・改変の要点 |

`reference` で書いたコードは写していないので出所の行は要らないが、参考にした上流のパスをコメントに書いておく。

## 上流を更新するとき

1. `UPSTREAM.properties` の `commit` を新しいものへ変え、古いものを `previous_commits` に移す
2. `git -C <ksp> diff <旧>..<新> --stat -- $(cut -d, -f1 porting/ksp/port-map.csv | tail -n +2)` で移植対象の変更を洗い出す
3. 取り込んだファイルは `Ported-From` を新しいコミットに書き換え、`previous_commits` から外せるようになったら外す
4. NOTICE のタグ・コミットも更新する（LIC-01 が検査する）
