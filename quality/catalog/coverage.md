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
| common | 0 | 0 | 10 | 10 | 7 |
| cream | 511 | 78 | 4 | 593 | 511 |
| moshi | 135 | 38 | 5 | 178 | 138 |
| koin | 9 | 53 | 14 | 76 | 17 |
| **計** | 655 | 169 | 33 | 857 | 673 |

「K で確認済み」は、KSP2 でビルドして EXPECT どおりだったパターン（`:e2e-tests:verifyCatalog` と `catalog.py apply-k` が付ける。ADR-072）。

## 2. 必須の値の網羅: 125 / 125

必須の値はすべて 1 件以上のパターンで覆われています。

1〜2 件しかない必須の値（薄いところ）: `proc.combo=multi`、`cream.family=options`、`moshi.option=moshi.generated`、`moshi.option=moshi.generateProguardRules`、`moshi.option=moshi.instantiateAnnotations`、`koin.definition=KoinViewModel`、`koin.module=KoinApplication`、`koin.module=default-module`、`koin.binding=auto-bind-interface`、`koin.binding=explicit-binds`、`koin.binding=Qualifier`、`koin.binding=InjectedParam`、`koin.binding=Property`、`koin.binding=lazy-param`、`koin.binding=nullable-param`、`koin.scope=archetype`、`koin.other=config-check-error`、`koin.option=KOIN_DEFAULT_MODULE`、`koin.option=KOIN_GENERATION_PACKAGE`、`koin.option=KOIN_USE_COMPOSE_VIEWMODEL`

K で確認済み（verified）だけで数えると: 114 / 125（M1 以降の合格条件。ADR-020 の 6 項、ADR-072）

verified のパターンがまだ無い必須の値: `kt.misc=kdoc`、`kt.layout=kmp-common`、`kt.layout=kmp-platform`、`kt.layout=android`、`koin.definition=Scoped`、`koin.definition=KoinViewModel`、`koin.binding=explicit-binds`、`koin.binding=lazy-param`、`koin.scope=Scope`、`koin.scope=archetype`、`koin.option=KOIN_USE_COMPOSE_VIEWMODEL`

## 3. 次元ごとの網羅

### 宣言の種類（`kt.decl`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `class` | ✓ | 141 | 22 | 22 | 185 | CMN-O-0001 CMN-O-0002 CMN-O-0003 |
| `data-class` | ✓ | 533 | 89 | 10 | 632 | CRM-O-0001 CRM-O-0002 CRM-O-0003 |
| `value-class` | ✓ | 55 | 5 | 2 | 62 | CRM-O-0003 CRM-R-0029 CRM-R-0040 |
| `enum-class` | ✓ | 8 | 4 | 1 | 13 | CMN-O-0008 CRM-T-0037 CRM-T-0086 |
| `sealed-class` | ✓ | 8 | 7 | 1 | 16 | CRM-R-0001 CRM-R-0065 CRM-R-0067 |
| `sealed-interface` | ✓ | 178 | 16 | 3 | 197 | CRM-O-0002 CRM-O-0003 CRM-O-0004 |
| `interface` | ✓ | 18 | 3 | 8 | 29 | CMN-O-0002 CMN-O-0004 CMN-O-0006 |
| `fun-interface` |  | 0 | 1 | 1 | 2 | CMN-O-0003 KOI-R-0043 |
| `abstract-class` | ✓ | 12 | 1 | 0 | 13 | CRM-T-0036 CRM-T-0085 CRM-T-0132 |
| `object` | ✓ | 164 | 19 | 2 | 185 | CMN-O-0001 CMN-O-0005 CRM-R-0008 |
| `data-object` |  | 12 | 12 | 1 | 25 | CRM-O-0003 CRM-R-0001 CRM-R-0002 |
| `companion-object` | ✓ | 1 | 5 | 1 | 7 | KOI-R-0001 KOI-R-0002 KOI-R-0033 |
| `annotation-class` |  | 9 | 2 | 2 | 13 | CMN-O-0008 CRM-R-0050 CRM-T-0082 |
| `typealias` | ✓ | 9 | 8 | 1 | 18 | CMN-O-0003 CRM-R-0011 CRM-R-0019 |
| `top-level-fun` | ✓ | 2 | 4 | 4 | 10 | CMN-O-0003 CMN-O-0004 CMN-O-0006 |
| `top-level-property` |  | 1 | 5 | 3 | 9 | CMN-O-0003 CMN-O-0005 CRM-R-0002 |
| `extension-fun` |  | 0 | 6 | 0 | 6 | KOI-R-0001 KOI-R-0033 KOI-R-0034 |
| `extension-property` |  | 2 | 2 | 0 | 4 | CRM-R-0002 CRM-T-0009 CRM-T-0415 |
| `local-class` |  | 0 | 0 | 2 | 2 | CMN-O-0003 CMN-O-0004 |
| `object-expression` |  | 1 | 0 | 2 | 3 | CMN-O-0003 KOI-O-0012 MSH-T-0013 |

