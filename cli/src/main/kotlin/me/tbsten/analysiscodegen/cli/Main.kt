package me.tbsten.analysiscodegen.cli

/**
 * `analysis-code-gen --model <project-model.json> --unit <name> [--full]`
 *
 * 終了コード: 0 = 成功、1 = Processor のエラー、2 = 引数・モデルの誤り、3 = 内部エラー（CLI-03）。
 * 診断は標準エラーに `<severity>: <file>:<line>: <message>` の形で出す。
 */
public fun main(args: Array<String>): Unit = TODO()
