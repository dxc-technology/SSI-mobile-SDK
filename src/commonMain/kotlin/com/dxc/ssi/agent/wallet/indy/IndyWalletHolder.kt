package com.dxc.ssi.agent.wallet.indy

import co.touchlab.stately.isolate.IsolateState
import co.touchlab.stately.isolate.StateHolder
import com.dxc.ssi.agent.api.pluggable.wallet.WalletHolder
import com.dxc.ssi.agent.exceptions.indy.WalletItemNotFoundException
import com.dxc.ssi.agent.model.Connection
import com.dxc.ssi.agent.model.DidConfig
import com.dxc.ssi.agent.model.IdentityDetails
import com.dxc.ssi.agent.model.messages.Message
import com.dxc.ssi.agent.utils.JsonUtils.extractValue
import com.dxc.ssi.agent.wallet.indy.helpers.WalletHelper
import com.dxc.ssi.agent.wallet.indy.libindy.*
import com.dxc.ssi.agent.wallet.indy.model.RetrievedWalletRecords
import com.dxc.ssi.agent.wallet.indy.model.WalletRecordTag
import com.dxc.ssi.agent.wallet.indy.model.WalletRecordType
import io.ktor.utils.io.core.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

//TODO: move this class to separate file
data class ObjectHolder<T>(var obj: T? = null)

open class IndyWalletHolder : WalletHolder {
    //TODO: think where do we need to store did
    //TODO: think how to avoid optionals here
    //understand what is the proper place for storing did
    private var isoDid = IsolateState {ObjectHolder<String?>()}
    private var isoVerkey = IsolateState {ObjectHolder<String?>()}

    private var isoWallet = IsolateState {ObjectHolder<Wallet>()}



    override fun createSessionDid(identityRecord: IdentityDetails): String {
        TODO("Not yet implemented")
    }

    override fun getIdentityDetails(): IdentityDetails {
        return IdentityDetails(isoDid.access { it.obj }!!, isoVerkey.access { it.obj }!!, null, null)
    }

    override fun getIdentityDetails(did: String): IdentityDetails {
        TODO("Not yet implemented")
    }

    override fun getTailsPath(): String {
        TODO("Not yet implemented")
    }

    override suspend fun storeConnectionRecord(connection: Connection) {
        //TODO: check if we need to check wallet health status before using it
        val existingConnection = getConnectionRecordById(connection.id)
        val tagsJson = "{\"${WalletRecordTag.ConnectionVerKey.name}\": \"${connection.peerVerkey}\"}"

        if (existingConnection == null) {

            val value = connection.toJson()

            println("Storing connection $connection")
            val wallet = isoWallet.access { it.obj }
            WalletRecord.add(wallet!!, WalletRecordType.ConnectionRecord.name, connection.id, value, tagsJson)
        } else {
            val value = connection.toJson()
            println("Updating connection $connection")
            val wallet = isoWallet.access { it.obj }
            WalletRecord.updateValue(wallet!!, WalletRecordType.ConnectionRecord.name, connection.id, value)
            //TODO: check if there are cases when we really need to update tags
            WalletRecord.updateTags(wallet!!, WalletRecordType.ConnectionRecord.name, connection.id, tagsJson)
        }
    }

    override suspend fun getConnectionRecordById(connectionId: String): Connection? {

        //TODO: use some serializable data structure
        val options = "{\"retrieveType\" : true}"
        /*
        *  retrieveType: (optional, false by default) Retrieve record type,
        * retrieveValue: (optional, true by default) Retrieve record value,
        * retrieveTags: (optional, true by default) Retrieve record tags }
        *
        * */


        //TODO: find out better solution of looking up for connection
        return try {
            val wallet = isoWallet.access { it.obj }
            val retrievedValue =
                WalletRecord.get(wallet!!, WalletRecordType.ConnectionRecord.name, connectionId, options)
            Connection.fromJson(extractValue(retrievedValue))
        } catch (e: WalletItemNotFoundException) {
            null
        } catch (e: Exception) {
            //TODO: understand what ExecutionException in java implementation corresponds to in kotlin code
            //TODO: check how to compare exact exception class rather than message contains string
            if (e.message!!.contains("WalletItemNotFoundException"))
                null
            else
                throw e
        }
    }

