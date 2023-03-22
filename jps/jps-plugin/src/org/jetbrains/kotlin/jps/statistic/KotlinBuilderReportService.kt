/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.statistic

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.statistic.HttpReportServiceImpl
import org.jetbrains.kotlin.build.report.statistic.file.FileReportService
import org.jetbrains.kotlin.gradle.plugin.stat.BuildDataType
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatisticsData
import java.io.File
import java.io.Serializable
import java.util.*
import java.util.concurrent.TimeUnit






interface KotlinBuilderMetric {
    fun report(metric: BuildTime, value: Long)
    fun start(metric: BuildTime)
    fun finish(metric: BuildTime)

    fun flush(context: CompileContext): CompileStatisticsData
}

//single thread execution
class KotlinBuilderMetricImpl : KotlinBuilderMetric {
    companion object {
        private val log = Logger.getInstance("#org.jetbrains.kotlin.jps.statistic.KotlinBuilderMetricImpl")
    }

    private val buildTimes = EnumMap<BuildTime, Long>(BuildTime::class.java)
    private val buildMetricsInProcess = EnumMap<BuildTime, Long>(BuildTime::class.java)
    private val uuid = UUID.randomUUID()
    private val startTime = System.currentTimeMillis()
    override fun report(metric: BuildTime, value: Long) {
        buildTimes[metric] = value
    }

    override fun start(metric: BuildTime) {
        if (buildMetricsInProcess[metric] != null) {
            log.error("$metric is already in process")
        } else {
            buildMetricsInProcess[metric] = System.nanoTime()
        }
    }

    override fun finish(metric: BuildTime) {
        val value = buildMetricsInProcess.remove(metric)
        if (value == null) {
            log.error("$metric hasn't started")
        } else {
            buildTimes[metric] = TimeUnit.NANOSECONDS.toMillis(value)
        }
    }

    override fun flush(context: CompileContext/*, listener: BuildListener*/): CompileStatisticsData {
        if (buildMetricsInProcess.isNotEmpty()) {
            log.error("Finish metric calcultaion, but ${buildMetricsInProcess.keys} metrics are in progress")
        }
        return CompileStatisticsData(
            version = 99, //TODO
            projectName = context.projectDescriptor.project.name,
            label = "JPS build", //TODO
            taskName = "JPS build", //TODO
            taskResult = "Unknown",
            startTimeMs = startTime,
            durationMs = System.currentTimeMillis() - startTime,
            tags = emptyList(), //TODO
            buildUuid = uuid.toString(),
            changes = emptyList(), //TODO
            kotlinVersion = "kotlin_version", //TODO
            hostName = "test", //TODO
            finishTime = System.currentTimeMillis(),
            buildTimesMetrics = buildTimes,
            performanceMetrics = emptyMap(),
            compilerArguments = emptyList(), //TODO
            nonIncrementalAttributes = emptySet(),
            type = BuildDataType.JPS_DATA.name,
            fromKotlinPlugin = true,
            compiledSources = emptyList(),
            skipMessage = null,
            icLogLines = emptyList(),
            gcTimeMetrics = null,//TODO
            gcCountMetrics = null,//TODO
        )

    }
}


data class FileReportSettings(
    val buildReportDir: File,
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 0
        fun init(): FileReportSettings? {
            return System.getProperty("kotlin.build.report.file.output_dir")?.let { FileReportSettings(File(it)) }
        }
    }
}

data class HttpReportSettings(
    val url: String,
    val password: String?,
    val user: String?,
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 0

        fun init(): HttpReportSettings? {
            val httpReportUrl = System.getProperty("kotlin.build.report.http.url") ?: return null
            val httpReportUser = System.getProperty("kotlin.build.report.http.user")
            val httpReportPassword = System.getProperty("kotlin.build.report.http.password")
            return HttpReportSettings(httpReportUrl, httpReportUser, httpReportPassword)
        }
    }
}

// TODO test UserDataHolder in CompileContext to store CompileStatisticsData.Build or KotlinBuilderMetric
class KotlinBuilderReportService(
    private val fileReportSettings: FileReportSettings?,
    private val httpReportSettings: HttpReportSettings?
) {
    constructor() : this(FileReportSettings.init(), HttpReportSettings.init())

    private val contextMetrics = HashMap<CompileContext, KotlinBuilderMetric>()
    private val log = Logger.getInstance("#org.jetbrains.kotlin.jps.statistic.KotlinBuilderReportService")
    private val loggerAdapter = JpsLoggerAdapter(log)
    private val httpService = httpReportSettings?.let { HttpReportServiceImpl(it.url, it.user, it.password) }
    fun buildStarted(context: CompileContext) {
        if (contextMetrics[context] != null) {
            log.error("Service already initialized for context")
        }
        contextMetrics[context] = KotlinBuilderMetricImpl()
    }

    fun buildFinished(context: CompileContext) {
        val metrics = contextMetrics.remove(context)
        if (metrics == null) {
            log.error("Service hasn't initialized for context")
            return
        }

        httpService?.sendData(metrics.flush(context), loggerAdapter)
        fileReportSettings?.also { FileReportService(it.buildReportDir, true, loggerAdapter) }
    }

    fun addMetric(context: CompileContext, metric: BuildTime, value: Long) {
        val metrics = contextMetrics[context]
        if (metrics == null) {
            log.error("Service hasn't initialized for context")
            return
        }
        metrics.report(metric, value)
    }
}



