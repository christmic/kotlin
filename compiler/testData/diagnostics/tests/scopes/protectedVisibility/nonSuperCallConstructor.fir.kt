// IGNORE_REVERSED_RESOLVE
// !DIAGNOSTICS: -UNUSED_PARAMETER

open class A protected constructor(x: Int) {
    protected constructor() : this(1)
    public constructor(x: Double) : this(3)
}

class B4 : A(1) {
    init {
        A()
        A(1)
        A(5.0)
    }

    fun foo() {
        A()
        A(1)
        A(5.0)

        object : A() {}
        object : A(1) {}
        object : A(5.0) {}

        class Local : A()
    }
}
