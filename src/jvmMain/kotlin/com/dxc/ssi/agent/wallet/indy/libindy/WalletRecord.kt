package com.dxc.ssi.agent.wallet.indy.libindy

import org.hyperledger.indy.sdk.non_secrets.WalletRecord

actual class WalletRecord {
    actual companion object {
        actual suspend fun get(wallet: Wallet, type: String, id: String, optionsJson: String): String {
            return WalletRecord.get(wallet.wallet, type, id, optionsJson).get()
        }

        actual suspend fun add(wallet: Wallet, type: String, id: String, value: String, tagsJson: String?) {
            WalletRecord.add(wallet.wallet, type, id, value, tagsJson).get()
        }

        actual suspend fun updateValue(wallet: Wallet, type: String, id: String, value: String) {
            WalletRecord.updateValue(wallet.wallet, type, id, value).get()
        }

        actual suspend fun updateTags(wallet: Wallet, type: String, id: String, tagsJson: String) {
            WalletRecord.updateTags(wallet.wallet, type, id, tagsJson).get()
        }

        actual suspend fun delete(wallet: Wallet, type: String, id: String) {
            WalletRecord.delete(wallet.wallet, type, id).get()
        }
    }

}