package com.dxc.ssi.agent.ledger.indy.libindy

import org.hyperledger.indy.sdk.ledger.Ledger

actual class Ledger {
    actual companion object {
        actual fun buildGetSchemaRequest(submitterDid: String, id: String): String {
            return Ledger.buildGetSchemaRequest(submitterDid, id).get()
        }

        actual fun buildGetCredDefRequest(submitterDid: String, id: String): String {
            return Ledger.buildGetCredDefRequest(submitterDid, id).get()
        }

        actual fun buildGetRevocRegDefRequest(submitterDid: String, id: String): String {
            return Ledger.buildGetRevocRegDefRequest(submitterDid, id).get()
        }

        actual fun buildGetRevocRegDeltaRequest(
            submitterDid: String,
            revocRegDefId: String,
            from: Long,
            to: Long
        ): String {
            return Ledger.buildGetRevocRegDeltaRequest(submitterDid, revocRegDefId, from, to).get()
        }

        actual fun submitRequest(pool: Pool, requestJson: String): String {
            return Ledger.submitRequest(pool, requestJson).get()
        }

        actual fun parseGetSchemaResponse(getSchemaResponse: String): ParseResponseResult {
            return ParseResponseResult( Ledger.parseGetSchemaResponse(getSchemaResponse).get().objectJson)
        }

        actual fun parseGetCredDefResponse(getCredDefResponse: String): ParseResponseResult {
            return ParseResponseResult(Ledger.parseGetCredDefResponse(getCredDefResponse).get().objectJson)
        }

        actual fun parseGetRevocRegDefResponse(getRevocRegDefResponse: String): ParseResponseResult {
            return ParseResponseResult(Ledger.parseGetRevocRegDefResponse(getRevocRegDefResponse).get().objectJson)
        }

        actual fun parseGetRevocRegDeltaResponse(getRevocRegDeltaResponse: String): ParseRegistryResponseResult {
            val result = Ledger.parseGetRevocRegDeltaResponse(getRevocRegDeltaResponse).get()
            return ParseRegistryResponseResult(timestamp = result.timestamp, objectJson = result.objectJson)
        }
    }
}