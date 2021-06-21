package com.dxc.ssi.agent.didcomm.processor.abandon

import com.dxc.ssi.agent.model.PeerConnection
import com.dxc.ssi.agent.model.messages.Message
import com.dxc.ssi.agent.model.messages.MessageContext

//TODO: for now this class won;t be part of abstraction, once it is implemented see if it is posible to generalize it with MessageProcessor and AbstractMessageProcessor

interface AbandonConnectionProcessor {
    suspend fun processMessage(messageContext: MessageContext)
    suspend fun abandonConnection(connection: PeerConnection, notifyPeerBeforeAbandoning: Boolean)

}
