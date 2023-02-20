/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.statistic.file

import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.statistic.LoggerAdapter
import org.jetbrains.kotlin.build.report.statistic.asString
import org.jetbrains.kotlin.build.report.statistic.formatSize
import org.jetbrains.kotlin.build.report.statistic.formatTime
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatisticsData
import org.jetbrains.kotlin.gradle.plugin.stat.GradleBuildStartParameters
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class FileReportService(
    private val outputFile: File,
    private val printMetrics: Boolean,
    private val logger: LoggerAdapter
) : Serializable {
    companion object {
        fun reportBuildStatInFile(
            buildReportDir: File,
            projectName: String,
            includeMetricsInReport: Boolean,
            buildData: List<CompileStatisticsData>,
            startParameters: GradleBuildStartParameters,
            failureMessages: List<String>,
            logger: LoggerAdapter
        ) {
            val ts = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().time)
            val reportFile = buildReportDir.resolve("$projectName-build-$ts.txt")

            FileReportService(
                outputFile = reportFile,
                printMetrics = includeMetricsInReport,
                logger = logger
            ).process(buildData, startParameters, failureMessages)
        }
    }

    private lateinit var p: Printer

    fun process(
        statisticsData: List<CompileStatisticsData>,
        startParameters: GradleBuildStartParameters,
        failureMessages: List<String>
    ) {
        val buildReportPath = outputFile.toPath().toUri().toString()
        try {
            outputFile.parentFile.mkdirs()
            if (!(outputFile.parentFile.exists() && outputFile.parentFile.isDirectory)) {
                logger.error("Kotlin build report cannot be created: '$outputFile.parentFile' is a file or do not have permissions to create")
                return
            }

            outputFile.bufferedWriter().use { writer ->
                p = Printer(writer)
                printBuildReport(statisticsData, startParameters, failureMessages)
            }

            logger.lifecycle("Kotlin build report is written to $buildReportPath")
        } catch (e: Exception) {
            logger.error("Could not write Kotlin build report to $buildReportPath", e)
        }
    }

    private fun printBuildReport(
        statisticsData: List<CompileStatisticsData>,
        startParameters: GradleBuildStartParameters,
        failureMessages: List<String>
    ) {
        // NOTE: BuildExecutionData / BuildOperationRecord contains data for both tasks and transforms.
        // Where possible, we still use the term "tasks" because saying "tasks/transforms" is a bit verbose and "build operations" may sound
        // a bit unfamiliar.
        // TODO: If it is confusing, consider renaming "tasks" to "build operations" in this class.
        printBuildInfo(startParameters, failureMessages)
        if (printMetrics) {
            printMetrics(
                statisticsData.map { it.buildTimesMetrics }.reduce { agg, value ->
                    (agg.keys + value.keys).associateWith { (agg[it] ?: 0) + (value[it] ?: 0) }
                },
                statisticsData.map { it.performanceMetrics }.reduce { agg, value ->
                    (agg.keys + value.keys).associateWith { (agg[it] ?: 0) + (value[it] ?: 0) }
                },
                statisticsData.map { it.nonIncrementalAttributes.asSequence() }.reduce { agg, value -> agg + value }.toList(),
                newLineBetweenSections = true
            )
            p.println()
        }
        printTaskOverview(statisticsData)
        printTasksLog(statisticsData)
    }

    private fun printBuildInfo(startParameters: GradleBuildStartParameters, failureMessages: List<String>) {
        p.withIndent("Gradle start parameters:") {
            startParameters.let {
                p.println("tasks = ${it.tasks}")
                p.println("excluded tasks = ${it.excludedTasks}")
                p.println("current dir = ${it.currentDir}")
                p.println("project properties args = ${it.projectProperties}")
                p.println("system properties args = ${it.systemProperties}")
            }
        }
        p.println()

        if (failureMessages.isNotEmpty()) {
            p.println("Build failed: ${failureMessages}")
            p.println()
        }
    }

    private fun printMetrics(
        buildTimesMetrics: Map<BuildTime, Long>,
        performanceMetrics: Map<BuildPerformanceMetric, Long>,
        nonIncrementalAttributes: Collection<BuildAttribute>,
        newLineBetweenSections: Boolean = false
    ) {
        printBuildTimes(buildTimesMetrics)
        if (newLineBetweenSections) p.println()

        printBuildPerformanceMetrics(performanceMetrics)
        if (newLineBetweenSections) p.println()

        printBuildAttributes(nonIncrementalAttributes)
    }

    private fun printBuildTimes(buildTimes: Map<BuildTime, Long>) {
        if (buildTimes.isEmpty()) return

        p.println("Time metrics:")
        p.withIndent {
            val visitedBuildTimes = HashSet<BuildTime>()
            fun printBuildTime(buildTime: BuildTime) {
                if (!visitedBuildTimes.add(buildTime)) return

                val timeMs = buildTimes[buildTime]
                if (timeMs != null) {
                    p.println("${buildTime.readableString}: ${formatTime(timeMs)}")
                    p.withIndent {
                        BuildTime.children[buildTime]?.forEach { printBuildTime(it) }
                    }
                } else {
                    //Skip formatting if parent metric does not set
                    BuildTime.children[buildTime]?.forEach { printBuildTime(it) }
                }
            }

            for (buildTime in BuildTime.values()) {
                if (buildTime.parent != null) continue

                printBuildTime(buildTime)
            }
        }
    }

    private fun printBuildPerformanceMetrics(buildMetrics: Map<BuildPerformanceMetric, Long>) {
        if (buildMetrics.isEmpty()) return

        p.withIndent("Size metrics:") {
            for (metric in BuildPerformanceMetric.values()) {
                buildMetrics[metric]?.let { printSizeMetric(metric, it) }
            }
        }
    }

    private fun printSizeMetric(sizeMetric: BuildPerformanceMetric, value: Long) {
        fun BuildPerformanceMetric.numberOfAncestors(): Int {
            var count = 0
            var parent: BuildPerformanceMetric? = parent
            while (parent != null) {
                count++
                parent = parent.parent
            }
            return count
        }

        val indentLevel = sizeMetric.numberOfAncestors()

        repeat(indentLevel) { p.pushIndent() }
        when (sizeMetric.type) {
            ValueType.BYTES -> p.println("${sizeMetric.readableString}: ${formatSize(value)}")
            ValueType.NUMBER -> p.println("${sizeMetric.readableString}: $value")
            ValueType.NANOSECONDS -> TODO()
            ValueType.MILLISECONDS -> TODO()
        }
        repeat(indentLevel) { p.popIndent() }
    }

    private fun printBuildAttributes(buildAttributes: Collection<BuildAttribute>) {
        if (buildAttributes.isEmpty()) return

        val buildAttributesMap = buildAttributes.groupingBy { it }.eachCount()
        p.withIndent("Build attributes:") {
            val attributesByKind = buildAttributesMap.entries.groupBy { it.key.kind }.toSortedMap()
            for ((kind, attributesCounts) in attributesByKind) {
                printMap(p, kind.name, attributesCounts.associate { (k, v) -> k.readableString to v })
            }
        }
    }

    private fun printTaskOverview(statisticsData: Collection<CompileStatisticsData>) {
        var allTasksTimeMs = 0L
        var kotlinTotalTimeMs = 0L
        val kotlinTasks = ArrayList<CompileStatisticsData>()

        for (task in statisticsData) {
            val taskTimeMs = task.durationMs
            allTasksTimeMs += taskTimeMs

            if (task.fromKotlinPlugin == true) {
                kotlinTotalTimeMs += taskTimeMs
                kotlinTasks.add(task)
            }
        }

        if (kotlinTasks.isEmpty()) {
            p.println("No Kotlin task was run")
            return
        }

        val ktTaskPercent = (kotlinTotalTimeMs.toDouble() / allTasksTimeMs * 100).asString(1)
        p.println("Total time for Kotlin tasks: ${formatTime(kotlinTotalTimeMs)} ($ktTaskPercent % of all tasks time)")

        val table = TextTable("Time", "% of Kotlin time", "Task")
        for (task in kotlinTasks.sortedWith(compareBy({ -it.durationMs }, { it.startTimeMs }))) {
            val timeMs = task.durationMs
            val percent = (timeMs.toDouble() / kotlinTotalTimeMs * 100).asString(1)
            table.addRow(formatTime(timeMs), "$percent %", task.taskName)
        }
        table.printTo(p)
        p.println()
    }

    private fun printTasksLog(statisticsData: List<CompileStatisticsData>) {
        for (task in statisticsData.sortedWith(compareBy({ -it.durationMs }, { it.startTimeMs }))) {
            printTaskLog(task)
            p.println()
        }
    }

    private fun printTaskLog(statisticsData: CompileStatisticsData) {
        val skipMessage = statisticsData.skipMessage
        if (skipMessage != null) {
            p.println("Task '${statisticsData.taskName}' was skipped: $skipMessage")
        } else {
            p.println("Task '${statisticsData.taskName}' finished in ${formatTime(statisticsData.durationMs)}")
        }

        if (statisticsData.icLogLines.isNotEmpty()) {
            p.withIndent("Compilation log for task '${statisticsData.taskName}':") {
                statisticsData.icLogLines.forEach { p.println(it) }
            }
        }

        if (printMetrics) {
            printMetrics(statisticsData.buildTimesMetrics, statisticsData.performanceMetrics, statisticsData.nonIncrementalAttributes)
        }
    }
}