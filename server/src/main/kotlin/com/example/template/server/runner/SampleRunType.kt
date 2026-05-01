package com.example.template.server.runner

import com.example.template.common.SampleConstants
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.serverSide.RunType
import jetbrains.buildServer.serverSide.RunTypeRegistry
import jetbrains.buildServer.web.openapi.PluginDescriptor

// SAMPLE: delete or adapt. Server-side runner registration; pairs with SampleAgentBuildRunner via SampleConstants.
class SampleRunType(
    registry: RunTypeRegistry,
    private val pluginDescriptor: PluginDescriptor,
) : RunType() {

    init {
        registry.registerRunType(this)
    }

    override fun getType(): String = SampleConstants.RUNNER_TYPE
    override fun getDisplayName(): String = "Sample Runner"
    override fun getDescription(): String = "Logs a configurable message N times and fails the build if the message contains '${SampleConstants.FAILURE_MARKER}'."

    override fun getDefaultRunnerProperties(): Map<String, String> = mapOf(
        SampleConstants.MESSAGE_PARAM to SampleConstants.DEFAULT_MESSAGE,
        SampleConstants.REPEAT_PARAM to SampleConstants.DEFAULT_REPEAT,
    )

    override fun describeParameters(parameters: Map<String, String>): String {
        val msg = parameters[SampleConstants.MESSAGE_PARAM] ?: SampleConstants.DEFAULT_MESSAGE
        val repeat = parameters[SampleConstants.REPEAT_PARAM] ?: SampleConstants.DEFAULT_REPEAT
        return "$repeat × $msg"
    }

    override fun getEditRunnerParamsJspFilePath(): String =
        pluginDescriptor.getPluginResourcesPath("editSampleRunner.jsp")

    override fun getViewRunnerParamsJspFilePath(): String? = null

    override fun getRunnerPropertiesProcessor(): PropertiesProcessor = SampleRunnerPropertiesProcessor()
}
