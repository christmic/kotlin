/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers

import org.jetbrains.kotlin.fir.analysis.FirDeclarationWithParents
import org.jetbrains.kotlin.fir.analysis.checkers.FirPlatformDiagnosticSuppressor
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.hasAnnotationOrInsideAnnotatedClass
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.name.StandardClassIds

private val NATIVE_ANNOTATIONS = arrayOf(
    StandardClassIds.Annotations.JsNative,
    StandardClassIds.Annotations.JsNativeInvoke,
    StandardClassIds.Annotations.JsNativeGetter,
    StandardClassIds.Annotations.JsNativeSetter,
)

private fun FirDeclaration.isLexicallyInsideJsNative(context: CheckerContext): Boolean {
    val data = FirDeclarationWithParents(this, context.containingDeclarations, context)
    return NATIVE_ANNOTATIONS.any { data.hasAnnotationOrInsideAnnotatedClass(it) }
}

object FirJsPlatformDiagnosticSuppressor : FirPlatformDiagnosticSuppressor {
    override fun shouldReportUnusedParameter(parameter: FirVariable, context: CheckerContext) =
        !parameter.isLexicallyInsideJsNative(context)

    override fun shouldReportNoBody(declaration: FirCallableDeclaration, context: CheckerContext) =
        !declaration.isLexicallyInsideJsNative(context)
}
