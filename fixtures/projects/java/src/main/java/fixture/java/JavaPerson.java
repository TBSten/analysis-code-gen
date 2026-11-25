package fixture.java;

/** Kotlin の @Mapped クラスが型として参照する Java クラス（GRD-08・ANA-03）。 */
public class JavaPerson {
    private final String name;

    public JavaPerson(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** Java からも Kotlin の生成物（Holder の Mapper）を使う。 */
    public static String describe(Holder holder) {
        return HolderMapper.INSTANCE.toMap(holder).toString();
    }
}
