// IGNORE_ERRORS
// ERROR_POLICY: SEMANTIC
// IGNORE_BACKEND_K2: JS_IR

// MODULE: lib
// FILE: t.kt

fun bar<T>(a: T): T = a

var storage = ""

fun foo() {
    storage += bar("O")
    storage += bar<Any, String, Number>("K")
}

// MODULE: main(lib)
// FILE: b.kt

fun box(): String {
    foo()
    return storage
}