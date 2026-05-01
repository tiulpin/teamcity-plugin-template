package com.example.template.server.feature

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SampleBuildFeatureTest {

    private val feature = SampleBuildFeature()

    @Test
    fun `default parameters include the mode`() {
        assertEquals(SampleBuildFeature.DEFAULT_MODE, feature.defaultParameters?.get(SampleBuildFeature.MODE_PARAM))
    }

    @Test
    fun `description reflects the mode parameter`() {
        val description = feature.describeParameters(mapOf(SampleBuildFeature.MODE_PARAM to "verbose"))
        assertEquals("Sample mode: verbose", description)
    }

    @Test
    fun `feature is single-instance per build type`() {
        assertFalse(feature.isMultipleFeaturesPerBuildTypeAllowed)
    }
}