### 可視性（`kt.vis`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `public-implicit` | ✓ | 143 | 108 | 28 | 279 | CMN-O-0001 CMN-O-0003 CMN-O-0004 |
| `public-explicit` |  | 490 | 0 | 1 | 491 | CMN-O-0002 CRM-T-0003 CRM-T-0004 |
| `internal` | ✓ | 26 | 13 | 2 | 41 | CMN-O-0006 CRM-R-0041 CRM-R-0062 |
| `private` | ✓ | 28 | 23 | 14 | 65 | CMN-O-0002 CMN-O-0004 CMN-O-0006 |
| `protected` |  | 1 | 0 | 0 | 1 | CRM-T-0361 |

### 入れ子（`kt.nest`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `nested` | ✓ | 183 | 16 | 6 | 205 | CMN-O-0003 CMN-O-0004 CRM-O-0002 |
| `inner` |  | 7 | 0 | 1 | 8 | CMN-O-0004 CRM-T-0038 CRM-T-0087 |
| `deep-nested` | ✓ | 5 | 2 | 1 | 8 | CMN-O-0004 CRM-R-0038 CRM-R-0073 |

### ジェネリクス（`kt.generics`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `type-param` | ✓ | 45 | 8 | 2 | 55 | CMN-O-0004 CRM-R-0005 CRM-R-0022 |
| `upper-bound` | ✓ | 17 | 1 | 1 | 19 | CMN-O-0004 CRM-T-0051 CRM-T-0096 |
| `multiple-bounds` |  | 3 | 1 | 1 | 5 | CMN-O-0004 CRM-R-0072 CRM-T-0453 |
| `variance-out` | ✓ | 3 | 1 | 3 | 7 | CMN-O-0003 CMN-O-0004 CRM-R-0064 |
| `variance-in` |  | 1 | 0 | 2 | 3 | CMN-O-0003 CMN-O-0004 MSH-T-0035 |
| `star-projection` | ✓ | 1 | 1 | 3 | 5 | CMN-O-0004 CMN-O-0008 KOI-R-0042 |
| `recursive-bound` |  | 11 | 0 | 1 | 12 | CMN-O-0004 CRM-T-0051 CRM-T-0096 |
| `nested-generic` | ✓ | 16 | 4 | 5 | 25 | CMN-O-0002 CMN-O-0004 CMN-O-0008 |
| `reified` |  | 0 | 0 | 1 | 1 | CMN-O-0004 |

### 型の形（`kt.types`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `nullable` | ✓ | 53 | 20 | 8 | 81 | CMN-O-0004 CMN-O-0006 CRM-O-0004 |
| `list` | ✓ | 24 | 16 | 10 | 50 | CMN-O-0002 CMN-O-0003 CMN-O-0004 |
| `set` |  | 3 | 3 | 2 | 8 | CMN-O-0004 CRM-R-0032 CRM-R-0052 |
| `map` | ✓ | 9 | 6 | 3 | 18 | CMN-O-0004 CMN-O-0006 CRM-R-0032 |
| `array` |  | 10 | 3 | 1 | 14 | CMN-O-0008 CRM-R-0078 CRM-T-0200 |
| `function-type` | ✓ | 1 | 5 | 2 | 8 | CMN-O-0003 CMN-O-0004 KOI-R-0033 |
| `primitive` | ✓ | 282 | 72 | 19 | 373 | CMN-O-0001 CMN-O-0003 CMN-O-0004 |
| `unsigned` |  | 0 | 1 | 0 | 1 | KOI-R-0033 |
| `string` | ✓ | 499 | 90 | 23 | 612 | CMN-O-0001 CMN-O-0003 CMN-O-0004 |
| `platform-type` |  | 1 | 0 | 2 | 3 | CMN-O-0002 MSH-O-0003 MSH-T-0021 |

