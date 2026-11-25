package fixture.android.app

import fixture.android.lib.LibModel
import me.tbsten.analysis.code.gen.tp.Mapped
import me.tbsten.analysis.code.gen.tp.Registered

/** android.jar の型を持つ。型表記に android.content.Context が出れば boot classpath を解析できている（GRD-12）。 */
@Mapped
@Registered
data class DeviceInfo(val sdk: Int, val context: android.content.Context?, val lib: LibModel)

fun describeDevice(): String = DeviceInfoMapper.toMap(DeviceInfo(android.os.Build.VERSION.SDK_INT, null, LibModel(1, null))).toString()
