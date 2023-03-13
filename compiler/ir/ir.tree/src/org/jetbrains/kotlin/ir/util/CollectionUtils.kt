/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.compact
import kotlin.math.min

fun <T> listWithStrictCapacity(size: Int): MutableList<T> {
    return when (size) {
        0, 1 -> SmartList()
        else -> ArrayList(size)
    }
}

inline fun <T, R> Collection<T>.map(transform: (T) -> R): List<R> {
    if (isEmpty()) return emptyList()
    if (size == 1) return SmartList(transform(first()))
    return mapTo(ArrayList(size), transform)
}

inline fun <T, R : Any> Collection<T>.mapNotNull(transform: (T) -> R?): List<R> {
    if (isEmpty()) return emptyList()
    if (size == 1) return transform(first())?.let { SmartList(it) } ?: emptyList()
    return mapNotNullTo(ArrayList(size), transform)
}

inline fun <T, R> Collection<T>.mapIndexed(transform: (index: Int, T) -> R): List<R> {
    if (isEmpty()) return emptyList()
    if (size == 1) return SmartList(transform(0, first()))
    return mapIndexedTo(ArrayList<R>(size), transform)
}

inline fun <T, R> Collection<T>.compactFlatMap(transform: (T) -> Iterable<R>): List<R> {
    return flatMapTo(ArrayList<R>(), transform).compact()
}

inline fun <T> Collection<T>.compactFilter(predicate: (T) -> Boolean): List<T> {
    return filterTo(ArrayList(), predicate).compact()
}

inline fun <T> Collection<T>.compactFilterNot(predicate: (T) -> Boolean): List<T> {
    return filterNotTo(ArrayList(), predicate).compact()
}

inline fun <reified T> Collection<*>.compactFilterIsInstance(): List<T> {
    return filterIsInstanceTo(ArrayList<T>()).compact()
}

operator fun <T> List<T>.plus(elements: List<T>): List<T> {
    if (isEmpty() && elements.isEmpty()) return emptyList()
    val result = ArrayList<T>(this.size + elements.size)
    result.addAll(this)
    result.addAll(elements)
    return when (result.size) {
        0 -> emptyList()
        1 -> SmartList(result.first())
        else -> result
    }
}

infix fun <T, R> Collection<T>.compactZip(other: Collection<R>): List<Pair<T, R>> {
    if (isEmpty() || other.isEmpty()) return emptyList()
    if (min(size, other.size) == 1) return SmartList(first() to other.first())
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * It's a stricter variant of the `singleOrNull` method.
 * The only difference is when there is more than 1 element in the `Sequence`, then it will throw an error
 * So, when:
 * - there is no element then `null` will be returned
 * - there is a single element then the element will be returned
 * - there is more than one element then the error will be thrown
 */
fun <T> Sequence<T>.atMostOne(): T? {
    val iterator = iterator()
    if (!iterator.hasNext())
        return null
    val single = iterator.next()
    if (iterator.hasNext())
        throw AssertionError("Assertion failed")
    return single
}

inline fun <reified T> Iterable<*>.findIsInstanceAnd(predicate: (T) -> Boolean): T? {
    for (element in this) if (element is T && predicate(element)) return element
    return null
}