### コンストラクタ・引数（`kt.ctor`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `primary` | ✓ | 594 | 94 | 26 | 714 | CMN-O-0001 CMN-O-0002 CMN-O-0003 |
| `no-arg` |  | 64 | 20 | 17 | 101 | CMN-O-0003 CMN-O-0007 CMN-O-0008 |
| `secondary` |  | 4 | 1 | 0 | 5 | CRM-T-0199 CRM-T-0247 CRM-T-0372 |
| `private-ctor` |  | 8 | 0 | 0 | 8 | CRM-T-0041 CRM-T-0090 CRM-T-0139 |
| `default-arg` | ✓ | 64 | 16 | 18 | 98 | CMN-O-0001 CMN-O-0003 CMN-O-0006 |
| `vararg` |  | 9 | 2 | 0 | 11 | CRM-R-0078 CRM-T-0200 CRM-T-0201 |
| `non-property-param` |  | 12 | 5 | 0 | 17 | CRM-R-0001 CRM-R-0073 CRM-T-0014 |

### プロパティ（`kt.prop`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `val` | ✓ | 541 | 97 | 29 | 667 | CMN-O-0001 CMN-O-0002 CMN-O-0003 |
| `var` | ✓ | 58 | 4 | 1 | 63 | CMN-O-0008 CRM-T-0444 KOI-R-0033 |
| `lateinit` |  | 1 | 0 | 1 | 2 | CMN-O-0008 CRM-T-0444 |
| `delegated` |  | 2 | 3 | 1 | 6 | CMN-O-0008 CRM-T-0442 KOI-R-0033 |
| `const` |  | 0 | 5 | 2 | 7 | CMN-O-0005 KOI-R-0001 KOI-R-0033 |
| `custom-getter` |  | 13 | 5 | 0 | 18 | CRM-R-0002 CRM-T-0009 CRM-T-0304 |
| `body-property` |  | 37 | 10 | 7 | 54 | CMN-O-0007 CMN-O-0008 CMN-O-0009 |
| `jvm-field` |  | 0 | 0 | 0 | 0 |  |
| `override` |  | 82 | 18 | 5 | 105 | CMN-O-0004 CRM-O-0002 CRM-O-0003 |

### アノテーションの書き方（`kt.annot`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `use-site-target` | ✓ | 1 | 3 | 2 | 6 | CMN-O-0005 CMN-O-0008 MSH-R-0013 |
| `array-arg` | ✓ | 71 | 11 | 2 | 84 | CMN-O-0008 CRM-R-0013 CRM-R-0014 |
| `class-literal-arg` | ✓ | 338 | 66 | 7 | 411 | CRM-O-0001 CRM-O-0002 CRM-O-0004 |
| `named-arg` | ✓ | 218 | 28 | 9 | 255 | CMN-O-0008 CRM-R-0001 CRM-R-0013 |
| `repeated` |  | 12 | 9 | 2 | 23 | CMN-O-0008 CRM-R-0001 CRM-R-0073 |
| `meta-annotation` |  | 5 | 1 | 2 | 8 | CMN-O-0008 KOI-O-0009 KOI-R-0036 |
| `fqn-annotation` |  | 0 | 0 | 0 | 0 |  |

### その他の言語要素（`kt.misc`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `kdoc` | ✓ | 0 | 25 | 0 | 25 | CRM-R-0005 CRM-R-0006 CRM-R-0007 |
| `deprecated` |  | 10 | 0 | 1 | 11 | CMN-O-0003 CRM-T-0173 CRM-T-0406 |
| `backtick-name` |  | 120 | 3 | 1 | 124 | CMN-O-0005 CRM-R-0054 CRM-R-0055 |
| `suspend` |  | 0 | 10 | 1 | 11 | CMN-O-0003 KOI-R-0001 KOI-R-0033 |
| `expect-actual` |  | 0 | 1 | 2 | 3 | CMN-O-0001 KOI-O-0012 KOI-R-0035 |
| `context-parameters` |  | 0 | 0 | 1 | 1 | CMN-O-0003 |
| `inheritance` | ✓ | 177 | 31 | 10 | 218 | CMN-O-0001 CMN-O-0003 CMN-O-0004 |
| `opt-in` |  | 0 | 2 | 0 | 2 | KOI-R-0033 KOI-R-0043 |
| `jvm-name` |  | 0 | 0 | 1 | 1 | CMN-O-0005 |

