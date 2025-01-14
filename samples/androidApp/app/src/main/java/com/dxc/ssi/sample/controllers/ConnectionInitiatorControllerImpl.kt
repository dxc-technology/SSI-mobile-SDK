package com.dxc.ssi.sample.controllers

import com.dxc.ssi.agent.api.callbacks.CallbackResult
import com.dxc.ssi.agent.api.callbacks.didexchange.ConnectionInitiatorController
import com.dxc.ssi.agent.api.callbacks.didexchange.DidExchangeError
import com.dxc.ssi.agent.didcomm.model.didexchange.ConnectionRequest
import com.dxc.ssi.agent.didcomm.model.didexchange.ConnectionResponse
import com.dxc.ssi.agent.didcomm.model.didexchange.Invitation
import com.dxc.ssi.agent.didcomm.model.problem.ProblemReport
import com.dxc.ssi.agent.model.PeerConnection
import com.dxc.ssi.agent.kermit.Kermit
import com.dxc.ssi.agent.kermit.LogcatLogger
import com.dxc.ssi.agent.kermit.Severity

class ConnectionInitiatorControllerImpl : ConnectionInitiatorController {
    var logger: Kermit = Kermit(LogcatLogger())

    override fun onRequestSent(connection: PeerConnection, request: ConnectionRequest) {
        logger.d {"Request sent hook called : $connection, $request" }

    }

    override fun onResponseReceived(
        connection: PeerConnection,
        response: ConnectionResponse
    ): CallbackResult {
        logger.d { "Response received hook called : $connection, $response" }
        return CallbackResult(true)
    }

    override fun onAbandoned(connection: PeerConnection, problemReport: ProblemReport?) {
        logger.d { "Connection abandoned : $connection" }
    }

    override fun onCompleted(connection: PeerConnection) {
        logger.d { "Connection completed : $connection" }
    }

    override fun onFailure(
        connection: PeerConnection?,
        error: DidExchangeError,
        message: String?,
        details: String?,
        stackTrace: String?
    ) {
        logger.d{ "Failure to establish connection:" +
                "connection -> $connection" +
                "error -> $error" +
                "message -> $message" +
                "details -> $details" +
                "stackTrace -> $stackTrace" }
    }

    override fun onInvitationReceived(connection: PeerConnection, invitation: Invitation): CallbackResult {
        logger.d { "Invitation received hook called : $connection, $invitation" }
        return CallbackResult(canProceedFurther = true)
    }

}