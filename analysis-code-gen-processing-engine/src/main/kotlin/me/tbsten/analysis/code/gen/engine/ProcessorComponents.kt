@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.engine

import me.tbsten.analysis.code.gen.engine.spi.ProcessorComponent
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import java.io.File

/**
 * Gradle の component 1 つ（モデルの `ProcessorModel`）。[classpath] の `META-INF/services` から Provider を探す。
 *
 * クラスは親（エンジンの ClassLoader）を先に探すので、processor-api などエンジンと共有する型は同じ Class になる。
 * サービスの一覧は [classpath] の中だけを見る（エンジンやテストの classpath にある登録を拾わないため）。
 */
@InternalAnalysisCodegenApi
public class ClasspathProcessorComponent(
    override val coordinates: String,
    public val classpath: List<File>,
    parent: ClassLoader = ClasspathProcessorComponent::class.java.classLoader,
) : ProcessorComponent {
    private val urls = classpath.map { it.toURI().toURL() }.toTypedArray()

    override val classLoader: ClassLoader = java.net.URLClassLoader(urls, parent)

    /** 親を持たない、サービスの登録を読むためだけの ClassLoader。 */
    private val resourceLoader = java.net.URLClassLoader(urls, null)

    /** `META-INF/services/<serviceName>` に書かれた実装クラス名（classpath の順、重複なし）。 */
    public fun serviceClassNames(serviceName: String): List<String> =
        resourceLoader
            .getResources("META-INF/services/$serviceName")
            .toList()
            .flatMap { url -> url.openStream().bufferedReader().use { it.readLines() } }
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    override fun hasService(serviceName: String): Boolean = serviceClassNames(serviceName).isNotEmpty()

    override fun <T : Any> loadServices(service: Class<T>): List<T> =
        serviceClassNames(service.name).map { className ->
            Class
                .forName(className, true, classLoader)
                .asSubclass(service)
                .getDeclaredConstructor()
                .newInstance()
        }

    override fun toString(): String = "ClasspathProcessorComponent($coordinates)"
}

/**
 * Provider のインスタンスをそのまま渡す component。testing の `native(provider)` / `ksp(provider)` が使う（ADR-061）。
 * ServiceLoader を通さないので、テストのクロージャがそのまま動く。
 */
@InternalAnalysisCodegenApi
public class InstanceProcessorComponent(
    override val coordinates: String,
    public val instances: List<Any>,
) : ProcessorComponent {
    override val classLoader: ClassLoader =
        instances.firstOrNull()?.javaClass?.classLoader ?: InstanceProcessorComponent::class.java.classLoader

    override fun hasService(serviceName: String): Boolean = instances.any { serviceName in supertypeNames(it.javaClass) }

    override fun <T : Any> loadServices(service: Class<T>): List<T> = instances.filter { service.isInstance(it) }.map { service.cast(it) }

    override fun toString(): String = "InstanceProcessorComponent($coordinates)"

    private fun supertypeNames(type: Class<*>): Set<String> {
        val names = mutableSetOf<String>()
        val queue = ArrayDeque<Class<*>>().apply { add(type) }
        while (queue.isNotEmpty()) {
            val c = queue.removeFirst()
            if (!names.add(c.name)) continue
            c.superclass?.let(queue::add)
            queue.addAll(c.interfaces)
        }
        return names
    }
}
