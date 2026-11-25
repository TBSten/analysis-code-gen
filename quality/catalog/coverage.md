---
title: 入力パターンカタログの網羅表
project: analysisCodeGen
status: 自動生成（quality/catalog/scripts/catalog.py index）。手で編集しない
---

# 入力パターンカタログの網羅表

> 判断したいこと: 利用者が書きうる入力の空間を、カタログがどこまで覆っているか。穴はどこか。

次元と値の定義は [dimensions.toml](dimensions.toml)、決め方は ADR-020。各セルは、その値を含むパターンの数（① テスト / ② 実プロジェクト / ③ 自作）。

## 1. 出所ごとの件数

| 対象 | ① テスト | ② 実プロジェクト | ③ 自作 | 計 | K で確認済み |
|---|---:|---:|---:|---:|---:|
| common | 0 | 0 | 10 | 10 | 0 |
| cream | 511 | 78 | 4 | 593 | 0 |
| moshi | 135 | 38 | 4 | 177 | 0 |
| koin | 9 | 53 | 14 | 76 | 0 |
| **計** | 655 | 169 | 32 | 856 | 0 |

「K で確認済み」は、KSP2 でビルドして結果を記録したパターン（ACG-42 の差分ハーネスが付ける）。

## 2. 必須の値の網羅: 125 / 125

必須の値はすべて 1 件以上のパターンで覆われています。

1〜2 件しかない必須の値（薄いところ）: `cream.family=options`、`moshi.option=moshi.generated`、`moshi.option=moshi.generateProguardRules`、`moshi.option=moshi.instantiateAnnotations`、`koin.module=default-module`、`koin.binding=auto-bind-interface`、`koin.binding=Qualifier`、`koin.binding=nullable-param`、`koin.other=config-check-error`、`koin.option=KOIN_GENERATION_PACKAGE`、`koin.option=KOIN_USE_COMPOSE_VIEWMODEL`

## 3. 次元ごとの網羅

### 宣言の種類（`kt.decl`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `class` | ✓ | 152 | 57 | 24 | 233 | CMN-O-0001 CMN-O-0002 CMN-O-0003 |
| `data-class` | ✓ | 541 | 115 | 11 | 667 | CRM-O-0001 CRM-O-0002 CRM-O-0003 |
| `value-class` | ✓ | 55 | 5 | 2 | 62 | CRM-O-0003 CRM-R-0029 CRM-R-0040 |
| `enum-class` | ✓ | 11 | 8 | 1 | 20 | CMN-O-0008 CRM-T-0037 CRM-T-0086 |
| `sealed-class` | ✓ | 8 | 9 | 1 | 18 | CRM-R-0001 CRM-R-0065 CRM-R-0067 |
| `sealed-interface` | ✓ | 179 | 22 | 4 | 205 | CRM-O-0002 CRM-O-0003 CRM-O-0004 |
| `interface` | ✓ | 22 | 9 | 9 | 40 | CMN-O-0002 CMN-O-0004 CMN-O-0006 |
| `fun-interface` |  | 0 | 2 | 1 | 3 | CMN-O-0003 KOI-R-0030 KOI-R-0043 |
| `abstract-class` | ✓ | 12 | 1 | 0 | 13 | CRM-T-0036 CRM-T-0085 CRM-T-0132 |
| `object` | ✓ | 167 | 22 | 2 | 191 | CMN-O-0001 CMN-O-0005 CRM-R-0008 |
| `data-object` |  | 13 | 15 | 1 | 29 | CRM-O-0003 CRM-R-0001 CRM-R-0002 |
| `companion-object` | ✓ | 2 | 13 | 1 | 16 | KOI-R-0001 KOI-R-0002 KOI-R-0006 |
| `annotation-class` |  | 13 | 2 | 3 | 18 | CMN-O-0008 CRM-R-0050 CRM-T-0082 |
| `typealias` | ✓ | 10 | 8 | 1 | 19 | CMN-O-0003 CRM-R-0011 CRM-R-0019 |
| `top-level-fun` | ✓ | 6 | 10 | 4 | 20 | CMN-O-0003 CMN-O-0004 CMN-O-0006 |
| `top-level-property` |  | 3 | 11 | 3 | 17 | CMN-O-0003 CMN-O-0005 CRM-R-0002 |
| `extension-fun` |  | 0 | 14 | 0 | 14 | KOI-R-0001 KOI-R-0007 KOI-R-0023 |
| `extension-property` |  | 2 | 2 | 0 | 4 | CRM-R-0002 CRM-T-0009 CRM-T-0415 |
| `local-class` |  | 2 | 2 | 2 | 6 | CMN-O-0003 CMN-O-0004 KOI-R-0026 |
| `object-expression` |  | 1 | 1 | 2 | 4 | CMN-O-0003 KOI-O-0012 KOI-R-0028 |

