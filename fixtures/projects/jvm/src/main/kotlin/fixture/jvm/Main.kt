package fixture.jvm

import me.tbsten.analysis.code.gen.tp.generated.TpRegistry

/** 生成物を使う。KSP 版がある TP の生成物だけを参照する（両方のエンジンで同じソースをビルドするため）。 */
fun describe(): List<String> =
    listOf(
        UserMapper.toMap(User(1, "alice")).toString(),
        Order_LineMapper.toMap(Order.Line("x", 2)).toString(),
        TpRegistry.entries.joinToString(),
        PipelineChain3(PipelineChain2(PipelineChain1(Pipeline()))).step.toString(),
        WaitsForChainResolved.targetName,
        ReportJavaRef.name,
    )

fun main() {
    describe().forEach(::println)
}
