package fixture.kmp

import me.tbsten.analysiscodegen.tp.Chain
import me.tbsten.analysiscodegen.tp.DeferUntil
import me.tbsten.analysiscodegen.tp.GenerateActual
import me.tbsten.analysiscodegen.tp.Mapped

/** commonMain に 1 回だけ SharedMapper を作り、全ターゲットから使う（KMP-01）。 */
@Mapped
data class Shared(val id: Int, val label: String, val tags: List<String>)

/** 各 Compilation で actual を作る（KMP-03）。 */
@GenerateActual
expect fun platformName(): String

@Chain(depth = 2)
class Step

@DeferUntil("fixture.kmp.StepChain2")
class AfterSteps

fun describe(): String = "${platformName()}:${SharedMapper.toMap(Shared(1, "a", emptyList()))}:${AfterStepsResolved.targetName}"
