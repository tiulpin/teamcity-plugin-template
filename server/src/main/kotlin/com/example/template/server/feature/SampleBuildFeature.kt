package com.example.template.server.feature

import jetbrains.buildServer.serverSide.BuildFeature

// SAMPLE: delete or adapt. Return a JSP path from getEditParametersUrl() to add a settings editor.
class SampleBuildFeature : BuildFeature() {

    override fun getType(): String = FEATURE_TYPE
    override fun getDisplayName(): String = "Sample Build Feature"
    override fun getEditParametersUrl(): String? = null

    override fun describeParameters(params: Map<String, String>): String =
        "Sample mode: ${params[MODE_PARAM] ?: DEFAULT_MODE}"

    override fun getDefaultParameters(): Map<String, String> = mapOf(
        MODE_PARAM to DEFAULT_MODE,
    )

    override fun isMultipleFeaturesPerBuildTypeAllowed(): Boolean = false

    companion object {
        const val FEATURE_TYPE = "sample.build.feature"
        const val MODE_PARAM = "sample.mode"
        const val DEFAULT_MODE = "info"
    }
}