### ファイル・モジュールの配置（`kt.layout`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `single-file` | ✓ | 632 | 111 | 18 | 761 | CMN-O-0003 CMN-O-0004 CMN-O-0005 |
| `multi-file` | ✓ | 4 | 0 | 12 | 16 | CMN-O-0001 CMN-O-0002 CMN-O-0006 |
| `multi-package` | ✓ | 4 | 0 | 4 | 8 | CMN-O-0006 CRM-O-0001 KOI-O-0007 |
| `java-source` | ✓ | 1 | 0 | 2 | 3 | CMN-O-0002 MSH-O-0003 MSH-T-0021 |
| `default-package` |  | 0 | 0 | 1 | 1 | CMN-O-0005 |
| `multi-module` | ✓ | 0 | 0 | 4 | 4 | CMN-O-0006 CRM-O-0001 KOI-O-0007 |
| `kmp-common` | ✓ | 0 | 90 | 5 | 95 | CMN-O-0001 CRM-O-0001 CRM-O-0002 |
| `kmp-intermediate` |  | 0 | 0 | 3 | 3 | CMN-O-0001 CRM-O-0002 KOI-O-0012 |
| `kmp-platform` | ✓ | 3 | 2 | 6 | 11 | CMN-O-0001 CMN-O-0009 CRM-O-0002 |
| `android` | ✓ | 3 | 5 | 3 | 11 | CMN-O-0009 KOI-O-0006 KOI-O-0011 |
| `test-source-set` |  | 0 | 0 | 1 | 1 | CMN-O-0010 |

### 期待する結果（`kt.outcome`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `ok` | ✓ | 546 | 111 | 28 | 685 | CMN-O-0001 CMN-O-0002 CMN-O-0003 |
| `processor-error` | ✓ | 90 | 0 | 1 | 91 | CRM-T-0001 CRM-T-0002 CRM-T-0036 |
| `processor-warning` |  | 0 | 0 | 0 | 0 |  |
| `compile-error` |  | 0 | 0 | 1 | 1 | CMN-O-0007 |

### Processor の組み合わせ（`proc.combo`、対象: all）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `single` | ✓ | 636 | 110 | 19 | 765 | CRM-O-0001 CRM-O-0002 CRM-O-0003 |
| `multi` | ✓ | 0 | 1 | 1 | 2 | KOI-O-0013 MSH-R-0001 |

### cream.kt のアノテーション（`cream.annotation`、対象: cream）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `CopyTo` | ✓ | 87 | 17 | 4 | 108 | CRM-O-0001 CRM-O-0002 CRM-O-0004 |
| `CopyFrom` | ✓ | 52 | 13 | 1 | 66 | CRM-O-0002 CRM-R-0031 CRM-R-0032 |
| `CopyToChildren` | ✓ | 47 | 8 | 3 | 58 | CRM-O-0002 CRM-O-0003 CRM-O-0004 |
| `CombineTo` | ✓ | 50 | 12 | 0 | 62 | CRM-R-0020 CRM-R-0021 CRM-R-0022 |
| `CombineFrom` | ✓ | 48 | 11 | 0 | 59 | CRM-R-0003 CRM-R-0004 CRM-R-0005 |
| `CopyMapping` | ✓ | 56 | 7 | 0 | 63 | CRM-R-0042 CRM-R-0043 CRM-R-0044 |
| `CombineMapping` | ✓ | 45 | 7 | 0 | 52 | CRM-R-0013 CRM-R-0014 CRM-R-0015 |
| `SealedCopy` | ✓ | 45 | 5 | 0 | 50 | CRM-R-0071 CRM-R-0074 CRM-R-0075 |
| `ParentOptional` | ✓ | 58 | 3 | 1 | 62 | CRM-O-0004 CRM-R-0001 CRM-R-0002 |
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
| `invalidUsage` | ✓ | 20 | 0 | 0 | 20 | CRM-T-0001 CRM-T-0002 CRM-T-0413 |
| `options` | ✓ | 0 | 0 | 1 | 1 | CRM-O-0003 |

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
| `JsonClass` | ✓ | 123 | 13 | 5 | 141 | KOI-O-0013 MSH-O-0002 MSH-O-0003 |
| `JsonClass-generator` | ✓ | 3 | 0 | 0 | 3 | MSH-T-0005 MSH-T-0091 MSH-T-0092 |
| `JsonClass-inline` |  | 0 | 0 | 0 | 0 |  |
| `Json-name` | ✓ | 11 | 12 | 3 | 26 | KOI-O-0013 MSH-O-0002 MSH-O-0005 |
| `Json-ignore` | ✓ | 1 | 0 | 2 | 3 | MSH-O-0002 MSH-O-0003 MSH-T-0015 |
| `Transient` | ✓ | 9 | 0 | 1 | 10 | MSH-O-0002 MSH-T-0014 MSH-T-0026 |
| `JsonQualifier` | ✓ | 4 | 0 | 0 | 4 | MSH-T-0026 MSH-T-0063 MSH-T-0065 |
| `ToJson-FromJson` |  | 0 | 1 | 0 | 1 | MSH-R-0001 |
| `generateAdapter-false` |  | 2 | 1 | 0 | 3 | MSH-R-0004 MSH-T-0054 MSH-T-0101 |

