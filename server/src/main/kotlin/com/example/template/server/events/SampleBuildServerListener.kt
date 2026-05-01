package com.example.template.server.events

import jetbrains.buildServer.log.Loggers
import jetbrains.buildServer.serverSide.BuildServerAdapter
import jetbrains.buildServer.serverSide.BuildServerListener
import jetbrains.buildServer.serverSide.SRunningBuild
import jetbrains.buildServer.util.EventDispatcher

// SAMPLE: delete or adapt.
class SampleBuildServerListener(
    events: EventDispatcher<BuildServerListener>,
) : BuildServerAdapter() {

    init {
        events.addListener(this)
    }

    override fun buildFinished(build: SRunningBuild) {
        Loggers.SERVER.info("Sample plugin saw build finish: id=${build.buildId} status=${build.buildStatus}")
    }
}
