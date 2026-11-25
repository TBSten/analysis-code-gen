package me.tbsten.analysiscodegen.pbt.source

/**
 * 生成する利用者プロジェクトのモデル（ADR-022 の 3 節）。
 *
 * ソース文字列ではなくモデルとして持ち、[render] でソースへ書き出す。縮小（[ProjectShrinker]）と
 * 変更の列（[me.tbsten.analysiscodegen.pbt.change.ChangeOp]）もモデルに対して行う。
 * 型の参照は宣言の [KClassDecl.id] で持ち、書き出し時に完全修飾名へ直すので、
 * 名前の変更やファイルの移動をしても参照は壊れない。
 *
 * 妥当性（コンパイルが通ること）は [normalize] の不動点として定義する: `normalize(spec) == spec` なら妥当。
 */
public data class ProjectSpec(
    val modules: List<ModuleSpec>,
) {
    /** 失敗時の表示用。kotest は反例を toString で出すので、書き出したソースを見せる。 */
    override fun toString(): String = render(this).describe()
}

/** Gradle モジュール 1 つ。[dependsOn] は自分より前のモジュール名だけ。 */
public data class ModuleSpec(
    val name: String,
    val dependsOn: List<String>,
    val files: List<SourceFileSpec>,
)

public sealed interface SourceFileSpec {
    public val pkg: String
}

public data class KotlinFileSpec(
    val name: String,
    override val pkg: String,
    val decls: List<KTopLevel>,
) : SourceFileSpec

public data class JavaFileSpec(
    override val pkg: String,
    val cls: JClassDecl,
) : SourceFileSpec

/** 可視性。順序は「狭い → 広い」。 */
public enum class Visibility(public val keyword: String) {
    PRIVATE("private"),
    INTERNAL("internal"),
    PUBLIC("public"),
    ;

    public fun min(other: Visibility): Visibility = if (ordinal <= other.ordinal) this else other
}

public enum class ClassKind {
    CLASS,
    DATA,
    INTERFACE,
    OBJECT,
    ENUM,
    SEALED,
    VALUE,
    ANNOTATION,
}

public enum class Variance(public val keyword: String) {
    INVARIANT(""),
    OUT("out "),
    IN("in "),
}

public sealed interface Bound {
    public data object None : Bound

    public data object AnyBound : Bound

    /** `T : Comparable<T>` */
    public data object ComparableSelf : Bound

    /** `T : <型引数の無い interface>` */
    public data class Interface(val id: String) : Bound
}

public data class TypeParamSpec(
    val name: String,
    val variance: Variance = Variance.INVARIANT,
    val bound: Bound = Bound.None,
)

public enum class BuiltinType(public val kotlin: String, public val arity: Int, public val java: String?) {
    INT("Int", 0, "int"),
    LONG("Long", 0, "long"),
    BOOLEAN("Boolean", 0, "boolean"),
    DOUBLE("Double", 0, "double"),
    STRING("String", 0, "String"),
    LIST("List", 1, null),
    SET("Set", 1, null),
    MAP("Map", 2, null),
}

/** 型の参照。[nullable] は各ノードに持つ。 */
public sealed interface TypeRef {
    public val nullable: Boolean

    public data class Builtin(
        val type: BuiltinType,
        val args: List<TypeRef> = emptyList(),
        override val nullable: Boolean = false,
    ) : TypeRef

    /** 生成した Kotlin 宣言（クラス・typealias）への参照。型引数は常に `*`（数は [normalize] が合わせる）。 */
    public data class Decl(
        val id: String,
        val starArgs: Int = 0,
        override val nullable: Boolean = false,
    ) : TypeRef

    /** 生成した Java クラスへの参照。プラットフォーム型を避けるため Kotlin 側では常に nullable にする。 */
    public data class Java(
        val id: String,
        override val nullable: Boolean = true,
    ) : TypeRef

    /** スコープ内の型パラメータ。 */
    public data class Param(
        val name: String,
        override val nullable: Boolean = false,
    ) : TypeRef

    public companion object {
        public val INT: TypeRef = Builtin(BuiltinType.INT)
    }
}

/** 注釈の使用位置ターゲット。 */
public enum class UseSite(public val prefix: String) {
    NONE(""),
    FIELD("field:"),
    GET("get:"),
    PARAM("param:"),
    PROPERTY("property:"),
}