### 可視性（`kt.vis`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `public-implicit` | ✓ | 161 | 159 | 30 | 350 | CMN-O-0001 CMN-O-0003 CMN-O-0004 |
| `public-explicit` |  | 492 | 0 | 1 | 493 | CMN-O-0002 CRM-T-0003 CRM-T-0004 |
| `internal` | ✓ | 27 | 32 | 2 | 61 | CMN-O-0006 CRM-R-0041 CRM-R-0062 |
| `private` | ✓ | 33 | 48 | 16 | 97 | CMN-O-0002 CMN-O-0004 CMN-O-0006 |
| `protected` |  | 1 | 0 | 0 | 1 | CRM-T-0361 |

### 入れ子（`kt.nest`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `nested` | ✓ | 187 | 25 | 7 | 219 | CMN-O-0003 CMN-O-0004 CRM-O-0002 |
| `inner` |  | 7 | 0 | 1 | 8 | CMN-O-0004 CRM-T-0038 CRM-T-0087 |
| `deep-nested` | ✓ | 5 | 5 | 1 | 11 | CMN-O-0004 CRM-R-0038 CRM-R-0073 |

### ジェネリクス（`kt.generics`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `type-param` | ✓ | 47 | 10 | 2 | 59 | CMN-O-0004 CRM-R-0005 CRM-R-0022 |
| `upper-bound` | ✓ | 17 | 4 | 1 | 22 | CMN-O-0004 CRM-T-0051 CRM-T-0096 |
| `multiple-bounds` |  | 3 | 1 | 1 | 5 | CMN-O-0004 CRM-R-0072 CRM-T-0453 |
| `variance-out` | ✓ | 4 | 3 | 3 | 10 | CMN-O-0003 CMN-O-0004 CRM-R-0064 |
| `variance-in` |  | 2 | 0 | 2 | 4 | CMN-O-0003 CMN-O-0004 MSH-T-0035 |
| `star-projection` | ✓ | 1 | 4 | 3 | 8 | CMN-O-0004 CMN-O-0008 KOI-R-0025 |
| `recursive-bound` |  | 11 | 0 | 1 | 12 | CMN-O-0004 CRM-T-0051 CRM-T-0096 |
| `nested-generic` | ✓ | 17 | 11 | 5 | 33 | CMN-O-0002 CMN-O-0004 CMN-O-0008 |
| `reified` |  | 0 | 0 | 1 | 1 | CMN-O-0004 |

