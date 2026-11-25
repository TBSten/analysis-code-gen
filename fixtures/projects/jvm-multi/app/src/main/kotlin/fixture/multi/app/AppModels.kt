package fixture.multi.app

import fixture.multi.lib.LibService
import fixture.multi.lib.LibUser
import fixture.multi.lib.LibUserMapper
import fixture.multi.lib.generated.LibRegistryHolder
import me.tbsten.analysis.code.gen.tp.DeferUntil
import me.tbsten.analysis.code.gen.tp.Mapped
import me.tbsten.analysis.code.gen.tp.Registered

/** :lib の型をプロパティに持つ（GRD-09: :app の Processor が :lib の宣言を解析する）。 */
@Mapped
@Registered
data class AppUser(val lib: LibUser, val nickname: String?)

/** :lib の生成物（LibUserMapper）がクラスパスにあるので、ラウンド 1 で解決される（GRD-10）。 */
@DeferUntil("fixture.multi.lib.LibUserMapper")
class NeedsLibMapper

@Registered
class AppService : LibService {
    override fun greet(user: LibUser): String = "hello ${LibUserMapper.toMap(user)["name"]}"
}

fun describe(): List<String> =
    listOf(
        AppUserMapper.toMap(AppUser(LibUser(1, "a"), null)).toString(),
        NeedsLibMapperResolved.targetName,
        LibRegistryHolder.entries.joinToString(),
    )