### Moshi で効く入力の形（`moshi.shape`、対象: moshi）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `default-values` | ✓ | 103 | 0 | 0 | 103 | MSH-T-0027 MSH-T-0028 MSH-T-0029 |
| `generic-class` | ✓ | 10 | 0 | 0 | 10 | MSH-T-0027 MSH-T-0041 MSH-T-0042 |
| `inheritance` | ✓ | 11 | 0 | 0 | 11 | MSH-T-0038 MSH-T-0039 MSH-T-0040 |
| `many-properties` | ✓ | 6 | 0 | 0 | 6 | MSH-T-0035 MSH-T-0052 MSH-T-0072 |
| `nullable-props` | ✓ | 21 | 0 | 0 | 21 | MSH-T-0029 MSH-T-0031 MSH-T-0035 |
| `nested-adapter` | ✓ | 86 | 0 | 0 | 86 | MSH-T-0032 MSH-T-0044 MSH-T-0045 |
| `unsupported-kind` | ✓ | 10 | 0 | 0 | 10 | MSH-T-0004 MSH-T-0006 MSH-T-0007 |
| `private-api` | ✓ | 5 | 0 | 0 | 5 | MSH-T-0001 MSH-T-0002 MSH-T-0003 |

### Moshi のオプション（既定値以外）（`moshi.option`、対象: moshi）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `moshi.generated` | ✓ | 0 | 0 | 1 | 1 | MSH-O-0002 |
| `moshi.generateProguardRules` | ✓ | 1 | 0 | 1 | 2 | MSH-O-0002 MSH-T-0018 |
| `moshi.instantiateAnnotations` | ✓ | 0 | 0 | 1 | 1 | MSH-O-0005 |

### Koin の定義アノテーション（`koin.definition`、対象: koin）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `Single` | ✓ | 3 | 11 | 9 | 23 | KOI-O-0001 KOI-O-0002 KOI-O-0004 |
| `Singleton` |  | 0 | 0 | 0 | 0 |  |
| `Factory` | ✓ | 3 | 1 | 4 | 8 | KOI-O-0001 KOI-O-0003 KOI-O-0007 |
| `Scoped` | ✓ | 2 | 0 | 1 | 3 | KOI-O-0011 KOI-T-0001 KOI-T-0002 |
| `KoinViewModel` | ✓ | 1 | 0 | 1 | 2 | KOI-O-0006 KOI-T-0001 |
| `KoinWorker` |  | 0 | 0 | 0 | 0 |  |
| `function-definition` | ✓ | 2 | 3 | 2 | 7 | KOI-O-0012 KOI-O-0013 KOI-R-0002 |
| `class-definition` | ✓ | 4 | 8 | 12 | 24 | KOI-O-0001 KOI-O-0002 KOI-O-0003 |

