package me.tbsten.analysis.code.gen.kspcompatibilitytests.kspt

import io.kotest.core.spec.style.FreeSpec

/**
 * 取り込んだ KSPT が公式 KSP2（K）で実際に通ることを確かめる補助テスト（ケース ID なし。進捗に数えない、ADR-024）。
 *
 * 全件は数分かかるため、既定では M ごとの先頭 1 件だけを流す。全件は `-Pacg.kspt.k=true`（夜間の定期実行）、
 * 1 件だけなら `-Pacg.kspt.only=KSPT-Hello`。期待値は JDK 17 前提なので、JDK 17 の場所を `-Pacg.kspt.jdkHome` か
 * 環境変数 `JAVA_HOME_17_X64` で渡す（無いと [KsptCases.jdk17Only] を流さない）。
 */
class KsptKsp2Test :
    FreeSpec({
        val only =
            System
                .getProperty("acg.kspt.only")
                ?.split(',')
                ?.map { it.trim() }
                ?.toSet()
        val all = System.getProperty("acg.kspt.k") == "true"
        val smoke =
            KsptCases.all
                .groupBy { it.milestone }
                .values
                .map { it.first().id }
                .toSet()
        // 流さないものは登録しない（無効化の印を使わない。test_guard の対象外にするため）
        val selected =
            KsptCases.all.filter { c ->
                when {
                    only != null -> c.id in only
                    all -> true
                    else -> c.id in smoke
                }
            }
        for (c in selected) {
            if (c.id in KsptCases.jdk17Only && !Ksp2KsptRunner.isJdk17) {
                println("[K] ${c.id} は JDK 17 が無いため流さない（acg.kspt.jdkHome か JAVA_HOME_17_X64 を渡す）")
                continue
            }
            "[K] ${c.id}" {
                KsptCases.assertPasses(c, KsptEngine.Ksp2)
            }
        }
    })
