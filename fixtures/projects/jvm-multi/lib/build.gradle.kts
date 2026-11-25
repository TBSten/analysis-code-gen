plugins {
    alias(libs.plugins.kotlin.jvm)
    id("me.tbsten.analysiscodegen")
}

analysisCodeGen {
    arg("tp.processors", "Mapper,Registry")
    arg("tp.registry.name", "LibRegistryHolder")
    arg("tp.registry.package", "fixture.multi.lib.generated")
    sourceSets {
        main { processor(acg.testProcessors.native) }
    }
}
