package com.dxc.ssi.sample.controllers

import com.dxc.ssi.agent.api.callbacks.CallbackResult
import com.dxc.ssi.agent.api.callbacks.verification.CredPresenterController
import com.dxc.ssi.agent.didcomm.model.problem.ProblemReport
import com.dxc.ssi.agent.didcomm.model.verify.container.PresentationRequestContainer
import com.dxc.ssi.agent.model.PeerConnection
import com.dxc.ssi.agent.model.PresentationRequestResponseAction
import com.dxc.ssi.agent.kermit.Kermit
import com.dxc.ssi.agent.kermit.LogcatLogger
import com.dxc.ssi.agent.kermit.Severity
import com.dxc.ssi.sample.ssiAgentApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CredPresenterControllerImpl : CredPresenterController {
    override fun onRequestReceived(
        connection: PeerConnection,
        presentationRequest: PresentationRequestContainer
    ): PresentationRequestResponseAction {
        var logger: Kermit = Kermit(LogcatLogger())

        GlobalScope.launch {
            delay(10_000)

            logger.d { "Woken up..." }

            ssiAgentApi!!.getParkedPresentationRequests().forEach { presentationRequestContainer ->

                logger.d { "Accepting parked presentation request $presentationRequestContainer"}

                ssiAgentApi!!.processParkedPresentationRequest(
                    presentationRequestContainer,
                    PresentationRequestResponseAction.ACCEPT
                )
            }
        }

        return PresentationRequestResponseAction.PARK
    }

    override fun onDone(connection: PeerConnection) {

    }

    override fun onProblemReportGenerated(connection: PeerConnection, problemReport: ProblemReport) {

    }

}