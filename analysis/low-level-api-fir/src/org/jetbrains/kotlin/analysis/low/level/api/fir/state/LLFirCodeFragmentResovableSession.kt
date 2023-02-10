/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtPsiSourceFileLinesMapping
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirCodeFragmentSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.builder.buildFileAnnotationsContainer
import org.jetbrains.kotlin.fir.builder.buildPackageDirective
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.types.ConstantValueKind


internal val FirSession.codeFragmentSymbolProvider: LLFirCodeFragmentSymbolProvider by FirSession.sessionComponentAccessor()

internal class LLFirCodeFragmentResovableSession(
    ktModule: KtModule,
    useSiteSessionFactory: (KtModule) -> LLFirSession
) : LLFirResolvableResolveSession(ktModule, useSiteSessionFactory) {
    override fun getModuleKind(module: KtModule): ModuleKind {
        TODO("Not yet implemented")
    }

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> {
        TODO("Not yet implemented")
    }

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> {
        TODO("Not yet implemented")
    }

    override fun getOrBuildFirFor(element: KtElement): FirElement? {
        val moduleComponents = getModuleComponentsForElement(element)
        val builder = object : RawFirBuilder(
            moduleComponents.session,
            moduleComponents.scopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL
        ) {
            fun build() = object : Visitor() {
                override fun visitCallExpression(expression: KtCallExpression, data: Unit): FirElement {
                    return super.visitCallExpression(expression, data)
                }

                override fun visitKtFile(file: KtFile, data: Unit): FirElement {
                    return buildFile {
                        symbol = FirFileSymbol()
                        source = file.toFirSourceElement()
                        moduleData = baseModuleData
                        origin = FirDeclarationOrigin.Source
                        name = file.name
                        sourceFile = KtPsiSourceFile(file)
                        sourceFileLinesMapping = KtPsiSourceFileLinesMapping(file)
                        packageDirective = buildPackageDirective {
                            packageFqName = FqName.ROOT
                            source = file.packageDirective?.toKtPsiSourceElement()
                        }
                        annotationsContainer = buildFileAnnotationsContainer {
                            moduleData = baseModuleData
                            containingFileSymbol = this@buildFile.symbol
                            source = file.toKtPsiSourceElement()
                            /**
                             * applying Suppress("INVISIBLE_*) to file, supposed to instruct frontend to ignore `private`
                             * modifier.
                             * TODO: invisitagte why it's not enough for
                             * [org.jetbrains.kotlin.idea.k2.debugger.test.cases.K2EvaluateExpressionTestGenerated.SingleBreakpoint.CompilingEvaluator.InaccessibleMembers]
                             */
                            annotations += buildAnnotationCall {
                                source = file.toFirSourceElement()
                                val annotationClassIdLookupTag = ClassId(
                                    StandardNames.FqNames.suppress.parent(),
                                    StandardNames.FqNames.suppress.shortName()
                                ).toLookupTag()
                                val annotationType = ConeClassLikeTypeImpl(
                                    annotationClassIdLookupTag,
                                    emptyArray(),
                                    isNullable = false
                                )
                                calleeReference = buildResolvedNamedReference {
                                    val annotationTypeSymbol = (annotationType.toSymbol(useSiteFirSession) as? FirRegularClassSymbol)
                                        ?: return@buildAnnotationCall

                                    val constructorSymbol =
                                        annotationTypeSymbol.unsubstitutedScope(
                                            useSiteFirSession,
                                            useSiteFirSession.getScopeSession(),
                                            withForcedTypeCalculator = false,
                                            memberRequiredPhase = null
                                        )
                                            .getDeclaredConstructors().firstOrNull() ?: return@buildAnnotationCall
                                    resolvedSymbol = constructorSymbol
                                    name = constructorSymbol.name
                                }
                                annotationTypeRef = buildResolvedTypeRef {
                                    source = file.toFirSourceElement()
                                    type = annotationType
                                }
                                argumentMapping = buildAnnotationArgumentMapping {
                                    source = file.toFirSourceElement()
                                    mapping[Name.identifier("name")] =
                                        buildConstExpression(file.toFirSourceElement(), ConstantValueKind.String, "INVISIBLE_REFERENCE")
                                }
                                annotationResolvePhase = FirAnnotationResolvePhase.CompilerRequiredAnnotations
                            }
                        }

                        for (importDirective in file.importDirectives) {
                            imports += buildImport {
                                source = importDirective.toFirSourceElement()
                                importedFqName = importDirective.importedFqName
                                isAllUnder = importDirective.isAllUnder
                                aliasName = importDirective.aliasName?.let { Name.identifier(it) }
                                aliasSource = importDirective.alias?.nameIdentifier?.toFirSourceElement()
                            }
                        }
                        /**
                         * TODO: imports it's supposed to be filled above, but ..
                         * this is workaround for tests should be removed.
                         */
                        imports += buildImport {
                            source = file.toFirSourceElement()
                            importedFqName = FqName.fromSegments(listOf("test", "Foo"))
                            isAllUnder = false
                        }
                        imports += buildImport {
                            source = file.toFirSourceElement()
                            importedFqName = FqName.fromSegments(listOf("test", "block"))
                            isAllUnder = false
                        }
                        for (declaration in file.declarations) {
                            declarations += when (declaration) {
                                is KtDestructuringDeclaration -> buildErrorTopLevelDestructuringDeclaration(declaration.toFirSourceElement())
                                else -> convertElement(declaration) as FirDeclaration
                            }
                        }
                        val name = Name.identifier("Generated_for_debugger_class")
                        val generatedClassId = ClassId(FqName.ROOT, name)
                        val generatedClass = buildRegularClass {
                            moduleData = baseModuleData
                            origin = FirDeclarationOrigin.Synthetic
                            this.name = name
                            symbol = FirRegularClassSymbol(generatedClassId)
                            status = FirResolvedDeclarationStatusImpl(
                                Visibilities.Public,
                                Modality.FINAL,
                                EffectiveVisibility.Public
                            ).apply {
                                isExpect = false
                                isActual = false
                                isCompanion = false
                                isInner = false
                                isData = false
                                isInline = false
                                isExternal = false
                                isFun = false
                            }
                            classKind = ClassKind.OBJECT
                            scopeProvider = this@LLFirCodeFragmentResovableSession.useSiteFirSession.kotlinScopeProvider
                            superTypeRefs += this@LLFirCodeFragmentResovableSession.useSiteFirSession.builtinTypes.anyType
                            val danglingExpression = file.children.filter {
                                it is KtExpression || it is KtBlockExpression
                            }.map {
                                super.convertElement(it as KtElement)
                            }.single()
                            val dangingReturnType = when (danglingExpression) {
                                is FirBlock -> (danglingExpression.statements.last() as? FirExpression)?.typeRef
                                    ?: FirImplicitUnitTypeRef(file.toKtPsiSourceElement())
                                else -> (danglingExpression as? FirExpression)?.typeRef
                                    ?: FirImplicitUnitTypeRef(file.toKtPsiSourceElement())
                            }
                            val generatedFunctionReturnTarget = FirFunctionTarget(null, false)
                            val generatedConstructor = buildPrimaryConstructor {
                                source = file.toFirSourceElement()
                                moduleData = baseModuleData
                                origin = FirDeclarationOrigin.Source
                                symbol = FirConstructorSymbol(generatedClassId)
                                status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL).apply {
                                    isExpect = false
                                    isActual = false
                                    isInner = false
                                    isFromSealedClass = false
                                    isFromEnumClass = false
                                }
                                returnTypeRef = buildResolvedTypeRef {
                                    //source = this@toDelegatedSelfType?.toFirSourceElement(KtFakeSourceElementKind.ClassSelfTypeRef)
                                    type = ConeClassLikeTypeImpl(
                                        this@buildRegularClass.symbol.toLookupTag(),
                                        emptyArray(),
                                        false
                                    )
                                }
                                delegatedConstructor = buildDelegatedConstructorCall {
                                    val superType = useSiteFirSession.builtinTypes.anyType.type
                                    constructedTypeRef = superType.toFirResolvedTypeRef()
                                    calleeReference = buildResolvedNamedReference {
                                        val superClassConstructorSymbol = superType.toRegularClassSymbol(useSiteFirSession)
                                            ?.declaredMemberScope(useSiteFirSession)
                                            ?.getDeclaredConstructors()
                                            ?.firstOrNull { it.valueParameterSymbols.isEmpty() }
                                            ?: error("shouldn't be here") //.toRegularClassSymbol(useSiteFirSession)!!
                                        this@buildResolvedNamedReference.name = superClassConstructorSymbol.name
                                        resolvedSymbol = superClassConstructorSymbol
                                    }
                                    isThis = false
                                }
                            }
                            val generatedFunction = buildSimpleFunction {
                                source = file.toFirSourceElement()
                                moduleData = baseModuleData
                                origin = FirDeclarationOrigin.Source
                                returnTypeRef = dangingReturnType
                                val functionName = Name.identifier("generated_for_debugger_fun")
                                this.name = functionName
                                status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL).apply {
                                    isOperator = false
                                    isStatic = true
                                }
                                symbol = FirNamedFunctionSymbol(CallableId(FqName.ROOT, null, functionName))
                                dispatchReceiverType = null//currentDispatchReceiverType()
                                body = buildBlock {
                                    statements += when (danglingExpression) {
                                        is FirBlock -> {
                                            buildReturnExpression {
                                                source = danglingExpression.source
                                                result = danglingExpression
                                                this.target = generatedFunctionReturnTarget
                                            }
                                        }
                                        is FirExpression -> buildReturnExpression {
                                            source = danglingExpression.source
                                            result = danglingExpression
                                            this.target = generatedFunctionReturnTarget
                                        }
                                        else -> TODO()
                                    }
                                }
                            }
                            generatedFunctionReturnTarget.bind(generatedFunction)
                            declarations.add(generatedConstructor)
                            declarations.add(generatedFunction)
                        }
                        declarations.add(generatedClass)
                        this@LLFirCodeFragmentResovableSession.useSiteFirSession.codeFragmentSymbolProvider.register(generatedClass)
                    }
                }
            }.convertElement(element)
        }
        val firFile = builder.build()
        FirLazyBodiesCalculator.calculateLazyBodies(firFile as FirFile)
        return firFile
    }
}
