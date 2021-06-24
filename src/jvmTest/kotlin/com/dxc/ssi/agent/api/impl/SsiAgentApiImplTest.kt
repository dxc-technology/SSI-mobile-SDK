package com.dxc.ssi.agent.api.impl

import com.dxc.ssi.agent.api.callbacks.CallbackResult
import com.dxc.ssi.agent.api.callbacks.didexchange.ConnectionInitiatorController
import com.dxc.ssi.agent.api.callbacks.issue.CredReceiverController
import com.dxc.ssi.agent.api.callbacks.verification.CredPresenterController
import com.dxc.ssi.agent.api.pluggable.wallet.WalletManager
import com.dxc.ssi.agent.api.pluggable.wallet.indy.IndyWalletConnector
import com.dxc.ssi.agent.didcomm.model.didexchange.ConnectionRequest
import com.dxc.ssi.agent.didcomm.model.didexchange.ConnectionResponse
import com.dxc.ssi.agent.didcomm.model.didexchange.Invitation
import com.dxc.ssi.agent.didcomm.model.issue.container.CredentialContainer
import com.dxc.ssi.agent.didcomm.model.issue.container.CredentialOfferContainer
import com.dxc.ssi.agent.didcomm.model.issue.container.CredentialRequestContainer
import com.dxc.ssi.agent.didcomm.model.problem.ProblemReport
import com.dxc.ssi.agent.didcomm.model.verify.container.PresentationRequestContainer
import com.dxc.ssi.agent.ledger.indy.IndyLedgerConnector
import com.dxc.ssi.agent.ledger.indy.IndyLedgerConnectorConfiguration
import com.dxc.ssi.agent.model.PeerConnection
import com.dxc.ssi.agent.model.DidConfig
import com.dxc.ssi.agent.wallet.indy.IndyWalletHolder
import com.dxc.ssi.agent.wallet.indy.IndyWalletManager
import com.dxc.utils.EnvironmentUtils
import com.dxc.utils.Sleeper
import org.junit.Ignore
import org.junit.Test

class SsiAgentApiImplTest {

    private val walletName = "newWalletName2"
    private val walletPassword = "newWalletPassword"
    private val did = "Aj4mwDVVEh46K17Cqh4dpU"


    @Test
    @Ignore("Ignored because it is actually integration tests which should be moved out of unit tests in order to to run during build")
    //TODO: Move integration tests to separate module
    fun basicTest() {
        println("Starting test")

        EnvironmentUtils.initEnvironment(EnvironmentImpl())

        val walletManager: WalletManager = IndyWalletManager

        if (!walletManager.isWalletExistsAndOpenable(walletName, walletPassword))
            walletManager.createWallet(walletName, walletPassword)

        if (!walletManager.isDidExistsInWallet(did, walletName, walletPassword)) {
            val didResult = walletManager.createDid(walletName = walletName, walletPassword = walletPassword)
            print("Got generated didResult: did = ${didResult.did} , verkey = ${didResult.verkey}")
            //Store did somewhere in your application to use it afterwards
        }

        val walletHolder = IndyWalletHolder(
            walletName = walletName,
            walletPassword = walletPassword,
            didConfig = DidConfig(did = did)
        )

        val indyWalletConnector = IndyWalletConnector.build(walletHolder)

        val ssiAgentApi = SsiAgentBuilderImpl(indyWalletConnector)
            .withConnectionInitiatorController(ConnectionInitiatorControllerImpl())
            .withCredReceiverController(CredReceiverControllerImpl())
            .withCredPresenterController(CredPresenterControllerImpl())
            .withLedgerConnector(IndyLedgerConnector(IndyLedgerConnectorConfiguration(genesisFilePath = "/home/ifedyanin/source/github/fedyiv/ssi-mobile-sdk-lumedic/files/docker_pool_transactions_genesis.txt")))
            .build()

        ssiAgentApi.init()


        val invitationUrl =
            "ws://192.168.0.117:9000/ws?c_i=eyJsYWJlbCI6IkNsb3VkIEFnZW50IiwiaW1hZ2VVcmwiOm51bGwsInNlcnZpY2VFbmRwb2ludCI6IndzOi8vMTkyLjE2OC4wLjExNzo5MDAwL3dzIiwicm91dGluZ0tleXMiOlsiRVk0ZFZSUjZVb0Q5WWN5SkQ5VURZUmd6QnI3SDZFeEhGUTdxWEJkaHVKUXQiXSwicmVjaXBpZW50S2V5cyI6WyI3WFJiZ1dmRlNaQThNZXpKMkJrRnI4eUszeTJrQUtvaVNWQVNuNGQxaEhSYSJdLCJAaWQiOiI1ZDBkNmM0OS0wYjQ0LTRiZDQtYjhhOS1jMDk3NTFlNjE5ZDEiLCJAdHlwZSI6ImRpZDpzb3Y6QnpDYnNOWWhNcmpIaXFaRFRVQVNIZztzcGVjL2Nvbm5lY3Rpb25zLzEuMC9pbnZpdGF0aW9uIn0="


        println("Connecting to issuer")
        ssiAgentApi.connect(invitationUrl)

        Sleeper().sleep(500000)

    }

    class CredPresenterControllerImpl : CredPresenterController {
        override fun onRequestReceived(
            connection: PeerConnection,
            presentationRequest: PresentationRequestContainer
        ): CallbackResult {
            return CallbackResult(true)
        }

        override fun onDone(connection: PeerConnection): CallbackResult {
            return CallbackResult(true)
        }

        override fun onProblemReportGenerated(connection: PeerConnection, problemReport: ProblemReport) {

        }

    }

    class CredReceiverControllerImpl : CredReceiverController {
        override fun onOfferReceived(
            connection: PeerConnection,
            credentialOfferContainer: CredentialOfferContainer
        ): CallbackResult {
            return CallbackResult(true)
        }

        override fun onRequestSent(
            connection: PeerConnection,
            credentialRequestContainer: CredentialRequestContainer
        ): CallbackResult {
            return CallbackResult(true)
        }

        override fun onCredentialReceived(
            connection: PeerConnection,
            credentialContainer: CredentialContainer
        ): CallbackResult {
            return CallbackResult(true)
        }

        override fun onDone(connection: PeerConnection, credentialContainer: CredentialContainer): CallbackResult {
            return CallbackResult(true)
        }


    }

    class ConnectionInitiatorControllerImpl : ConnectionInitiatorController {
        override fun onInvitationReceived(
            connection: PeerConnection,
            invitation: Invitation
        ): CallbackResult {
            return CallbackResult(canProceedFurther = true)
        }

        override fun onRequestSent(connection: PeerConnection, request: ConnectionRequest): CallbackResult {
            println("Request sent hook called : $connection, $request")
            return CallbackResult(true)
        }

        override fun onResponseReceived(connection: PeerConnection, response: ConnectionResponse): CallbackResult {
            println("Response received hook called : $connection, $response")
            return CallbackResult(true)
        }

        override fun onCompleted(connection: PeerConnection): CallbackResult {
            println("Connection completed : $connection")
            return CallbackResult(true)
        }

        override fun onAbandoned(connection: PeerConnection, problemReport: ProblemReport): CallbackResult {
            println("Connection abandoned : $connection")
            return CallbackResult(true)
        }

    }
}