### 型の形（`kt.types`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `nullable` | ✓ | 54 | 41 | 7 | 102 | CMN-O-0004 CMN-O-0006 CRM-O-0004 |
| `list` | ✓ | 28 | 35 | 12 | 75 | CMN-O-0002 CMN-O-0003 CMN-O-0004 |
| `set` |  | 3 | 5 | 2 | 10 | CMN-O-0004 CRM-R-0032 CRM-R-0052 |
| `map` | ✓ | 9 | 12 | 3 | 24 | CMN-O-0004 CMN-O-0006 CRM-R-0032 |
| `array` |  | 11 | 4 | 1 | 16 | CMN-O-0008 CRM-R-0078 CRM-T-0200 |
| `function-type` | ✓ | 1 | 11 | 2 | 14 | CMN-O-0003 CMN-O-0004 KOI-R-0013 |
| `primitive` | ✓ | 294 | 98 | 19 | 411 | CMN-O-0001 CMN-O-0003 CMN-O-0004 |
| `unsigned` |  | 0 | 1 | 0 | 1 | KOI-R-0033 |
| `string` | ✓ | 507 | 123 | 24 | 654 | CMN-O-0001 CMN-O-0003 CMN-O-0004 |
| `platform-type` |  | 1 | 0 | 2 | 3 | CMN-O-0002 MSH-O-0003 MSH-T-0021 |

### コンストラクタ・引数（`kt.ctor`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `primary` | ✓ | 611 | 137 | 28 | 776 | CMN-O-0001 CMN-O-0002 CMN-O-0003 |
| `no-arg` |  | 74 | 38 | 20 | 132 | CMN-O-0003 CMN-O-0007 CMN-O-0008 |
| `secondary` |  | 4 | 1 | 0 | 5 | CRM-T-0199 CRM-T-0247 CRM-T-0372 |
| `private-ctor` |  | 8 | 0 | 0 | 8 | CRM-T-0041 CRM-T-0090 CRM-T-0139 |
| `default-arg` | ✓ | 68 | 37 | 19 | 124 | CMN-O-0001 CMN-O-0003 CMN-O-0006 |
| `vararg` |  | 9 | 2 | 0 | 11 | CRM-R-0078 CRM-T-0200 CRM-T-0201 |
| `non-property-param` |  | 12 | 11 | 0 | 23 | CRM-R-0001 CRM-R-0073 CRM-T-0014 |

### プロパティ（`kt.prop`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `val` | ✓ | 556 | 146 | 31 | 733 | CMN-O-0001 CMN-O-0002 CMN-O-0003 |
| `var` | ✓ | 63 | 15 | 2 | 80 | CMN-O-0008 CRM-T-0444 KOI-O-0008 |
| `lateinit` |  | 1 | 1 | 1 | 3 | CMN-O-0008 CRM-T-0444 KOI-R-0032 |
| `delegated` |  | 5 | 14 | 1 | 20 | CMN-O-0008 CRM-T-0442 KOI-R-0009 |
| `const` |  | 2 | 16 | 2 | 20 | CMN-O-0005 KOI-R-0001 KOI-R-0006 |
| `custom-getter` |  | 13 | 8 | 0 | 21 | CRM-R-0002 CRM-T-0009 CRM-T-0304 |
| `body-property` |  | 39 | 25 | 7 | 71 | CMN-O-0007 CMN-O-0008 CMN-O-0009 |
| `jvm-field` |  | 0 | 0 | 0 | 0 |  |
| `override` |  | 82 | 25 | 6 | 113 | CMN-O-0004 CRM-O-0002 CRM-O-0003 |

### アノテーションの書き方（`kt.annot`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `use-site-target` | ✓ | 2 | 6 | 2 | 10 | CMN-O-0005 CMN-O-0008 MSH-R-0013 |
| `array-arg` | ✓ | 75 | 16 | 3 | 94 | CMN-O-0008 CRM-R-0013 CRM-R-0014 |
| `class-literal-arg` | ✓ | 341 | 75 | 8 | 424 | CRM-O-0001 CRM-O-0002 CRM-O-0004 |
| `named-arg` | ✓ | 233 | 59 | 10 | 302 | CMN-O-0008 CRM-R-0001 CRM-R-0013 |
| `repeated` |  | 14 | 17 | 2 | 33 | CMN-O-0008 CRM-R-0001 CRM-R-0073 |
| `meta-annotation` |  | 9 | 1 | 3 | 13 | CMN-O-0008 KOI-O-0009 KOI-R-0036 |
| `fqn-annotation` |  | 0 | 1 | 0 | 1 | KOI-R-0026 |

