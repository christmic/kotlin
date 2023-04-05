/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.psi.KtContractEffect
import org.jetbrains.kotlin.psi.stubs.KotlinContractEffectStub
import org.jetbrains.kotlin.psi.stubs.elements.KtContractEffectElementType

class KotlinContractEffectStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: KtContractEffectElementType
) : KotlinPlaceHolderStubImpl<KtContractEffect>(parent, elementType), KotlinContractEffectStub


enum class KotlinContractEffectType {
    RETURNS_CONSTANT,
    RETURNS_NOT_NULL,
    CALLS,
}

enum class KotlinContractInvocationKind {
    AT_MOST_ONCE,
    EXACTLY_ONCE,
    AT_LEAST_ONCE;

    fun toEventOccurrencesRange(): EventOccurrencesRange {
        return when (this) {
            AT_MOST_ONCE -> EventOccurrencesRange.AT_MOST_ONCE
            EXACTLY_ONCE -> EventOccurrencesRange.EXACTLY_ONCE
            AT_LEAST_ONCE -> EventOccurrencesRange.AT_LEAST_ONCE
        }
    }
}

enum class KotlinContractConstantValue {
    TRUE, FALSE, NULL;
}

data class KotlinContractEffect(
    val effectType: KotlinContractEffectType,
    val arguments: List<KotlinContractExpression>?,
    val conclusion: KotlinContractExpression?,
    val invocationKind: KotlinContractInvocationKind?
)

data class KotlinContractExpression(
    val isNegated: Boolean,
    val isInNullPredicate: Boolean,
    val valueParameter: Int?,
    val type: KotlinTypeBean?,
    val constantValue: KotlinContractConstantValue?,
    val andArgs: List<KotlinContractExpression>?,
    val orArgs: List<KotlinContractExpression>?
)