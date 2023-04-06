/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase

internal class LLFirLazyResolveContractChecker {
    private val currentTransformerPhase = ThreadLocal.withInitial<FirResolvePhase?> { null }

    inline fun lazyResolveToPhaseInside(phase: FirResolvePhase, resolve: () -> Unit) {
        if (currentTransformerPhase.get() == FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS) return
        checkIfCanLazyResolveToPhase(phase)
        val previousPhase = currentTransformerPhase.get()
        currentTransformerPhase.set(phase)
        try {
            resolve()
        } finally {
            currentTransformerPhase.set(previousPhase)
        }
    }

    private fun checkIfCanLazyResolveToPhase(requestedPhase: FirResolvePhase) {
        val currentPhase = currentTransformerPhase.get() ?: return

        if (requestedPhase >= currentPhase) {
            logOrThrowException(
                """`lazyResolveToPhase($requestedPhase)` cannot be called from a transformer with a phase $currentPhase.
                    `lazyResolveToPhase` can be called only from a transformer with a phase which is strictly greater than a requested phase;
                     i.e., `lazyResolveToPhase(A)` may be only called from a lazy transformer with a phase B, where A < B. This is a contract of lazy resolve""".trimIndent()
            )
        }
    }

    private fun logOrThrowException(text: String) {
        val exception = FirLazyResolveContractViolationException(text)
        if (System.getProperty("kotlin.suppress.lazy.resolve.contract.violation") != null) {
            LoggerHolder.LOG.warn(exception)
        } else {
            throw exception
        }
    }

    private object LoggerHolder {
        val LOG = Logger.getInstance(LLFirLazyResolveContractChecker::class.java)
    }
}

private class FirLazyResolveContractViolationException(message: String) : IllegalStateException(message)