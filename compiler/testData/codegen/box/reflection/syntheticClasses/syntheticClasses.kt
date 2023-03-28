// TARGET_BACKEND: JVM
// WITH_REFLECT
// JVM_TARGET: 1.8
// FILE: syntheticClasses.kt

package test

import kotlin.reflect.*
import kotlin.test.*

fun check(x: KClass<*>) {
    assertEquals(setOf("equals", "hashCode", "toString"), x.members.mapTo(hashSetOf()) { it.name })

    assertEquals(emptyList(), x.annotations)
    assertEquals(emptyList(), x.constructors)
    assertEquals(emptyList(), x.nestedClasses)
    assertEquals(null, x.objectInstance)
    assertEquals(listOf(typeOf<Any>()), x.supertypes)
    assertEquals(emptyList(), x.sealedSubclasses)

    assertEquals(KVisibility.PUBLIC, x.visibility)
    assertTrue(x.isFinal)
    assertFalse(x.isOpen)
    assertFalse(x.isAbstract)
    assertFalse(x.isSealed)
    assertFalse(x.isData)
    assertFalse(x.isInner)
    assertFalse(x.isCompanion)
    assertFalse(x.isFun)
    assertFalse(x.isValue)

    assertFalse(x.isInstance(42))
}

fun checkFileClass() {
    val fileClass = Class.forName("test.SyntheticClassesKt").kotlin
    assertEquals("SyntheticClassesKt", fileClass.simpleName)
    assertEquals("test.SyntheticClassesKt", fileClass.qualifiedName)
    check(fileClass)
}

fun checkKotlinLambda() {
    // Annotate with @JvmSerializableLambda to prevent the lambda from being generated via invokedynamic.
    val lambda = @JvmSerializableLambda {}
    val klass = lambda::class

    // isAnonymousClass/simpleName behavior is different for Kotlin anonymous classes in JDK 1.8 and 9+, see KT-23072.
    if (klass.java.isAnonymousClass) {
        assertEquals(null, klass.simpleName)
    } else {
        assertEquals("lambda\$1", klass.simpleName)
    }

    assertEquals(null, klass.qualifiedName)
    check(klass)

    assertTrue(klass.isInstance(lambda))
    assertNotEquals(klass, (@JvmSerializableLambda {})::class)
    val equals = klass.members.single { it.name == "equals" } as KFunction<Boolean>
    assertTrue(equals.call(lambda, lambda))
}

fun checkJavaLambda() {
    val lambda = JavaClass.lambda()
    val klass = lambda::class
    check(klass)

    assertTrue(klass.isInstance(lambda))
    assertNotEquals(klass, Runnable {}::class)
    val equals = klass.members.single { it.name == "equals" } as KFunction<Boolean>
    assertTrue(equals.call(lambda, lambda))
}

fun box(): String {
    checkFileClass()
    checkKotlinLambda()

    // TODO: fails with KotlinReflectionInternalError: Unresolved class: class JavaClass$$Lambda$1166/180251002 (kind = null)
    // checkJavaLambda()

    return "OK"
}

// FILE: JavaClass.java

public class JavaClass {
    public static Runnable lambda() {
        return () -> {};
    }
}
