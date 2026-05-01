package com.example.template.agent.runner

import com.example.template.common.SampleConstants
import jetbrains.buildServer.BuildProblemData
import jetbrains.buildServer.agent.BuildFinishedStatus
import jetbrains.buildServer.agent.BuildProcessAdapter
import jetbrains.buildServer.agent.BuildRunnerContext
import kotlin.concurrent.thread

// SAMPLE: delete or adapt.
class SampleBuildProcess(private val context: BuildRunnerContext) : BuildProcessAdapter() {

    @Volatile private var status: BuildFinishedStatus? = null
    @Volatile private var process: Process? = null

    override fun start() {
        val params = context.runnerParameters
        val message = params[SampleConstants.MESSAGE_PARAM] ?: SampleConstants.DEFAULT_MESSAGE
        val repeat = params[SampleConstants.REPEAT_PARAM]?.toIntOrNull()
            ?.coerceIn(1, SampleConstants.MAX_REPEAT)
            ?: SampleConstants.DEFAULT_REPEAT.toInt()

        if (SampleConstants.FAILURE_MARKER in message) {
            context.build.buildLogger.logBuildProblem(
                BuildProblemData.createBuildProblem(
                    "sample-fail-marker",
                    SampleConstants.BUILD_PROBLEM_TYPE,
                    "Message contains '${SampleConstants.FAILURE_MARKER}': $message",
                ),
            )
            status = BuildFinishedStatus.FINISHED_FAILED
            return
        }

        val logger = context.build.buildLogger
        repeat(repeat) { i ->
            logger.message("[${i + 1}/$repeat] $message")
        }

        // Spawn a subprocess to demonstrate ProcessBuilder + stdout capture; the date is just
        // a stand-in for "the runner doing real external work".
        val pb = ProcessBuilder(externalCommand()).redirectErrorStream(true)
        process = pb.start()
        val reader = thread(name = "sample-runner-stdout") {
            process?.inputStream?.bufferedReader()?.forEachLine(logger::message)
        }
        val exit = process?.waitFor() ?: 0
        reader.join()
        if (exit != 0) {
            logger.logBuildProblem(
                BuildProblemData.createBuildProblem(
                    "sample-subprocess-exit-$exit",
                    SampleConstants.BUILD_PROBLEM_TYPE,
                    "Subprocess exited with code $exit",
                ),
            )
            status = BuildFinishedStatus.FINISHED_FAILED
            return
        }
        status = BuildFinishedStatus.FINISHED_SUCCESS
    }

    override fun interrupt() {
        process?.takeIf { it.isAlive }?.destroy()
        super.interrupt()
    }

    override fun isFinished(): Boolean = status != null

    override fun waitFor(): BuildFinishedStatus = status ?: BuildFinishedStatus.FINISHED_FAILED

    private fun externalCommand(): List<String> =
        if (System.getProperty("os.name").lowercase().startsWith("windows")) {
            listOf("cmd.exe", "/c", "echo", "subprocess ran at %date% %time%")
        } else {
            listOf("/bin/sh", "-c", "echo \"subprocess ran at $(date)\"")
        }
}