/** 注釈クラスのパラメータに使える型。 */
public enum class AnnotationParamType(public val kotlin: String) {
    STRING("String"),
    INT("Int"),
    BOOLEAN("Boolean"),
    KCLASS("kotlin.reflect.KClass<*>"),
    STRING_ARRAY("Array<String>"),
}

public data class AnnotationParamSpec(
    val name: String,
    val type: AnnotationParamType,
    val hasDefault: Boolean,
)

/** 注釈の引数。KClass の値は宣言 id か組み込みの `String::class`。 */
public sealed interface AnnotationArg {
    public data class Str(val value: String) : AnnotationArg

    public data class IntLit(val value: Int) : AnnotationArg

    public data class Bool(val value: Boolean) : AnnotationArg

    public data class ClassLit(val id: String?) : AnnotationArg

    public data class StrArray(val values: List<String>) : AnnotationArg
}

public data class AnnotationUse(
    val annotationId: String,
    val args: Map<String, AnnotationArg> = emptyMap(),
    val site: UseSite = UseSite.NONE,
)

public enum class PropKind(public val keyword: String?) {
    VAL("val"),
    VAR("var"),

    /** コンストラクタの、プロパティにならない引数 */
    PLAIN(null),
}

public data class ParamSpec(
    val name: String,
    val type: TypeRef,
    val kind: PropKind = PropKind.PLAIN,
    val visibility: Visibility = Visibility.PUBLIC,
    val hasDefault: Boolean = false,
    val annotations: List<AnnotationUse> = emptyList(),
)

public sealed interface Member {
    public val name: String
    public val visibility: Visibility
    public val annotations: List<AnnotationUse>
}

public data class PropertyMember(
    override val name: String,
    val type: TypeRef,
    val mutable: Boolean = false,
    override val visibility: Visibility = Visibility.PUBLIC,
    override val annotations: List<AnnotationUse> = emptyList(),
) : Member

public data class FunctionMember(
    override val name: String,
    val typeParams: List<TypeParamSpec> = emptyList(),
    val params: List<ParamSpec> = emptyList(),
    val returnType: TypeRef = TypeRef.INT,
    override val visibility: Visibility = Visibility.PUBLIC,
    override val annotations: List<AnnotationUse> = emptyList(),
    /** 本体だけの変更（ABI は変わらない）を表す版。0 なら `TODO()`、それ以外は `TODO("b<版>")` */
    val bodyVersion: Int = 0,
) : Member

public sealed interface KTopLevel {
    public val id: String
    public val visibility: Visibility
}

public data class KClassDecl(
    override val id: String,
    val name: String = id,
    val kind: ClassKind = ClassKind.CLASS,
    override val visibility: Visibility = Visibility.PUBLIC,
    val typeParams: List<TypeParamSpec> = emptyList(),
    val ctorParams: List<ParamSpec> = emptyList(),
    val members: List<Member> = emptyList(),
    val nested: List<KClassDecl> = emptyList(),
    val inner: Boolean = false,
    val annotations: List<AnnotationUse> = emptyList(),
    /** 型引数の無い interface の id */
    val superInterfaces: List<String> = emptyList(),
    /** 直接外側にある sealed クラスを継承するか（入れ子のときだけ意味がある） */
    val extendsSealedOuter: Boolean = false,
    val enumEntries: List<String> = emptyList(),
    val companion: List<Member> = emptyList(),
    val hasCompanion: Boolean = false,
    val annotationParams: List<AnnotationParamSpec> = emptyList(),
) : KTopLevel

public data class KTypeAlias(
    override val id: String,
    val name: String = id,
    override val visibility: Visibility = Visibility.PUBLIC,
    val target: TypeRef,
) : KTopLevel

public data class KTopFunction(
    override val id: String,
    val fn: FunctionMember,
    /** 拡張関数の受け手（null なら通常の関数） */
    val receiver: TypeRef? = null,
) : KTopLevel {
    override val visibility: Visibility get() = fn.visibility
}

public data class KTopProperty(
    override val id: String,
    val prop: PropertyMember,
    val isConst: Boolean = false,
) : KTopLevel {
    override val visibility: Visibility get() = prop.visibility
}

public data class JFieldSpec(
    val name: String,
    val type: TypeRef,
)

/** Java のクラス（Java 混在用）。public で、引数なしコンストラクタを持つ。 */
public data class JClassDecl(
    val id: String,
    val name: String = id,
    val fields: List<JFieldSpec> = emptyList(),
)
