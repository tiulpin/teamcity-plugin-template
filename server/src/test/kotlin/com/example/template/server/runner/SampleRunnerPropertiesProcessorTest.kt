package com.example.template.server.runner

import com.example.template.common.SampleConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleRunnerPropertiesProcessorTest {

    private val p = SampleRunnerPropertiesProcessor()

    @Test
    fun `valid properties pass`() {
        val errors = p.process(mapOf(
            SampleConstants.MESSAGE_PARAM to "hello",
            SampleConstants.REPEAT_PARAM to "5",
        ))
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `blank message is rejected`() {
        val errors = p.process(mapOf(
            SampleConstants.MESSAGE_PARAM to "  ",
            SampleConstants.REPEAT_PARAM to "1",
        )).associateBy { it.propertyName }
        assertTrue("expected error on message", errors.containsKey(SampleConstants.MESSAGE_PARAM))
    }

    @Test
    fun `non-integer repeat is rejected`() {
        val errors = p.process(mapOf(
            SampleConstants.MESSAGE_PARAM to "hi",
            SampleConstants.REPEAT_PARAM to "twice",
        )).associateBy { it.propertyName }
        assertTrue(errors.containsKey(SampleConstants.REPEAT_PARAM))
    }

    @Test
    fun `repeat outside 1-100 is rejected`() {
        for (bad in listOf("0", "-1", "101")) {
            val errors = p.process(mapOf(
                SampleConstants.MESSAGE_PARAM to "hi",
                SampleConstants.REPEAT_PARAM to bad,
            )).associateBy { it.propertyName }
            assertTrue("expected error for repeat=$bad", errors.containsKey(SampleConstants.REPEAT_PARAM))
        }
    }

    @Test
    fun `null properties produces both errors`() {
        val errors = p.process(null).associateBy { it.propertyName }
        assertEquals(setOf(SampleConstants.MESSAGE_PARAM, SampleConstants.REPEAT_PARAM), errors.keys)
    }
}
