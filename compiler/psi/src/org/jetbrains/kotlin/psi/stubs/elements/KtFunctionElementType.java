/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionStub;
import org.jetbrains.kotlin.psi.stubs.impl.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KtFunctionElementType extends KtStubElementType<KotlinFunctionStub, KtNamedFunction> {

    private static final String NAME = "kotlin.FUNCTION";

    public KtFunctionElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtNamedFunction.class, KotlinFunctionStub.class);
    }

    @NotNull
    @Override
    public KotlinFunctionStub createStub(@NotNull KtNamedFunction psi, @NotNull StubElement parentStub) {
        boolean isTopLevel = psi.getParent() instanceof KtFile;
        boolean isExtension = psi.getReceiverTypeReference() != null;
        FqName fqName = KtPsiUtilKt.safeFqNameForLazyResolve(psi);
        boolean hasBlockBody = psi.hasBlockBody();
        boolean hasBody = psi.hasBody();
        return new KotlinFunctionStubImpl(
                (StubElement<?>) parentStub, StringRef.fromString(psi.getName()), isTopLevel, fqName,
                isExtension, hasBlockBody, hasBody, psi.hasTypeParameterListBeforeFunctionName(),
                psi.mayHaveContract(),
                null
        );
    }

    @Override
    public void serialize(@NotNull KotlinFunctionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isTopLevel());

        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName != null ? fqName.asString() : null);

        dataStream.writeBoolean(stub.isExtension());
        dataStream.writeBoolean(stub.hasBlockBody());
        dataStream.writeBoolean(stub.hasBody());
        dataStream.writeBoolean(stub.hasTypeParameterListBeforeFunctionName());
        boolean haveContract = stub.mayHaveContract();
        dataStream.writeBoolean(haveContract);
        if (haveContract && stub instanceof KotlinFunctionStubImpl) {
            List<Effect> effects = ((KotlinFunctionStubImpl) stub).getContract();
            dataStream.writeInt(effects == null ? 0 : effects.size());
            if (effects != null) {
                for (Effect effect : effects) {
                    dataStream.writeInt(effect.getEffectType().ordinal());
                    writeExpressions(dataStream, effect.getArguments());
                    Expression conclusion = effect.getConclusion();
                    dataStream.writeBoolean(conclusion != null);
                    if (conclusion != null) {
                        writeExpression(dataStream, conclusion);
                    }
                    InvocationKind invocationKind = effect.getInvocationKind();
                    dataStream.writeInt(invocationKind != null ? invocationKind.ordinal() : -1);
                }
            }
        }
    }

    private static void writeExpressions(@NotNull StubOutputStream dataStream, List<Expression> arguments) throws IOException {
        dataStream.writeInt(arguments != null ? arguments.size() : -1);
        if (arguments != null) {
            for (Expression argument : arguments) {
                writeExpression(dataStream, argument);
            }
        }
    }

    private static List<Expression> readExpressions(@NotNull StubInputStream dataStream) throws IOException {
        int count = dataStream.readInt();
        if (count == -1) return null;
        List<Expression> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(readExpression(dataStream));
        }
        return result;
    }

    private static void writeExpression(@NotNull StubOutputStream dataStream, Expression argument) throws IOException {
        dataStream.writeBoolean(argument.isNegated());
        dataStream.writeBoolean(argument.isInNullPredicate());
        Integer valueParameter = argument.getValueParameter();
        dataStream.writeInt(valueParameter != null ? valueParameter : NO_VALUE_PARAMETER);
        KotlinFlexibleAwareTypeBean type = argument.getType();
        KtUserTypeElementType.serializeType(dataStream, type);
        ContractConstantValue value = argument.getConstantValue();
        dataStream.writeInt(value != null ? value.ordinal() : -1);
        writeExpressions(dataStream, argument.getAndArgs());
        writeExpressions(dataStream, argument.getOrArgs());
    }

    private static Expression readExpression(@NotNull StubInputStream dataStream) throws IOException {
        boolean isNegated = dataStream.readBoolean();
        boolean isInNullPredicate = dataStream.readBoolean();
        Integer valueParameter = dataStream.readInt();
        if (valueParameter == NO_VALUE_PARAMETER) {
            valueParameter = null;
        }
        KotlinFlexibleAwareTypeBean type = KtUserTypeElementType.deserializeType(dataStream);
        int constantValueOrdinal = dataStream.readInt();
        ContractConstantValue value = constantValueOrdinal < 0 ? null : ContractConstantValue.getEntries().get(constantValueOrdinal);

        List<Expression> andArgs = readExpressions(dataStream);
        List<Expression> orArgs = readExpressions(dataStream);

        return new Expression(isNegated, isInNullPredicate, valueParameter, type, value, andArgs, orArgs);
    }

    @NotNull
    @Override
    public KotlinFunctionStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        boolean isTopLevel = dataStream.readBoolean();

        StringRef fqNameAsString = dataStream.readName();
        FqName fqName = fqNameAsString != null ? new FqName(fqNameAsString.toString()) : null;

        boolean isExtension = dataStream.readBoolean();
        boolean hasBlockBody = dataStream.readBoolean();
        boolean hasBody = dataStream.readBoolean();
        boolean hasTypeParameterListBeforeFunctionName = dataStream.readBoolean();
        boolean mayHaveContract = dataStream.readBoolean();
        List<Effect> effects = null;
        if (mayHaveContract) {
            effects = new ArrayList<>();
            int count = dataStream.readInt();
            for (int i = 0; i< count; i++) {
                EffectType effectType = EffectType.getEntries().get(dataStream.readInt());
                List<Expression> arguments = readExpressions(dataStream);
                boolean hasConclusion = dataStream.readBoolean();
                Expression conclusion = hasConclusion ? readExpression(dataStream) : null;
                int invocationKindOrdinal = dataStream.readInt();
                InvocationKind invocationKind = invocationKindOrdinal < 0 ? null : InvocationKind.getEntries().get(invocationKindOrdinal);
                effects.add(new Effect(effectType, arguments, conclusion, invocationKind));
            }
        }
        return new KotlinFunctionStubImpl(
                (StubElement<?>) parentStub, name, isTopLevel, fqName, isExtension, hasBlockBody, hasBody,
                hasTypeParameterListBeforeFunctionName, mayHaveContract, effects
        );
    }

    //-1 corresponds to receiver
    private static final int NO_VALUE_PARAMETER = -2;

    @Override
    public void indexStub(@NotNull KotlinFunctionStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexFunction(stub, sink);
    }

    @NotNull
    @Override
    public String getExternalId() {
        return NAME;
    }
}
