/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

inline fun <reified R, C : MutableCollection<in R>> Iterable<*>.filterIsInstanceAndTo(destination: C, predicate: (R) -> Boolean): C {
    for (element in this) if (element is R && predicate(element)) destination.add(element)
    return destination
}

inline fun <reified T, reified R, C : MutableCollection<in R>> Iterable<*>.filterIsInstanceMapTo(destination: C, transform: (T) -> R): C {
    for (element in this) if (element is T) {
        destination.add(transform(element))
    }
    return destination
}

inline fun <reified T, reified R> Collection<*>.filterIsInstanceMapNotNull(transform: (T) -> R?): Collection<R> {
    if (isEmpty()) return emptyList()
    return filterIsInstanceMapNotNullTo(SmartList(), transform)
}

inline fun <reified R> Collection<*>.filterIsInstanceAnd(predicate: (R) -> Boolean): List<R> {
    if (isEmpty()) return emptyList()
    return filterIsInstanceAndTo(SmartList(), predicate)
}

inline fun <reified T, reified R, C : MutableCollection<in R>> Iterable<*>.filterIsInstanceMapNotNullTo(destination: C, transform: (T) -> R?): C {
    for (element in this) if (element is T) {
        val result = transform(element)
        if (result != null) {
            destination.add(result)
        }
    }
    return destination
}

/**
 * Concatenates the contents of this collection with the given collection, avoiding allocations if possible.
 * Can modify `this` if it is a mutable collection.
 */
fun <T> Collection<T>?.concat(collection: Collection<T>): Collection<T>? {
    if (collection.isEmpty()) {
        return this
    }
    if (this == null) {
        return collection
    }
    if (this is LinkedHashSet) {
        addAll(collection)
        return this
    }

    val result = LinkedHashSet(this)
    result.addAll(collection)
    return result
}

