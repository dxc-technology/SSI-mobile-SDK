package com.dxc.ssi.agent.api.pluggable.wallet

import com.dxc.ssi.agent.model.DidConfig
import com.dxc.ssi.agent.wallet.indy.libindy.CreateAndStoreMyDidResult

interface WalletManager {
    fun isWalletExistsAndOpenable(walletName: String, walletPassword: String): Boolean
    fun isDidExistsInWallet(did: String, walletName: String, walletPassword: String): Boolean
    fun createWallet(
        walletName: String,
        walletPassword: String,
        walletCreationStrategy: WalletCreationStrategy = WalletCreationStrategy.CreateOrFail
    )

    fun createDid(
        didConfig: DidConfig = DidConfig(),
        walletName: String,
        walletPassword: String
    ): CreateAndStoreMyDidResult
}