### その他の言語要素（`kt.misc`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `kdoc` | ✓ | 1 | 40 | 0 | 41 | CRM-R-0005 CRM-R-0006 CRM-R-0007 |
| `deprecated` |  | 10 | 2 | 1 | 13 | CMN-O-0003 CRM-T-0173 CRM-T-0406 |
| `backtick-name` |  | 121 | 4 | 1 | 126 | CMN-O-0005 CRM-R-0054 CRM-R-0055 |
| `suspend` |  | 0 | 21 | 1 | 22 | CMN-O-0003 KOI-R-0001 KOI-R-0013 |
| `expect-actual` |  | 0 | 1 | 2 | 3 | CMN-O-0001 KOI-O-0012 KOI-R-0035 |
| `context-parameters` |  | 0 | 0 | 1 | 1 | CMN-O-0003 |
| `inheritance` | ✓ | 184 | 60 | 11 | 255 | CMN-O-0001 CMN-O-0003 CMN-O-0004 |
| `opt-in` |  | 0 | 6 | 0 | 6 | KOI-R-0023 KOI-R-0026 KOI-R-0033 |
| `jvm-name` |  | 1 | 0 | 1 | 2 | CMN-O-0005 MSH-T-0071 |

### ファイル・モジュールの配置（`kt.layout`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `single-file` | ✓ | 646 | 169 | 19 | 834 | CMN-O-0003 CMN-O-0004 CMN-O-0005 |
| `multi-file` | ✓ | 9 | 0 | 13 | 22 | CMN-O-0001 CMN-O-0002 CMN-O-0006 |
| `multi-package` | ✓ | 8 | 0 | 4 | 12 | CMN-O-0006 CRM-O-0001 KOI-O-0007 |
| `java-source` | ✓ | 1 | 0 | 2 | 3 | CMN-O-0002 MSH-O-0003 MSH-T-0021 |
| `default-package` |  | 1 | 0 | 1 | 2 | CMN-O-0005 KOI-T-0004 |
| `multi-module` | ✓ | 0 | 0 | 4 | 4 | CMN-O-0006 CRM-O-0001 KOI-O-0007 |
| `kmp-common` | ✓ | 0 | 90 | 5 | 95 | CMN-O-0001 CRM-O-0001 CRM-O-0002 |
| `kmp-intermediate` |  | 0 | 0 | 3 | 3 | CMN-O-0001 CRM-O-0002 KOI-O-0012 |
| `kmp-platform` | ✓ | 3 | 2 | 6 | 11 | CMN-O-0001 CMN-O-0009 CRM-O-0002 |
| `android` | ✓ | 3 | 28 | 4 | 35 | CMN-O-0009 KOI-O-0006 KOI-O-0008 |
| `test-source-set` |  | 0 | 0 | 1 | 1 | CMN-O-0010 |

### 期待する結果（`kt.outcome`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `ok` | ✓ | 560 | 169 | 30 | 759 | CMN-O-0001 CMN-O-0002 CMN-O-0003 |
| `processor-error` | ✓ | 95 | 0 | 1 | 96 | CRM-T-0001 CRM-T-0002 CRM-T-0036 |
| `processor-warning` |  | 0 | 0 | 0 | 0 |  |
| `compile-error` |  | 0 | 0 | 1 | 1 | CMN-O-0007 |

### Processor の組み合わせ（`proc.combo`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `single` | ✓ | 655 | 168 | 20 | 843 | CRM-O-0001 CRM-O-0002 CRM-O-0003 |
| `multi` | ✓ | 0 | 1 | 2 | 3 | KOI-O-0008 KOI-O-0013 MSH-R-0001 |

