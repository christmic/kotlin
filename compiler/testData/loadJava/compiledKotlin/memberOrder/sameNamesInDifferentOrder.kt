package test

class A {
    fun b() {}
    fun a() {}
    val b: Int = { 1 }()
    val a: Int = { 2 }()
    val Int.b: String get() = "b"
    fun String.b() {}
    val Int.a: String get() = "a"
    fun String.a() {}
}
