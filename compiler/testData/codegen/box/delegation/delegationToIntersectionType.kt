fun <T> select(a: T, b: T) : T = a

interface A
interface B {
    fun foo(): String
}
// the to-be-called method is in the second supertype to assert that we don't blindly pick the first type in an intersection type
class C : A, B {
    override fun foo() = "OK"
}
class D : A, B {
    override fun foo() = "FAIL"
}

fun test(c: C, d: D): String {
    val intersection = select(c, d)
    return object: B by intersection {}.foo()
}

fun box() = test(C(), D())