### cream.kt のアノテーション（`cream.annotation`、対象: cream）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `CopyTo` | ✓ | 88 | 17 | 4 | 109 | CRM-O-0001 CRM-O-0002 CRM-O-0004 |
| `CopyFrom` | ✓ | 52 | 13 | 1 | 66 | CRM-O-0002 CRM-R-0031 CRM-R-0032 |
| `CopyToChildren` | ✓ | 47 | 8 | 4 | 59 | CRM-O-0002 CRM-O-0003 CRM-O-0004 |
| `CombineTo` | ✓ | 50 | 12 | 0 | 62 | CRM-R-0020 CRM-R-0021 CRM-R-0022 |
| `CombineFrom` | ✓ | 48 | 11 | 0 | 59 | CRM-R-0003 CRM-R-0004 CRM-R-0005 |
| `CopyMapping` | ✓ | 56 | 7 | 0 | 63 | CRM-R-0042 CRM-R-0043 CRM-R-0044 |
| `CombineMapping` | ✓ | 45 | 7 | 0 | 52 | CRM-R-0013 CRM-R-0014 CRM-R-0015 |
| `SealedCopy` | ✓ | 45 | 5 | 0 | 50 | CRM-R-0071 CRM-R-0074 CRM-R-0075 |
| `ParentOptional` | ✓ | 59 | 3 | 1 | 63 | CRM-O-0004 CRM-R-0001 CRM-R-0002 |
| `ChildOptionals` | ✓ | 30 | 2 | 0 | 32 | CRM-R-0001 CRM-R-0002 CRM-T-0001 |
| `Map` | ✓ | 25 | 8 | 0 | 33 | CRM-R-0010 CRM-R-0027 CRM-R-0037 |
| `Exclude` | ✓ | 29 | 3 | 0 | 32 | CRM-R-0002 CRM-R-0062 CRM-R-0071 |

### cream.kt の網羅軸（cream の snapshot テストの scenario 族）（`cream.family`、対象: cream）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `sourceKind` | ✓ | 18 | 0 | 0 | 18 | CRM-T-0033 CRM-T-0034 CRM-T-0035 |
| `targetKind` | ✓ | 47 | 0 | 0 | 47 | CRM-T-0036 CRM-T-0037 CRM-T-0038 |
| `generics` | ✓ | 38 | 0 | 0 | 38 | CRM-T-0051 CRM-T-0052 CRM-T-0053 |
| `propertyShape` | ✓ | 54 | 0 | 0 | 54 | CRM-T-0055 CRM-T-0056 CRM-T-0057 |
| `matching` | ✓ | 6 | 0 | 0 | 6 | CRM-T-0061 CRM-T-0105 CRM-T-0158 |
| `map` | ✓ | 27 | 0 | 0 | 27 | CRM-T-0062 CRM-T-0063 CRM-T-0106 |
| `exclude` | ✓ | 31 | 0 | 0 | 31 | CRM-T-0022 CRM-T-0023 CRM-T-0024 |
| `visibility` | ✓ | 39 | 0 | 0 | 39 | CRM-T-0020 CRM-T-0021 CRM-T-0068 |
| `funName` | ✓ | 26 | 0 | 0 | 26 | CRM-T-0072 CRM-T-0073 CRM-T-0116 |
| `kdoc` | ✓ | 24 | 0 | 0 | 24 | CRM-T-0018 CRM-T-0019 CRM-T-0066 |
| `valueClassMapping` | ✓ | 46 | 0 | 0 | 46 | CRM-T-0031 CRM-T-0032 CRM-T-0079 |
| `nesting` |  | 13 | 0 | 0 | 13 | CRM-T-0190 CRM-T-0191 CRM-T-0192 |
| `constructor` |  | 12 | 0 | 0 | 12 | CRM-T-0199 CRM-T-0200 CRM-T-0201 |
| `multiSource` |  | 19 | 0 | 0 | 19 | CRM-T-0043 CRM-T-0044 CRM-T-0045 |
| `repeatable` |  | 9 | 0 | 0 | 9 | CRM-T-0074 CRM-T-0075 CRM-T-0076 |
| `hierarchyShape` |  | 18 | 0 | 0 | 18 | CRM-T-0005 CRM-T-0006 CRM-T-0007 |
| `deprecated` |  | 8 | 0 | 0 | 8 | CRM-T-0173 CRM-T-0406 CRM-T-0407 |
| `escaping` |  | 3 | 0 | 0 | 3 | CRM-T-0409 CRM-T-0410 CRM-T-0411 |
| `useCase` |  | 8 | 0 | 0 | 8 | CRM-T-0225 CRM-T-0281 CRM-T-0325 |
| `sealedParentKind` |  | 10 | 0 | 0 | 10 | CRM-T-0003 CRM-T-0004 CRM-T-0286 |
| `propertyFiltering` |  | 6 | 0 | 0 | 6 | CRM-T-0008 CRM-T-0009 CRM-T-0010 |
| `propertyName` |  | 7 | 0 | 0 | 7 | CRM-T-0027 CRM-T-0028 CRM-T-0029 |
| `parentOptionalInterop` |  | 4 | 0 | 0 | 4 | CRM-T-0014 CRM-T-0015 CRM-T-0016 |
| `sourceKindValidation` |  | 2 | 0 | 0 | 2 | CRM-T-0119 CRM-T-0120 |
| `canReverse` |  | 2 | 0 | 0 | 2 | CRM-T-0261 CRM-T-0262 |
| `merge` |  | 4 | 0 | 0 | 4 | CRM-T-0433 CRM-T-0434 CRM-T-0435 |
| `notCopyToObject` |  | 3 | 0 | 0 | 3 | CRM-T-0311 CRM-T-0312 CRM-T-0313 |
| `nonCopyableStrategy` |  | 5 | 0 | 0 | 5 | CRM-T-0491 CRM-T-0492 CRM-T-0493 |
| `invalidUsage` | ✓ | 21 | 0 | 0 | 21 | CRM-T-0001 CRM-T-0002 CRM-T-0413 |
| `options` | ✓ | 1 | 0 | 1 | 2 | CRM-O-0003 CRM-T-0414 |

