package com.example.template.agent.runner

import com.example.template.common.SampleConstants
import jetbrains.buildServer.agent.BuildFinishedStatus
import jetbrains.buildServer.agent.BuildProcessAdapter
import jetbrains.buildServer.agent.BuildRunnerContext

// SAMPLE: delete or adapt.
class SampleBuildProcess(private val context: BuildRunnerContext) : BuildProcessAdapter() {

    @Volatile private var finished = false

    override fun start() {
        val message = context.runnerParameters[SampleConstants.MESSAGE_PARAM] ?: SampleConstants.DEFAULT_MESSAGE
        context.build.buildLogger.message(message)
        finished = true
    }

    override fun isFinished(): Boolean = finished

    override fun waitFor(): BuildFinishedStatus = BuildFinishedStatus.FINISHED_SUCCESS
}
