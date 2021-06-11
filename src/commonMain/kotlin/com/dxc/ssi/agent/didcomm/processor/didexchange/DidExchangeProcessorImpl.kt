package com.dxc.ssi.agent.didcomm.processor.didexchange

import com.dxc.ssi.agent.api.Callbacks
import com.dxc.ssi.agent.api.pluggable.LedgerConnector
import com.dxc.ssi.agent.api.pluggable.Transport
import com.dxc.ssi.agent.api.pluggable.wallet.WalletConnector
import com.dxc.ssi.agent.didcomm.actions.Action
import com.dxc.ssi.agent.didcomm.actions.ActionParams
import com.dxc.ssi.agent.didcomm.actions.didexchange.impl.ReceiveConnectionResponseAction
import com.dxc.ssi.agent.didcomm.actions.didexchange.impl.ReceiveInvitationAction
import com.dxc.ssi.agent.didcomm.processor.AbstractMessageProcessor
import com.dxc.ssi.agent.didcomm.processor.MessageType
import com.dxc.ssi.agent.didcomm.processor.trustping.TrustPingProcessor
import com.dxc.ssi.agent.didcomm.services.TrustPingTrackerService
import com.dxc.ssi.agent.model.Connection

//TODO: for now this class won;t be part of abstraction, once it is implemented see if it is posible to generalize it with MessageProcessor and AbstractMessageProcessor
//TODO: rename DidExchange to Connection everywhere? Since Did exchange spec is not yet in active status
class DidExchangeProcessorImpl(
    walletConnector: WalletConnector,
    ledgerConnector: LedgerConnector, transport: Transport, callbacks: Callbacks,
    trustPingProcessor: TrustPingProcessor, trustPingTrackerService: TrustPingTrackerService
) : AbstractMessageProcessor(
    walletConnector, ledgerConnector, transport, callbacks, trustPingProcessor,
    trustPingTrackerService
), DidExchangeProcessor {


    override suspend fun initiateConnectionByInvitation(invitation: String): Connection {

        //TODO: think how to avoid NPE here
        val receiveInvitationAction =
            ReceiveInvitationAction(walletConnector, transport, callbacks.connectionInitiatorController!!, invitation)
        return receiveInvitationAction.perform().connection!!
    }


    enum class DidExchangeMessageType(val _typeString: String, val _action: (ActionParams) -> Action) : MessageType {
        //TODO:  add ProblemReport/error message
        INVITATION("^.*connections/1.0/invitation$", { actionParams -> kotlin.TODO("Not implemented") }),
        CONNECTION_REQUEST("^.*connections/1.0/request$", { actionParams -> kotlin.TODO("Not implemented") }),
        CONNECTION_RESPONSE(
            "^.*connections/1.0/response$",
            { actionParams -> ReceiveConnectionResponseAction(actionParams) });

        override fun getTypeString(): String = _typeString
        override fun getMessageHandler(): (ActionParams) -> Action = _action
    }

    override fun getMessageType(message: String): MessageType {
        return getMessageTypeGeneric<DidExchangeMessageType>(message)
    }


}