### cream.kt のオプション（既定値以外を使うパターン）（`cream.option`、対象: cream）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `copyFunNamePrefix` |  | 330 | 0 | 1 | 331 | CRM-O-0003 CRM-T-0033 CRM-T-0034 |
| `copyFunNamingStrategy` | ✓ | 330 | 0 | 1 | 331 | CRM-O-0003 CRM-T-0033 CRM-T-0034 |
| `escapeDot` |  | 330 | 0 | 1 | 331 | CRM-O-0003 CRM-T-0033 CRM-T-0034 |
| `notCopyToObject` | ✓ | 435 | 0 | 1 | 436 | CRM-O-0003 CRM-T-0003 CRM-T-0004 |
| `defaultVisibility` | ✓ | 434 | 0 | 1 | 435 | CRM-O-0003 CRM-T-0003 CRM-T-0004 |
| `autoValueClassMapping` | ✓ | 46 | 0 | 1 | 47 | CRM-O-0003 CRM-T-0031 CRM-T-0032 |

### Moshi のアノテーション・機能（`moshi.annotation`、対象: moshi）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `JsonClass` | ✓ | 135 | 38 | 5 | 178 | KOI-O-0013 MSH-O-0001 MSH-O-0002 |
| `JsonClass-generator` | ✓ | 3 | 0 | 0 | 3 | MSH-T-0005 MSH-T-0091 MSH-T-0092 |
| `JsonClass-inline` |  | 0 | 0 | 0 | 0 |  |
| `Json-name` | ✓ | 11 | 29 | 2 | 42 | KOI-O-0013 MSH-O-0002 MSH-R-0001 |
| `Json-ignore` | ✓ | 1 | 0 | 2 | 3 | MSH-O-0002 MSH-O-0003 MSH-T-0015 |
| `Transient` | ✓ | 9 | 0 | 1 | 10 | MSH-O-0002 MSH-T-0014 MSH-T-0026 |
| `JsonQualifier` | ✓ | 7 | 0 | 1 | 8 | MSH-O-0001 MSH-T-0022 MSH-T-0023 |
| `ToJson-FromJson` |  | 0 | 1 | 0 | 1 | MSH-R-0001 |
| `generateAdapter-false` |  | 2 | 3 | 0 | 5 | MSH-R-0004 MSH-R-0025 MSH-R-0030 |

