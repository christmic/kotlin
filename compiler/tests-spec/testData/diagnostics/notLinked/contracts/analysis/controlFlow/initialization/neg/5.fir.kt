// !OPT_IN: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1() {
    val value_1: Int
    funWithExactlyOnceCallsInPlace({ value_1 = 10 })
    value_1.inc()
}

// TESTCASE NUMBER: 2
fun case_2() {
    var value_1: Int
    val l = { value_1 = 10 }
    funWithAtLeastOnceCallsInPlace(l)
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

// TESTCASE NUMBER: 3
fun case_3() {
    var value_1: Int
    val l = fun () { value_1 = 10 }
    funWithAtLeastOnceCallsInPlace(l)
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

// TESTCASE NUMBER: 4
fun case_4() {
    var value_1: Int
    funWithAtLeastOnceCallsInPlace(fun () { value_1 = 10 })
    value_1.inc()
}

// TESTCASE NUMBER: 5
fun case_5() {
    val value_1: Int
    val o = object {
        fun l() { <!CAPTURED_VAL_INITIALIZATION!>value_1<!> = 10 }
    }
    funWithExactlyOnceCallsInPlace(o::l)
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}
