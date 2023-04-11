// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR
// WITH_STDLIB
fun <T> T.id() = this

const val code = '1'.<!EVALUATED("49")!>code<!>
const val floorDiv = 10.<!EVALUATED("5")!>floorDiv(2)<!>
const val mod = 5.<!EVALUATED("2")!>mod(3)<!>

fun box(): String {
    if (<!EVALUATED("49")!>code<!>.id() != 49) return "Fail 1"
    if (<!EVALUATED("5")!>floorDiv<!>.id() != 5) return "Fail 2"
    if (<!EVALUATED("2")!>mod<!>.id() != 2) return "Fail 3"

    return "OK"
}
