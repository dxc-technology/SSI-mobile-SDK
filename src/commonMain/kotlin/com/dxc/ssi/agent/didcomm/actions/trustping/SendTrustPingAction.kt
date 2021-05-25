package com.dxc.ssi.agent.didcomm.actions.trustping

import com.benasher44.uuid.uuid4
import com.dxc.ssi.agent.api.pluggable.Transport
import com.dxc.ssi.agent.api.pluggable.wallet.WalletConnector
import com.dxc.ssi.agent.didcomm.actions.ActionResult
import com.dxc.ssi.agent.didcomm.commoon.MessagePacker
import com.dxc.ssi.agent.didcomm.model.trustping.TrustPingRequest
import com.dxc.ssi.agent.didcomm.model.trustping.TrustPingResponse
import com.dxc.ssi.agent.didcomm.services.TrustPingTrackerService
import com.dxc.ssi.agent.model.Connection
import com.dxc.ssi.agent.model.messages.Message
import com.dxc.ssi.agent.model.messages.ReceivedUnpackedMessage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SendTrustPingAction(
    val walletConnector: WalletConnector,
    val transport: Transport,
    val trustPingTrackerService: TrustPingTrackerService,
    private val connection: Connection
) {

    suspend fun perform(): ActionResult {
        //TODO: make TrustPing Stateless

        // 1. Form TrustPingRequestMessage

        val requestId = uuid4().toString()

        val trustPingRequest =
            TrustPingRequest(
                type = "https://didcomm.org/trust_ping/1.0/ping",
                id = requestId,
                responseRequested = true
            )


        val messageToSend =
            MessagePacker.packAndPrepareForwardMessage(
                Message(Json.encodeToString(trustPingRequest)),
                connection,
                walletConnector
            )

        // 2. Send TrustPingRequestMessage

        //TODO: ensure that transport function is synchronous here because we will save new status to wallet only after actual message was sent
        transport.sendMessage(connection, messageToSend)
        trustPingTrackerService.trustPingSentOverConnectionEvent(connection)


        //TODO: instead of true, set some other status, like trustPing is sent, and we do not know the result yet
        return ActionResult(trustPingSuccessful = true)

    }
}