package fixture.jvm

import me.tbsten.analysiscodegen.tp.CallGraph

/** TP-Body が関数本体の呼び出し先を解決する（Native 版だけ）。 */
class Service {
    @CallGraph
    fun run(): String {
        val user = User(1, "alice")
        return format(user)
    }

    private fun format(user: User): String = user.name.uppercase()
}

@CallGraph
fun topLevel() {
    println(helper())
}

fun helper(): Int = 42