    override suspend fun findConnectionByVerKey(verKey: String): Connection? {


        val query = "{\"${WalletRecordTag.ConnectionVerKey.name}\": \"${verKey}\"}"
        val options = "{\"retrieveType\" : true, \"retrieveTotalCount\" : true}"

        println("Searching connections using query: $query")

        val search = WalletSearch()

        val wallet = isoWallet.access { it.obj }
        search.open(wallet!!, WalletRecordType.ConnectionRecord.name, query, options)
        //TODO: make proper fetch in batches insetad of just fetching 100 records
        val foundRecordsJson = search.searchFetchNextRecords(wallet!!, 100)
        search.closeSearch()

        println("Fetched connections json = $foundRecordsJson")

        val retrievedWalletRecords = Json {}.decodeFromString<RetrievedWalletRecords>(foundRecordsJson)

        if (retrievedWalletRecords.totalCount == null || retrievedWalletRecords.totalCount == 0)
            return null

        //TODO: consider case when we received several records. Is it ok? What do do in this case? Need to make some robust solution instead of taking first one
        return retrievedWalletRecords.records!!
            .map {
                println(it.value)
                it.value
            }.map<String, Connection> { Json.decodeFromString(it) }
            .firstOrNull()

    }

    override suspend fun openOrCreateWallet() {

        //TODO: think where to store name and password and how to pass it properly
        val walletName = "testWalletName"
        val walletPassword = "testWalletPassword"

        //TODO: remove this line in order to not clear wallet each time
        WalletHelper.createOrTrunc(walletName = walletName, walletPassword = walletPassword)
        val wallet = WalletHelper.openOrCreate(walletName = walletName, walletPassword = walletPassword)
        isoWallet.access { it.obj = wallet }

        //TODO: do not recreate did each time on wallet opening
        //TODO: alow to provide specific DID config

        val didConfigJson = Json.encodeToString(DidConfig())
        val didResult = Did.createAndStoreMyDid(wallet!!, didConfigJson)
        isoDid.access { it.obj = didResult.getDid() }
        isoVerkey.access { it.obj = didResult.getVerkey() }

    }

    override fun getWallet(): Any {
        //TODO: see if it is possible to return common Wallet instead of Any
        val wallet = isoWallet.access { it.obj }
        return wallet as Any
    }

    //TODO: remove all unnecessary code and beautify this function
    override suspend fun packMessage(message: Message, recipientKeys: List<String>, useAnonCrypt: Boolean): String {
        val byteArrayMessage = message.payload.toByteArray()
        val recipientVk = recipientKeys.joinToString(separator = "\",\"", prefix = "[\"", postfix = "\"]")
        //val recipientVk = recipientKeys.joinToString(separator = ",",prefix = "", postfix = "")
        println("recipientKeys = $recipientVk")

        val senderVk = if (useAnonCrypt) null else isoVerkey.access { it.obj }
        val wallet = isoWallet.access { it.obj }
        val byteArrayPackedMessage = Crypto.packMessage(wallet!!, recipientVk, senderVk, byteArrayMessage)

        val decodedString = String(byteArrayPackedMessage)

        println("Decoded packed message = $decodedString")

        return decodedString
    }

    //TODO: remove all unnecessary code and beautify this function
    override suspend fun unPackMessage(packedMessage: Message): Message {

        val byteArrayMessage = packedMessage.payload.toByteArray()

        val wallet = isoWallet.access { it.obj }
        val byteArrayUnpackedMessage = Crypto.unpackMessage(wallet!!, byteArrayMessage)

        val decodedString = String(byteArrayUnpackedMessage)

        println("Decoded packed message = $decodedString")

        return Message(decodedString)

    }
}