### Moshi で効く入力の形（`moshi.shape`、対象: moshi）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `default-values` | ✓ | 109 | 0 | 0 | 109 | MSH-T-0027 MSH-T-0028 MSH-T-0029 |
| `generic-class` | ✓ | 10 | 0 | 0 | 10 | MSH-T-0027 MSH-T-0041 MSH-T-0042 |
| `inheritance` | ✓ | 12 | 0 | 0 | 12 | MSH-T-0037 MSH-T-0038 MSH-T-0039 |
| `many-properties` | ✓ | 6 | 0 | 0 | 6 | MSH-T-0035 MSH-T-0052 MSH-T-0072 |
| `nullable-props` | ✓ | 21 | 0 | 0 | 21 | MSH-T-0029 MSH-T-0031 MSH-T-0035 |
| `nested-adapter` | ✓ | 89 | 0 | 0 | 89 | MSH-T-0032 MSH-T-0044 MSH-T-0045 |
| `unsupported-kind` | ✓ | 12 | 0 | 0 | 12 | MSH-T-0004 MSH-T-0006 MSH-T-0007 |
| `private-api` | ✓ | 5 | 0 | 0 | 5 | MSH-T-0001 MSH-T-0002 MSH-T-0003 |

### Moshi のオプション（既定値以外）（`moshi.option`、対象: moshi）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `moshi.generated` | ✓ | 1 | 0 | 1 | 2 | MSH-O-0002 MSH-T-0017 |
| `moshi.generateProguardRules` | ✓ | 1 | 0 | 1 | 2 | MSH-O-0002 MSH-T-0018 |
| `moshi.instantiateAnnotations` | ✓ | 0 | 0 | 1 | 1 | MSH-O-0001 |

### Koin の定義アノテーション（`koin.definition`、対象: koin）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `Single` | ✓ | 8 | 30 | 11 | 49 | KOI-O-0001 KOI-O-0002 KOI-O-0004 |
| `Singleton` |  | 4 | 2 | 0 | 6 | KOI-R-0005 KOI-R-0011 KOI-T-0004 |
| `Factory` | ✓ | 7 | 8 | 4 | 19 | KOI-O-0001 KOI-O-0003 KOI-O-0007 |
| `Scoped` | ✓ | 4 | 0 | 1 | 5 | KOI-O-0011 KOI-T-0001 KOI-T-0002 |
| `KoinViewModel` | ✓ | 1 | 4 | 2 | 7 | KOI-O-0006 KOI-O-0008 KOI-R-0003 |
| `KoinWorker` |  | 0 | 0 | 0 | 0 |  |
| `function-definition` | ✓ | 7 | 6 | 2 | 15 | KOI-O-0012 KOI-O-0013 KOI-R-0002 |
| `class-definition` | ✓ | 9 | 32 | 14 | 55 | KOI-O-0001 KOI-O-0002 KOI-O-0003 |

### Koin のモジュール構成（`koin.module`、対象: koin）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `Module` | ✓ | 9 | 16 | 12 | 37 | KOI-O-0002 KOI-O-0003 KOI-O-0004 |
| `ComponentScan` | ✓ | 9 | 12 | 12 | 33 | KOI-O-0002 KOI-O-0003 KOI-O-0004 |
| `ComponentScan-package` | ✓ | 3 | 8 | 4 | 15 | KOI-O-0002 KOI-O-0006 KOI-O-0007 |
| `includes` | ✓ | 3 | 2 | 1 | 6 | KOI-O-0007 KOI-R-0004 KOI-R-0024 |
| `Configuration` | ✓ | 8 | 5 | 0 | 13 | KOI-R-0004 KOI-R-0005 KOI-R-0010 |
| `KoinApplication` | ✓ | 5 | 1 | 0 | 6 | KOI-R-0009 KOI-T-0001 KOI-T-0004 |
| `createdAtStart` |  | 2 | 3 | 1 | 6 | KOI-O-0014 KOI-R-0024 KOI-R-0028 |
| `default-module` | ✓ | 0 | 0 | 1 | 1 | KOI-O-0001 |

