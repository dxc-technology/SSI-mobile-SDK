package com.dxc.ssi.agent.wallet.indy.libindy

import com.dxc.ssi.agent.exceptions.indy.IndyJvmToCommonExceptionConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.hyperledger.indy.sdk.did.Did

actual class Did {
    actual companion object {
        actual suspend fun createAndStoreMyDid(
            wallet: Wallet,
            didJson: String
        ): CreateAndStoreMyDidResult {
            return Did.createAndStoreMyDid(wallet.wallet, didJson).get()
        }

        actual suspend fun getDidWithMeta(
            wallet: Wallet,
            did: String
        ): DidWithMetadataResult {
            val converter = IndyJvmToCommonExceptionConverter<DidWithMetadataResult>()
            return converter.convertException {
                val result = Did.getDidWithMeta(wallet.wallet, did).get()
                Json.decodeFromString(result)
            }
        }

    }

}