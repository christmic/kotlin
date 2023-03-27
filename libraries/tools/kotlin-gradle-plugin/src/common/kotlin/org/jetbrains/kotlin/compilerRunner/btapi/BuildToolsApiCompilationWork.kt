/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import org.jetbrains.kotlin.buildtools.api.compilation.*
import org.jetbrains.kotlin.compilerRunner.GradleKotlinCompilerWorkArguments
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.internal.ParentClassLoaderProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import java.io.File

internal abstract class BuildToolsApiCompilationWork : WorkAction<BuildToolsApiCompilationWork.BuildToolsApiCompilationParameters> {
    internal interface BuildToolsApiCompilationParameters : WorkParameters {
        val classLoadersCachingService: Property<ClassLoadersCachingBuildService>
        val compilerWorkArguments: Property<GradleKotlinCompilerWorkArguments>
        val taskOutputsToRestore: ListProperty<File>
        val snapshotsDir: DirectoryProperty
        val buildDir: DirectoryProperty
        val metricsReporter: Property<BuildMetricsReporter>
    }

    private val workArguments
        get() = parameters.compilerWorkArguments.get()

    override fun execute() {
        val classLoader = parameters.classLoadersCachingService.get()
            .getClassLoader(workArguments.compilerFullClasspath, SharedApiClassesClassLoaderProvider)
        val compilationService = CompilationService.loadImplementation(classLoader)
        val compilerOptions = when (val strategy = workArguments.compilerExecutionSettings.strategy) {
            KotlinCompilerExecutionStrategy.DAEMON -> CompilerOptions.Daemon(
                workArguments.compilerFullClasspath,
                workArguments.projectFiles.sessionDir,
                workArguments.compilerExecutionSettings.daemonJvmArgs ?: emptyList()
            )
            KotlinCompilerExecutionStrategy.IN_PROCESS -> CompilerOptions.InProcess()
            else -> error("`$strategy` is an unsupported strategy for running via build-tools-api")
        }
        val compilationOptions = prepareCompilationOptions()
        compilationService.compile(
            compilerOptions,
            workArguments.compilerArgs.toList(),
            compilationOptions,
        )
    }

    private fun prepareCompilationOptions(): CompilationOptions {
        val kotlinScriptExtensions = workArguments.kotlinScriptExtensions.toList()
        val icEnv = workArguments.incrementalCompilationEnvironment
        return when {
            icEnv == null -> NonIncrementalCompilationOptions(workArguments.targetPlatform, kotlinScriptExtensions)
            icEnv.disableMultiModuleIC -> IntraModuleIncrementalCompilationOptions(
                workArguments.targetPlatform,
                kotlinScriptExtensions,
                icEnv.changedFiles,
                workArguments.outputFiles,
                icEnv.workingDir,
                icEnv.rootProjectDir,
            )
            icEnv.classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled -> ClasspathSnapshotBasedIncrementalCompilationOptions(
                workArguments.targetPlatform,
                kotlinScriptExtensions,
                icEnv.changedFiles,
                workArguments.outputFiles,
                icEnv.workingDir,
                icEnv.rootProjectDir,
                icEnv.classpathChanges,
            )
            else -> HistoryFilesBasedIncrementalCompilationOptions(
                workArguments.targetPlatform,
                kotlinScriptExtensions,
                icEnv.changedFiles,
                workArguments.outputFiles,
                icEnv.workingDir,
                icEnv.rootProjectDir,
                icEnv.multiModuleICSettings.buildHistoryFile,
                icEnv.multiModuleICSettings.useModuleDetection,
                workArguments.incrementalModuleInfo
                    ?: error("The build is configured to use the history-files based approach, but doesn't provide the modules meta information"),
            )
        }
    }
}

private object SharedApiClassesClassLoaderProvider : ParentClassLoaderProvider {
    override fun getClassLoader() = SharedApiClassesClassLoader()

    override fun hashCode() = SharedApiClassesClassLoaderProvider::class.hashCode()

    override fun equals(other: Any?) = other is SharedApiClassesClassLoaderProvider
}