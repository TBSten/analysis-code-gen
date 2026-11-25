package fixture.android.lib

import me.tbsten.analysiscodegen.tp.Mapped

@Mapped
data class LibModel(val id: Int, val uri: android.net.Uri?)