### Koin のバインド・引数（`koin.binding`、対象: koin）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `auto-bind-interface` | ✓ | 0 | 0 | 1 | 1 | KOI-O-0002 |
| `explicit-binds` | ✓ | 2 | 4 | 1 | 7 | KOI-O-0014 KOI-R-0013 KOI-R-0024 |
| `Named` | ✓ | 6 | 7 | 1 | 14 | KOI-O-0009 KOI-R-0023 KOI-R-0024 |
| `Qualifier` | ✓ | 1 | 0 | 1 | 2 | KOI-O-0009 KOI-T-0009 |
| `InjectedParam` | ✓ | 3 | 2 | 1 | 6 | KOI-O-0003 KOI-R-0003 KOI-R-0023 |
| `Property` | ✓ | 2 | 0 | 1 | 3 | KOI-O-0010 KOI-T-0006 KOI-T-0009 |
| `PropertyValue` |  | 1 | 0 | 1 | 2 | KOI-O-0010 KOI-T-0009 |
| `Provided` |  | 1 | 1 | 0 | 2 | KOI-R-0002 KOI-T-0001 |
| `lazy-param` | ✓ | 3 | 2 | 1 | 6 | KOI-O-0014 KOI-R-0011 KOI-R-0028 |
| `list-param` | ✓ | 3 | 10 | 3 | 16 | KOI-O-0002 KOI-O-0013 KOI-O-0014 |
| `nullable-param` | ✓ | 0 | 0 | 1 | 1 | KOI-O-0003 |

### Koin のスコープ（`koin.scope`、対象: koin）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `Scope` | ✓ | 4 | 0 | 1 | 5 | KOI-O-0011 KOI-T-0001 KOI-T-0002 |
| `ScopeId` |  | 1 | 0 | 0 | 1 | KOI-T-0009 |
| `archetype` | ✓ | 1 | 1 | 1 | 3 | KOI-O-0011 KOI-R-0010 KOI-T-0001 |

### Koin のその他（`koin.other`、対象: koin）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `Monitor` |  | 0 | 1 | 0 | 1 | KOI-R-0007 |
| `jsr330-Inject` |  | 4 | 2 | 0 | 6 | KOI-R-0005 KOI-R-0011 KOI-T-0004 |
| `external-definition` |  | 0 | 0 | 0 | 0 |  |
| `meta` |  | 0 | 0 | 0 | 0 |  |
| `config-check-error` | ✓ | 0 | 0 | 1 | 1 | KOI-O-0004 |
| `cycle` |  | 1 | 0 | 0 | 1 | KOI-T-0008 |

### Koin のオプション（既定値以外）（`koin.option`、対象: koin）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `KOIN_CONFIG_CHECK` | ✓ | 9 | 12 | 1 | 22 | KOI-O-0004 KOI-R-0001 KOI-R-0002 |
| `KOIN_LOG_TIMES` |  | 8 | 1 | 0 | 9 | KOI-R-0003 KOI-T-0001 KOI-T-0002 |
| `KOIN_DEFAULT_MODULE` | ✓ | 3 | 0 | 1 | 4 | KOI-O-0001 KOI-T-0001 KOI-T-0008 |
| `KOIN_GENERATION_PACKAGE` | ✓ | 0 | 0 | 1 | 1 | KOI-O-0005 |
| `KOIN_USE_COMPOSE_VIEWMODEL` | ✓ | 0 | 0 | 1 | 1 | KOI-O-0006 |
| `KOIN_EXPORT_DEFINITIONS` |  | 0 | 0 | 0 | 0 |  |
