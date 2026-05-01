package com.example.template.agent.runner

import com.example.template.common.SampleConstants
import jetbrains.buildServer.agent.AgentBuildRunner
import jetbrains.buildServer.agent.AgentBuildRunnerInfo
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.BuildAgentConfiguration
import jetbrains.buildServer.agent.BuildProcess
import jetbrains.buildServer.agent.BuildRunnerContext

// SAMPLE: delete or adapt. For runners that exec a binary, use CommandLineBuildServiceFactory instead.
class SampleAgentBuildRunner : AgentBuildRunner, AgentBuildRunnerInfo {

    override fun createBuildProcess(runningBuild: AgentRunningBuild, context: BuildRunnerContext): BuildProcess =
        SampleBuildProcess(context)

    override fun getRunnerInfo(): AgentBuildRunnerInfo = this

    override fun getType(): String = SampleConstants.RUNNER_TYPE

    override fun canRun(agentConfiguration: BuildAgentConfiguration): Boolean = true
}
