package me.tbsten.analysis.code.gen.differential

import me.tbsten.analysis.code.gen.testprocessors.TpAnnotationsSource
import me.tbsten.analysis.code.gen.testprocessors.ksp.BadKspProvider
import me.tbsten.analysis.code.gen.testprocessors.ksp.ChainKspProvider
import me.tbsten.analysis.code.gen.testprocessors.ksp.MapperKspProvider
import me.tbsten.analysis.code.gen.testprocessors.ksp.RegistryKspProvider
import me.tbsten.analysis.code.gen.testprocessors.ksp.ResourceKspProvider

/** テスト用 Processor（TP-*、ADR-027）を使った固定の入力。 */
object TpInputs {
    val annotations = DiffSource("me/tbsten/analysis/code/gen/tp/${TpAnnotationsSource.FILE_NAME}", TpAnnotationsSource.content)

    val mapper = DiffProcessor("TP-Mapper", "test", ksp = { MapperKspProvider() })
    val registry = DiffProcessor("TP-Registry", "test", ksp = { RegistryKspProvider() })
    val chain = DiffProcessor("TP-Chain", "test", ksp = { ChainKspProvider() })
    val resource = DiffProcessor("TP-Resource", "test", ksp = { ResourceKspProvider() })
    val bad = DiffProcessor("TP-Bad", "test", ksp = { BadKspProvider() })

    /**
     * XD-03 の固定入力: 4 つの TP を一緒に動かし、isolating（Mapper）・aggregating（Registry）・複数ラウンド（Chain・Resource）・
     * Java とリソース（Resource）の出力がそろう 1 件。
     */
    fun fixed(): DiffInput =
        DiffInput(
            name = "xd-03-fixed",
            sources =
                listOf(
                    annotations,
                    DiffSource(
                        "com/example/User.kt",
                        """
                        package com.example

                        import me.tbsten.analysis.code.gen.tp.Mapped
                        import me.tbsten.analysis.code.gen.tp.Registered

                        @Mapped
                        @Registered
                        class User(val id: Long, val name: String?)
                        """.trimIndent(),
                    ),
                    DiffSource(
                        "com/example/Pipeline.kt",
                        """
                        package com.example

                        import me.tbsten.analysis.code.gen.tp.Chain
                        import me.tbsten.analysis.code.gen.tp.GenerateJava

                        @Chain(depth = 2)
                        class Pipeline

                        @GenerateJava
                        class Report
                        """.trimIndent(),
                    ),
                ),
            processors = listOf(mapper, registry, chain, resource),
            options = mapOf("tp.processors" to "Mapper,Registry,Chain,Resource"),
            expectation = Expectation.Valid,
        )
}
