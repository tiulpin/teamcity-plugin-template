package com.example.template.common

// SAMPLE: delete or adapt. Constants must be loadable from both server and agent classloaders.
object SampleConstants {
    const val RUNNER_TYPE = "sample-runner"

    const val MESSAGE_PARAM = "sample.message"
    const val REPEAT_PARAM = "sample.repeat"

    const val DEFAULT_MESSAGE = "Hello from teamcity-plugin-template"
    const val DEFAULT_REPEAT = "1"

    const val MAX_REPEAT = 100

    /** Substring that, if present in the message, makes the runner fail the build. */
    const val FAILURE_MARKER = "FAIL"

    const val BUILD_PROBLEM_TYPE = "SampleRunnerProblem"
}
