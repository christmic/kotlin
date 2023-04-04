/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.compilation.CompilationOptions
import org.jetbrains.kotlin.buildtools.api.compilation.CompilationResult
import org.jetbrains.kotlin.buildtools.api.compilation.CompilerOptions

/**
 * A facade for invoking compilation in Kotlin compiler. It allows to use compiler in different modes.
 * TODO: add a mention where to see the available modes after implementing them
 */
interface CompilationService {
    fun compile(
        compilerOptions: CompilerOptions,
        arguments: List<String>,
        compilationOptions: CompilationOptions,
    ): CompilationResult

    companion object {
        @JvmStatic
        fun loadImplementation(classLoader: ClassLoader) = loadImplementation(CompilationService::class, classLoader)
    }
}