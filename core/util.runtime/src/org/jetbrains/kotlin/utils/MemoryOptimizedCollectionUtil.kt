/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import java.util.*
import kotlin.math.min

/**
 * A memory-optimized version of [Iterable.map].
 * @see Iterable.map
 */
inline fun <T, R> Collection<T>.memoryOptimizedMap(transform: (T) -> R): List<R> {
    if (isEmpty()) return emptyList()
    if (size == 1) return Collections.singletonList(transform(first()))
    return mapTo(ArrayList(size), transform)
}

/**
 * A memory-optimized version of [Iterable.mapNotNull].
 * @see Iterable.mapNotNull
 */
inline fun <T, R : Any> Collection<T>.memoryOptimizedMapNotNull(transform: (T) -> R?): List<R> {
    return mapNotNullTo(ArrayList(), transform).smartCompact()
}

/**
 * A memory-optimized version of [Iterable.mapIndexed].
 * @see Iterable.mapIndexed
 */
inline fun <T, R> Collection<T>.memoryOptimizedMapIndexed(transform: (index: Int, T) -> R): List<R> {
    if (isEmpty()) return emptyList()
    if (size == 1) return Collections.singletonList(transform(0, first()))
    return mapIndexedTo(ArrayList<R>(size), transform)
}

/**
 * A memory-optimized version of [Iterable.flatMap].
 * @see Iterable.flatMap
 */
inline fun <T, R> Collection<T>.memoryOptimizedFlatMap(transform: (T) -> Iterable<R>): List<R> {
    return flatMapTo(ArrayList<R>(), transform).smartCompact()
}

/**
 * A memory-optimized version of [Iterable.filter].
 * @see Iterable.filter
 */
inline fun <T> Collection<T>.memoryOptimizedFilter(predicate: (T) -> Boolean): List<T> {
    return filterTo(ArrayList(), predicate).smartCompact()
}

/**
 * A memory-optimized version of [Iterable.filterNot].
 * @see Iterable.filterNot
 */
inline fun <T> Collection<T>.memoryOptimizedFilterNot(predicate: (T) -> Boolean): List<T> {
    return filterNotTo(ArrayList(), predicate).smartCompact()
}

/**
 * A memory-optimized version of [Iterable.filterIsInstance].
 * @see Iterable.filterIsInstance
 */
inline fun <reified T> Collection<*>.memoryOptimizedFilterIsInstance(): List<T> {
    return filterIsInstanceTo(ArrayList<T>()).smartCompact()
}

/**
 * A memory-optimized version of [Iterable.plus].
 * @see Iterable.plus
 */
infix fun <T> List<T>.memoryOptimizedPlus(elements: List<T>): List<T> =
    when (val resultSize = size + elements.size) {
        0 -> emptyList()
        1 -> Collections.singletonList(if (isEmpty()) elements.first() else first())
        else -> ArrayList<T>(resultSize).also {
            it.addAll(this)
            it.addAll(elements)
        }
    }

/**
 * A memory-optimized version of [Iterable.plus].
 * @see Iterable.plus
 */
infix fun <T> List<T>.memoryOptimizedPlus(element: T): List<T> =
    when (size) {
        0 -> Collections.singletonList(element)
        else -> ArrayList<T>(size + 1).also {
            it.addAll(this)
            it.add(element)
        }
    }

/**
 * A memory-optimized version of [Iterable.zip].
 * @see Iterable.zip
 */
infix fun <T, R> Collection<T>.memoryOptimizedZip(other: Collection<R>): List<Pair<T, R>> {
    if (isEmpty() || other.isEmpty()) return emptyList()
    if (min(size, other.size) == 1) return Collections.singletonList(first() to other.first())
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * [Sequence] variant of [org.jetbrains.kotlin.backend.common.atMostOne]
 * So, when:
 * - there is no element then `null` will be returned
 * - there is a single element then the element will be returned
 * - there is more than one element then the error will be thrown
 * @see org.jetbrains.kotlin.backend.common.atMostOne
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

/**
 * The variant of [org.jetbrains.kotlin.util.collectionUtils.filterIsInstanceAnd] extension function but to find the first element
 * which is an instance of type [T] and satisfies [predicate] condition
 * @see org.jetbrains.kotlin.util.collectionUtils.filterIsInstanceAnd
 */
inline fun <reified T> Iterable<*>.findIsInstanceAnd(predicate: (T) -> Boolean): T? {
    for (element in this) if (element is T && predicate(element)) return element
    return null
}

/**
 * The same as [org.jetbrains.kotlin.utils.compact] extension function, but it could be used with the
 * immutable list type (without [java.util.ArrayList.trimToSize] for the collections with more than 1 element)
 * @see org.jetbrains.kotlin.utils.compact
 */
fun <T> List<T>.smartCompact(): List<T> =
    when (size) {
        0 -> emptyList()
        1 -> Collections.singletonList(first())
        else -> apply {
            if (this is ArrayList<*>) trimToSize()
        }
    }

