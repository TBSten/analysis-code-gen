plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

ksp {
    arg("tp.processors", "Mapper,Registry")
    arg("tp.registry.name", "LibRegistryHolder")
    arg("tp.registry.package", "fixture.multi.lib.generated")
}

dependencies {
    ksp(acg.testProcessors.ksp)
}
