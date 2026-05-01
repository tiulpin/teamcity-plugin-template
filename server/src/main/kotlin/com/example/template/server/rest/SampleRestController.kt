package com.example.template.server.rest

import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.web.openapi.WebControllerManager
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.springframework.web.servlet.ModelAndView

// SAMPLE: delete or adapt. /app/* is TC-internal; namespace plugin controllers under /<plugin-name>/*.
class SampleRestController(
    server: SBuildServer,
    manager: WebControllerManager,
) : BaseController(server) {

    init {
        manager.registerController(PATH, this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        response.contentType = "application/json"
        response.writer.println("""{"status":"ok","plugin":"teamcity-plugin-template"}""")
        return null
    }

    companion object {
        const val PATH = "/sample-plugin/status.html"
    }
}