### Koin のモジュール構成（`koin.module`、対象: koin）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `Module` | ✓ | 4 | 8 | 11 | 23 | KOI-O-0002 KOI-O-0003 KOI-O-0004 |
| `ComponentScan` | ✓ | 4 | 8 | 11 | 23 | KOI-O-0002 KOI-O-0003 KOI-O-0004 |
| `ComponentScan-package` | ✓ | 1 | 5 | 4 | 10 | KOI-O-0002 KOI-O-0006 KOI-O-0007 |
| `includes` | ✓ | 2 | 0 | 1 | 3 | KOI-O-0007 KOI-T-0001 KOI-T-0002 |
| `Configuration` | ✓ | 4 | 1 | 0 | 5 | KOI-R-0012 KOI-T-0001 KOI-T-0002 |
| `KoinApplication` | ✓ | 2 | 0 | 0 | 2 | KOI-T-0001 KOI-T-0007 |
| `createdAtStart` |  | 0 | 1 | 0 | 1 | KOI-R-0035 |
| `default-module` | ✓ | 0 | 0 | 1 | 1 | KOI-O-0001 |

### Koin のバインド・引数（`koin.binding`、対象: koin）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `auto-bind-interface` | ✓ | 0 | 0 | 1 | 1 | KOI-O-0002 |
| `explicit-binds` | ✓ | 0 | 1 | 0 | 1 | KOI-R-0037 |
| `Named` | ✓ | 2 | 1 | 1 | 4 | KOI-O-0009 KOI-R-0036 KOI-T-0001 |
| `Qualifier` | ✓ | 0 | 0 | 1 | 1 | KOI-O-0009 |
| `InjectedParam` | ✓ | 1 | 0 | 1 | 2 | KOI-O-0003 KOI-T-0001 |
| `Property` | ✓ | 0 | 0 | 1 | 1 | KOI-O-0010 |
| `PropertyValue` |  | 0 | 0 | 1 | 1 | KOI-O-0010 |
| `Provided` |  | 1 | 1 | 0 | 2 | KOI-R-0002 KOI-T-0001 |
| `lazy-param` | ✓ | 1 | 0 | 0 | 1 | KOI-T-0002 |
| `list-param` | ✓ | 0 | 3 | 2 | 5 | KOI-O-0002 KOI-O-0013 KOI-R-0033 |
| `nullable-param` | ✓ | 0 | 0 | 1 | 1 | KOI-O-0003 |

### Koin のスコープ（`koin.scope`、対象: koin）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `Scope` | ✓ | 2 | 0 | 1 | 3 | KOI-O-0011 KOI-T-0001 KOI-T-0002 |
| `ScopeId` |  | 0 | 0 | 0 | 0 |  |
| `archetype` | ✓ | 1 | 0 | 1 | 2 | KOI-O-0011 KOI-T-0001 |

### Koin のその他（`koin.other`、対象: koin）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `Monitor` |  | 0 | 0 | 0 | 0 |  |
| `jsr330-Inject` |  | 0 | 0 | 0 | 0 |  |
| `external-definition` |  | 0 | 0 | 0 | 0 |  |
| `meta` |  | 0 | 0 | 0 | 0 |  |
| `config-check-error` | ✓ | 0 | 0 | 1 | 1 | KOI-O-0004 |
| `cycle` |  | 0 | 0 | 0 | 0 |  |

### Koin のオプション（既定値以外）（`koin.option`、対象: koin）

| 値 | 必須 | ① | ② | ③ | 計 | 例 |
|---|:-:|---:|---:|---:|---:|---|
| `KOIN_CONFIG_CHECK` | ✓ | 4 | 3 | 1 | 8 | KOI-O-0004 KOI-R-0001 KOI-R-0002 |
| `KOIN_LOG_TIMES` |  | 4 | 0 | 0 | 4 | KOI-T-0001 KOI-T-0002 KOI-T-0003 |
| `KOIN_DEFAULT_MODULE` | ✓ | 1 | 0 | 1 | 2 | KOI-O-0001 KOI-T-0001 |
| `KOIN_GENERATION_PACKAGE` | ✓ | 0 | 0 | 1 | 1 | KOI-O-0005 |
| `KOIN_USE_COMPOSE_VIEWMODEL` | ✓ | 0 | 0 | 1 | 1 | KOI-O-0006 |
| `KOIN_EXPORT_DEFINITIONS` |  | 0 | 0 | 0 | 0 |  |
