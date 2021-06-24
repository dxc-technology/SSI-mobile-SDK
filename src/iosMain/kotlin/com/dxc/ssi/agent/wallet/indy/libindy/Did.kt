package com.dxc.ssi.agent.wallet.indy.libindy

import com.dxc.ssi.agent.callback.CallbackData
import com.dxc.ssi.agent.callback.callbackHandler
import com.dxc.ssi.agent.callback.impl.StringCallback
import com.indylib.indy_create_and_store_my_did
import com.indylib.indy_get_my_did_with_meta
import kotlinx.cinterop.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


actual class Did {
    data class CreateAndStoreMyDidCallbackResult(
        override val commandHandle: Int,
        override val errorCode: UInt,
        val did: String,
        val verkey: String
    ) : CallbackData

    actual companion object {
        actual suspend fun createAndStoreMyDid(
            wallet: Wallet,
            didJson: String
        ): CreateAndStoreMyDidResult {
            memScoped {

                val commandHandle = callbackHandler.prepareCallback()
                val walletHandle = wallet.getWalletHandle()

                val callback =
                    staticCFunction() { commandHandle: Int, errorCode: UInt, didData: CPointer<ByteVar>?, verkeyData: CPointer<ByteVar>?
                        ->
                        initRuntimeIfNeeded()
                        callbackHandler.setCallbackResult(
                            CreateAndStoreMyDidCallbackResult(commandHandle, errorCode,
                                didData!!.toKString(),
                                verkeyData!!.toKString()
                            ))
                    }

                indy_create_and_store_my_did(
                    commandHandle,
                    walletHandle,
                    didJson,
                    callback
                )
                //TODO: if indy function returned non-zero result, probably we need to catch that and cancel corresponding job in callbackHandler
                val callbackResult = callbackHandler.waitForCallbackResult(commandHandle) as CreateAndStoreMyDidCallbackResult

                return CreateAndStoreMyDidResult(callbackResult.did, callbackResult.verkey)
            }
        }

        actual suspend fun getDidWithMeta(
            wallet: Wallet,
            did: String
        ): DidWithMetadataResult {

            val commandHandle = callbackHandler.prepareCallback()

            indy_get_my_did_with_meta(
                commandHandle,
                wallet.getWalletHandle(),
                did,
                StringCallback.callback
            )

            val callbackResultJson = callbackHandler.waitForCallbackResult(commandHandle) as StringCallback.Result

            return Json.decodeFromString(callbackResultJson.stringResult)

        }
    }
}