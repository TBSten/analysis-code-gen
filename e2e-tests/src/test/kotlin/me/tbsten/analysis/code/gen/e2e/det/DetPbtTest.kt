package me.tbsten.analysis.code.gen.e2e.det

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import me.tbsten.analysis.code.gen.e2e.support.Fixture
import me.tbsten.analysis.code.gen.e2e.support.fixtureProject
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PbtSpec
import me.tbsten.analysis.code.gen.testfoundation.PendingCase

/** ADR-038: 順番の入れ替えはエンジンの予約オプション（Processor には渡らない）で行う。値は乱数の seed。 */
private const val SHUFFLE_SEED = "analysisCodeGen.internal.shuffleSeed"

// 5.8 決定性（DET）の PBT。1 試行ごとに Gradle ビルドを流すので試行数は少なくし、毎時の PBT（ADR-022）で数を稼ぐ
class DetPbtTest : PbtSpec({
    case("DET-02", "Processor の登録順・ファイルの列挙順・ラウンド内の実行順をランダムに入れ替えても生成物が一致する", PendingCase(Milestone.M2)) {
        val baseline = fixtureProject(Fixture.JVM).also { it.build("analysisCodeGenMain") }.generated("main").files()
        checkAll(PropTestConfig(iterations = 5), Arb.long()) { seed ->
            val shuffled =
                fixtureProject(Fixture.JVM) {
                    edit("build.gradle.kts") { it + "\nanalysisCodeGen { arg(\"$SHUFFLE_SEED\", \"$seed\") }\n" }
                }
            shuffled.build("analysisCodeGenMain")
            shuffled.generated("main").files() shouldBe baseline
        }
    }
})
