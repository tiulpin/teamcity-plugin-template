package com.example.template.server.web

import jetbrains.buildServer.web.openapi.PagePlaces
import jetbrains.buildServer.web.openapi.PlaceId
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.SimplePageExtension

// SAMPLE: delete or adapt. JSP lives in server/src/main/resources/buildServerResources/.
class SamplePageExtension(
    pagePlaces: PagePlaces,
    descriptor: PluginDescriptor,
) : SimplePageExtension(
    pagePlaces,
    PlaceId.BUILD_RESULTS_FRAGMENT,
    descriptor.pluginName,
    descriptor.getPluginResourcesPath("samplePage.jsp"),
) {
    init {
        register()
    }
}
