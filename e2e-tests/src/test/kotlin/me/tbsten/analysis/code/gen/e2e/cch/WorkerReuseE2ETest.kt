package me.tbsten.analysis.code.gen.e2e.cch

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.tbsten.analysis.code.gen.e2e.support.Acg45Engine
import me.tbsten.analysis.code.gen.e2e.support.acg45Project
import me.tbsten.analysis.code.gen.e2e.support.codegenTask
import me.tbsten.analysis.code.gen.e2e.support.mappedClass
import me.tbsten.analysis.code.gen.testfoundation.E2ESpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import org.gradle.testkit.runner.TaskOutcome

// 5.9 CCH-04。Worker の中の再利用は生成物では見えないため、--info のログ（ADR-039）で観察する

/** ADR-039 の 1 行: `analysisCodeGen[<unit>]: engineClassLoader=<id> reused=<true|false> worker=<classLoader|process>`。 */
private val engineLine =
    Regex("""analysisCodeGen\[(\w+)]: engineClassLoader=([0-9a-f]+) reused=(true|false) worker=(classLoader|process)""")

private data class EngineLog(
    val unit: String,
    val classLoaderId: String,
    val reused: Boolean,
    val worker: String,
)

private fun engineLogs(output: String): List<EngineLog> =
    engineLine
        .findAll(output)
        .map { m ->
            EngineLog(m.groupValues[1], m.groupValues[2], m.groupValues[3].toBoolean(), m.groupValues[4])
        }.toList()

class WorkerReuseE2ETest : E2ESpec({
    case("CCH-04", "2 回目のビルドで Worker と AA の ClassLoader を再利用する", pending = PendingCase(Milestone.M2)) {
        val task = codegenTask(Acg45Engine.Native)
        val fixture = acg45Project { kotlin("src/main/kotlin/com/example/A.kt", mappedClass("A")) }

        // 1 回目: daemon にエンジンの ClassLoader はまだ無い
        val first = fixture.build(task, "--info")
        first.task(task)?.outcome shouldBe TaskOutcome.SUCCESS
        val firstLogs = engineLogs(first.output)
        firstLogs shouldHaveSize 1
        firstLogs.single().unit shouldBe "main"
        firstLogs.single().reused shouldBe false
        firstLogs.single().worker shouldBe "classLoader"

        // 2 回目: 入力を変えてタスクを実行させる。TestKit は同じ daemon を使うので、同じ ClassLoader が再利用される
        fixture.kotlin("src/main/kotlin/com/example/B.kt", mappedClass("B"))
        val second = fixture.build(task, "--info")
        second.task(task)?.outcome shouldBe TaskOutcome.SUCCESS
        val secondLogs = engineLogs(second.output)
        secondLogs shouldHaveSize 1
        secondLogs.single().reused shouldBe true
        secondLogs.single().classLoaderId shouldBe firstLogs.single().classLoaderId
        secondLogs.single().worker shouldBe "classLoader"

        // 増分の判断も同じ書式で出る（ADR-039）。2 回目は B.kt だけが dirty
        val plan = Regex("""analysisCodeGen\[main]: plan=(full|incremental) reason=.* dirty=(\d+)""").findAll(second.output).toList()
        plan shouldHaveSize 1
        plan.single().groupValues[1] shouldBe "incremental"
        plan.single().groupValues[2] shouldNotBe "0"
    }
})
