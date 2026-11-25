package fixture.custom

import me.tbsten.analysis.code.gen.tp.GenerateActual

// commonMain には @Mapped を置かない（KSP2 版でターゲット側と二重に生成しないため。build.ksp.gradle.kts 参照）

@GenerateActual
expect fun platformName(): String
