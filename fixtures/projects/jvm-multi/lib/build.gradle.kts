plugins {
    alias(libs.plugins.kotlin.jvm)
    id("me.tbsten.analysis.code.gen")
}

analysisCodeGen {
    arg("tp.processors", "Mapper,Registry")
    arg("tp.registry.name", "LibRegistryHolder")
    arg("tp.registry.package", "fixture.multi.lib.generated")
    sourceSets {
        main { processor(acg.testProcessors.native) }
    }
}
