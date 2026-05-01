package com.example.template.server.runner

import com.example.template.common.SampleConstants
import jetbrains.buildServer.serverSide.RunType
import jetbrains.buildServer.serverSide.RunTypeRegistry

// SAMPLE: delete or adapt. Server-side runner registration; pairs with SampleAgentBuildRunner via SampleConstants.
class SampleRunType(registry: RunTypeRegistry) : RunType() {

    init {
        registry.registerRunType(this)
    }

    override fun getType(): String = SampleConstants.RUNNER_TYPE
    override fun getDisplayName(): String = "Sample Runner"
    override fun getDescription(): String = "Logs a configurable message to the build log."

    override fun getDefaultRunnerProperties(): Map<String, String> = mapOf(
        SampleConstants.MESSAGE_PARAM to SampleConstants.DEFAULT_MESSAGE,
    )

    override fun describeParameters(parameters: Map<String, String>): String =
        "Message: ${parameters[SampleConstants.MESSAGE_PARAM] ?: SampleConstants.DEFAULT_MESSAGE}"

    // Returning null disables the params editor — users get the defaults as-is.
    // To add an editor, return pluginDescriptor.getPluginResourcesPath("editSampleRunner.jsp").
    override fun getEditRunnerParamsJspFilePath(): String? = null
    override fun getViewRunnerParamsJspFilePath(): String? = null

    override fun getRunnerPropertiesProcessor() = null
}
