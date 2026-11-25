package fixture.java

import kotlin.test.Test
import kotlin.test.assertEquals

class JavaTest {
    @Test
    fun Javaの型を解決できる() {
        assertEquals("fixture.java.JavaPerson", HolderMapper.propertyTypes["person"])
        assertEquals("kotlin.collections.List<fixture.java.JavaPerson>", HolderMapper.propertyTypes["people"])
        assertEquals("java.lang.StringBuilder?", HolderMapper.propertyTypes["builder"])
        assertEquals("fixture.java.JavaPerson", NeedsJavaResolved.targetName)
    }

    @Test
    fun 生成したJavaをKotlinとJavaから使える() {
        assertEquals("fixture.java.Exported", ExportedJavaRef.name)
        assertEquals("fixture.java.Exported", ExportedJava.NAME)
        val holder = Holder(JavaPerson("a"), emptyList(), null)
        assertEquals(HolderMapper.toMap(holder).toString(), JavaPerson.describe(holder))
    }
}
