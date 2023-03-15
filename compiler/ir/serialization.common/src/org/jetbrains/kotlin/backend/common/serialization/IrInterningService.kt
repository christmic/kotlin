/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.name.Name
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet

/**
 * The interface provide an API for interning [String] and [Name] values
 * to save memory by eliminating duplicates of instances of those classes
 */
interface IrInterningService {
    fun string(string: String): String {
        return string
    }

    fun name(name: Name): Name {
        return name
    }

    fun clear() {}
}

/**
 * The default implementation of [IrInterningService] which used [ObjectOpenHashSet]
 * to cache [String] and [Name] values. It helps to eliminate saving a lot of the same strings (mostly [org.jetbrains.kotlin.ir.util.IdSignature.CommonSignature.packageFqName] and [org.jetbrains.kotlin.ir.util.IdSignature.CommonSignature.declarationFqName])
 * and names in IR nodes
 */
class DefaultIrInterningService : IrInterningService {
    private val strings by lazy { ObjectOpenHashSet<String>() }
    private val names by lazy { ObjectOpenHashSet<Name>() }

    override fun string(string: String): String {
        return strings.addOrGet(string)
    }

    override fun name(name: Name): Name {
        return names.addOrGet(name)
    }

    override fun clear() {
        strings.clear()
        names.clear()
    }
}