/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.utils.SmartList
import kotlin.math.min

inline fun <T, R> Collection<T>.compactMap(transform: (T) -> R): List<R> {
    if (isEmpty()) return emptyList()
    if (size == 1) return SmartList(transform(first()))
    return mapTo(ArrayList(size), transform)
}

inline fun <T, R : Any> Collection<T>.compactMapNotNull(transform: (T) -> R?): List<R> {
    return mapNotNullTo(ArrayList(), transform).smartCompact()
}

inline fun <T, R> Collection<T>.compactMapIndexed(transform: (index: Int, T) -> R): List<R> {
    if (isEmpty()) return emptyList()
    if (size == 1) return SmartList(transform(0, first()))
    return mapIndexedTo(ArrayList<R>(size), transform)
}

inline fun <T, R> Collection<T>.compactFlatMap(transform: (T) -> Iterable<R>): List<R> {
    return flatMapTo(ArrayList<R>(), transform).smartCompact()
}

inline fun <T> Collection<T>.compactFilter(predicate: (T) -> Boolean): List<T> {
    return filterTo(ArrayList(), predicate).smartCompact()
}

inline fun <T> Collection<T>.compactFilterNot(predicate: (T) -> Boolean): List<T> {
    return filterNotTo(ArrayList(), predicate).smartCompact()
}

inline fun <reified T> Collection<*>.compactFilterIsInstance(): List<T> {
    return filterIsInstanceTo(ArrayList<T>()).smartCompact()
}

infix fun <T> List<T>.compactPlus(elements: List<T>): List<T> =
    when (val resultSize = size + elements.size) {
        0 -> emptyList()
        1 -> ifEmpty { elements }
        else -> ArrayList<T>(resultSize).also {
            it.addAll(this)
            it.addAll(elements)
        }
    }

infix fun <T> List<T>.compactPlus(element: T): List<T> =
    when (size) {
        0 -> SmartList(element)
        else -> ArrayList<T>(size + 1).also {
            it.addAll(this)
            it.add(element)
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
        throw IllegalArgumentException("Collection has more than one element.")
    return single
}

inline fun <reified T> Iterable<*>.findIsInstanceAnd(predicate: (T) -> Boolean): T? {
    for (element in this) if (element is T && predicate(element)) return element
    return null
}

/**
 * The same as `ArrayList::compact` extension function, but it could be used with the
 * immutable list type (without `trimToSize` for the collections with more than 1 element)
 * and return mutable `SmartList` for single element instead of `java.collections.SingletonList` for single element container
 */
fun <T> List<T>.smartCompact(): List<T> =
    when (size) {
        0 -> emptyList()
        1 -> SmartList(first())
        else -> apply {
            if (this is ArrayList<*>) trimToSize()
        }
    }

