package com.dxc.ssi.agent.wallet.indy

import co.touchlab.stately.isolate.IsolateState
import com.dxc.ssi.agent.api.pluggable.LedgerConnector
import com.dxc.ssi.agent.api.pluggable.wallet.Prover
import com.dxc.ssi.agent.api.pluggable.wallet.WalletHolder
import com.dxc.ssi.agent.config.Configuration
import com.dxc.ssi.agent.didcomm.model.common.RawData
import com.dxc.ssi.agent.didcomm.model.common.Thread
import com.dxc.ssi.agent.didcomm.model.issue.container.CredentialOfferContainer
import com.dxc.ssi.agent.didcomm.model.issue.data.*
import com.dxc.ssi.agent.didcomm.model.revokation.data.RevocationRegistryDefinition
import com.dxc.ssi.agent.didcomm.model.verify.container.PresentationRequestContainer
import com.dxc.ssi.agent.didcomm.model.verify.data.CredentialInfo
import com.dxc.ssi.agent.didcomm.model.verify.data.Presentation
import com.dxc.ssi.agent.didcomm.model.verify.data.PresentationRequest
import com.dxc.ssi.agent.didcomm.states.State
import com.dxc.ssi.agent.didcomm.states.issue.CredentialIssuenceState
import com.dxc.ssi.agent.didcomm.states.verify.CredentialVerificationState
import com.dxc.ssi.agent.exceptions.common.NoCredentialToSatisfyPresentationRequestException
import com.dxc.ssi.agent.exceptions.indy.DuplicateMasterSecretNameException
import com.dxc.ssi.agent.ledger.indy.helpers.TailsHelper
import com.dxc.ssi.agent.model.CredentialExchangeRecord
import com.dxc.ssi.agent.model.ExchangeRecord
import com.dxc.ssi.agent.model.PresentationExchangeRecord
import com.dxc.ssi.agent.utils.ObjectHolder
import com.dxc.ssi.agent.utils.indy.IndySerializationUtils
import com.dxc.ssi.agent.wallet.indy.helpers.WalletCustomRecordsRepository
import com.dxc.ssi.agent.wallet.indy.libindy.*
import com.dxc.ssi.agent.wallet.indy.model.WalletRecordType
import com.dxc.ssi.agent.wallet.indy.model.issue.*
import com.dxc.ssi.agent.wallet.indy.model.issue.temp.RevocationRegistryDefinitionId
import com.dxc.ssi.agent.wallet.indy.model.revoke.IndyRevocationRegistryDefinition
import com.dxc.ssi.agent.wallet.indy.model.revoke.RevocationState
import com.dxc.ssi.agent.wallet.indy.model.verify.*
import com.dxc.utils.Base64
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import com.dxc.ssi.agent.kermit.Kermit
import com.dxc.ssi.agent.kermit.LogcatLogger
import com.dxc.ssi.agent.kermit.Severity


class IndyProver(val walletHolder: WalletHolder) : Prover {
    var logger: Kermit = Kermit(LogcatLogger())
    private var masterSecretId: String?
        get() = isoMasterSecret.access { it.obj }!!
        set(value) {
            isoMasterSecret.access { it.obj = value }
        }

    private val isoMasterSecret = IsolateState { ObjectHolder<String>() }

    private val tailsPath = Configuration.tailsPath

    override suspend fun createCredentialRequest(
        proverDid: String,
        credentialDefinition: CredentialDefinition,
        credentialOffer: CredentialOffer,
        masterSecretId: String
    ): CredentialRequestInfo {
        val credentialOfferJson = IndySerializationUtils.jsonProcessor.encodeToString(
            PolymorphicSerializer(CredentialOffer::class),
            credentialOffer
        )

        val credDefJson = IndySerializationUtils.jsonProcessor.encodeToString(
            PolymorphicSerializer(CredentialDefinition::class),
            credentialDefinition
        )

        logger.d {
            "Before executing Anoncreds.proverCreateCredentialReq " +
                    "proverDid = $proverDid," +
                    "credentialOfferJson = $credentialOfferJson," +
                    "credDefJson = $credDefJson," +
                    "masterSecretId = $masterSecretId" }

        val credReq = Anoncreds.proverCreateCredentialReq(
            walletHolder.getWallet() as Wallet, proverDid, credentialOfferJson, credDefJson, masterSecretId
        )

        logger.d { "credReq.credentialRequestJson = ${credReq.getCredentialRequestJson()}" }
        logger.d { "credReq.credentialRequestMetadataJson = ${credReq.getCredentialRequestMetadataJson()}" }

        val credentialRequest =
            IndySerializationUtils.jsonProcessor.decodeFromString<IndyCredentialRequest>(credReq.getCredentialRequestJson())
        val credentialRequestMetadata =
            IndySerializationUtils.jsonProcessor.decodeFromString<IndyCredentialRequestMetadata>(credReq.getCredentialRequestMetadataJson())

        return CredentialRequestInfo(credentialRequest, credentialRequestMetadata)

    }

    override suspend fun createMasterSecret(id: String) {
        masterSecretId = id
        try {
            Anoncreds.proverCreateMasterSecret(walletHolder.getWallet() as Wallet, id)
        } catch (e: DuplicateMasterSecretNameException) {
            logger.d { "MasterSecret already exists, so we will use it" }
        }
    }

    override fun createCredentialDefinitionIdFromOffer(credentialOffer: CredentialOffer): CredentialDefinitionId {

        logger.d { "createCredentialDefinitionIdFromOffer: cred offer $credentialOffer" }

        val indyCredentialOffer = credentialOffer as IndyCredentialOffer


        logger.d {  "indy cred offer ${indyCredentialOffer}" }

        val credentialDefinitionIdRaw = indyCredentialOffer.credentialDefinitionIdRaw

        val strSplitted = credentialDefinitionIdRaw.split(":")
        val didCred = strSplitted[0]
        val tag = strSplitted[strSplitted.lastIndex]
        val seqNo = strSplitted[3].toInt()

        return IndyCredentialDefinitionId(didCred, seqNo, tag)

    }

    override suspend fun storeCredentialExchangeRecord(credentialExchangeRecord: CredentialExchangeRecord) {
        storeExchangeRecord(credentialExchangeRecord)
    }


    override suspend fun storePresentationExchangeRecord(presentationExchangeRecord: PresentationExchangeRecord) {
        storeExchangeRecord(presentationExchangeRecord)
    }

    private suspend inline fun <reified T : ExchangeRecord> storeExchangeRecord(exchangeRecord: T) {
        WalletCustomRecordsRepository.upsertWalletRecord(walletHolder.getWallet() as Wallet, exchangeRecord)
    }

    override suspend fun getCredentialExchangeRecordByThread(thread: Thread): CredentialExchangeRecord? {
        return getExchangeRecordByThreadId(thread)
    }

    override suspend fun getPresentationExchangeRecordByThread(thread: Thread): PresentationExchangeRecord? {
        return getExchangeRecordByThreadId(thread)
    }

    //TODO: consider removing this functions
    private suspend inline fun <reified T : ExchangeRecord> getExchangeRecordByThreadId(thread: Thread): T? {
        return WalletCustomRecordsRepository.getWalletRecordById(walletHolder.getWallet() as Wallet, thread.thid)
    }

    override suspend fun findCredentialExchangeRecordsWithState(credentialIssuenceState: CredentialIssuenceState): Set<CredentialExchangeRecord> {
        return findExchangeRecordsWithState(credentialIssuenceState)
    }

    override suspend fun findPresentationExchangeRecordsWithState(credentialVerificationState: CredentialVerificationState): Set<PresentationExchangeRecord> {
        return findExchangeRecordsWithState(credentialVerificationState)
    }

    private suspend inline fun <reified T : ExchangeRecord, S : State> findExchangeRecordsWithState(state: S): Set<T> {
        val query =
            "{\"${ExchangeRecord.getWalletRecordStateTag(T::class)}\": \"${state.name}\"}"
        return WalletCustomRecordsRepository.getWalletRecordsByQuery(walletHolder.getWallet() as Wallet, query)
    }

    override suspend fun getCredentialInfos(): Set<CredentialInfo> {

        val queryJson = "{}"

        val credentialsSearch = CredentialsSearch()

        credentialsSearch.open(walletHolder.getWallet() as Wallet, queryJson)

        //TODO: implement batches instead of hardcoded 200 creds
        val credentialsJson = credentialsSearch.fetchNextCredentials(200)

        logger.d { "Retrieved  credentialsJson: $credentialsJson" }

        val credentialInfos =
            IndySerializationUtils.jsonProcessor.decodeFromString<List<IndyCredInfo>>(credentialsJson)

        credentialsSearch.closeSearch()

        return credentialInfos.toSet()

    }

    override suspend fun getCredentialInfo(localWalletCredId: String): CredentialInfo {
        val credentialInfoJson = Anoncreds.proverGetCredential(walletHolder.getWallet() as Wallet, localWalletCredId)
        return IndySerializationUtils.jsonProcessor.decodeFromString<IndyCredInfo>(credentialInfoJson)
    }

    override fun extractCredentialRequestDataFromCredentialInfo(credentialRequestInfo: CredentialRequestInfo): RawData {

        //TODO: check if this type cast is needed here
        val credentialRequestJson =
            IndySerializationUtils.jsonProcessor.encodeToString(credentialRequestInfo.credentialRequest as IndyCredentialRequest)
        logger.d { "extractCredentialRequestDataFromCredentialInfo: credentialRequestJson = $credentialRequestJson" }


        return RawData(base64 = Base64.plainStringToBase64String(credentialRequestJson))

    }


    override suspend fun receiveCredential(
        credential: Credential,
        credentialRequestInfo: CredentialRequestInfo,
        credentialDefinition: CredentialDefinition,
        revocationRegistryDefinition: RevocationRegistryDefinition?
    ): String {

        val credentialJson =
            IndySerializationUtils.jsonProcessor.encodeToString(PolymorphicSerializer(Credential::class), credential)
        val credentialRequestMetadataJson =
            IndySerializationUtils.jsonProcessor.encodeToString(
                PolymorphicSerializer(CredentialRequestMetadata::class),
                credentialRequestInfo.credentialRequestMetadata
            )
        val credDefJson = IndySerializationUtils.jsonProcessor.encodeToString(
            PolymorphicSerializer(CredentialDefinition::class),
            credentialDefinition
        )
        val revRegDefJson =
            if (revocationRegistryDefinition == null)
                null
            else
                IndySerializationUtils.jsonProcessor.encodeToString(
                    PolymorphicSerializer(RevocationRegistryDefinition::class),
                    revocationRegistryDefinition
                )

        logger.d { "receiveCredential: credentialRequestMetadataJson -> $credentialRequestMetadataJson" }
        logger.d { "receiveCredential: credentialJson -> $credentialJson" }
        logger.d { "receiveCredential: credDefJson -> $credDefJson" }
        logger.d { "receiveCredential: revRegDefJson -> $revRegDefJson" }

        return Anoncreds.proverStoreCredential(
            walletHolder.getWallet() as Wallet,
            null,
            credentialRequestMetadataJson,
            credentialJson,
            credDefJson,
            revRegDefJson
        )

    }

    override suspend fun createRevocationState(
        revocationRegistryDefinition: RevocationRegistryDefinition,
        revocationRegistryEntry: RevocationRegistryEntry,
        credentialRevocationId: String,
        timestamp: Long
    ): RevocationState {

        val indyRevocationRegistryDefinition = revocationRegistryDefinition as IndyRevocationRegistryDefinition

        val tailsReaderHandle = TailsHelper.getTailsReaderHandler(tailsPath)

        val revRegDefJson = IndySerializationUtils.jsonProcessor.encodeToString(revocationRegistryDefinition)

        val revRegDeltaJson = IndySerializationUtils.jsonProcessor.encodeToString(revocationRegistryEntry)


        val revStateJson = Anoncreds.createRevocationState(
            tailsReaderHandle,
            revRegDefJson,
            revRegDeltaJson,
            timestamp,
            credentialRevocationId
        )

        val revocationState = IndySerializationUtils.jsonProcessor.decodeFromString<RevocationState>(revStateJson)
        revocationState.revocationRegistryIdRaw = indyRevocationRegistryDefinition.id

        return revocationState
    }

    override fun buildCredentialObjectFromRawData(data: RawData): Credential {

        val indyCredentialJson = Base64.base64StringToPlainString(data.base64)

        val indyCredential =
            IndySerializationUtils.jsonProcessor.decodeFromString<IndyCredential>(indyCredentialJson)

        return indyCredential

    }

    override fun buildCredentialOfferObjectFromRawData(data: RawData): CredentialOffer {

        val jsonCredentialOffer = Base64.base64StringToPlainString(data.base64)


        logger.d { "jsonCredentialOffer = $jsonCredentialOffer" }

        val indyCredentialOffer =
            IndySerializationUtils.jsonProcessor.decodeFromString<IndyCredentialOffer>(
                jsonCredentialOffer
            )

        return indyCredentialOffer

    }

    override suspend fun removeCredentialExchangeRecordByThread(thread: Thread) {

        WalletRecord.delete(
            walletHolder.getWallet() as Wallet,
            WalletRecordType.CredentialExchangeRecord.name,
            thread.thid
        )
    }

    override fun buildPresentationRequestObjectFromRawData(data: RawData): PresentationRequest {
        val indyPresentationRequestJson = Base64.base64StringToPlainString(data.base64)

        logger.d { "Received JSON PresentationRequest: $indyPresentationRequestJson" }

        val indyPresentationReuqest =
            IndySerializationUtils.jsonProcessor.decodeFromString<IndyPresentationRequest>(indyPresentationRequestJson)

        return indyPresentationReuqest
    }

    override suspend fun createPresentation(
        presentationRequest: PresentationRequest,
        ledgerConnector: LedgerConnector,
        /* TODO: add extra query parameter
        extraQuery: Map<String, Map<String, Any>>?*/
    ): Presentation {

        val indyPresentationRequest = presentationRequest as IndyPresentationRequest

        val proofRequestJson = IndySerializationUtils.jsonProcessor.encodeToString(presentationRequest)

        logger.d { "In createPresentation function: proofRequestJson = $proofRequestJson" }

        //TODO: deal with extra query. Understand what it is and how to use it. See cordentity
        val extraQueryJson = null

        val searchObj = CredentialsSearchForProofReq()
        searchObj.open(walletHolder.getWallet() as Wallet, proofRequestJson, extraQueryJson)


        val allSchemaIds = mutableListOf<IndySchemaId>()
        val allCredentialDefinitionIds = mutableListOf<IndyCredentialDefinitionId>()
        val allRevStates = mutableListOf<RevocationState>()

        //TODO: remove copypaste code from requestedAttributes and requestedPredicates
        val requestedAttributes = indyPresentationRequest.requestedAttributes.keys.associate { key ->

            val credentialJson = searchObj.fetchNextCredentials(key, 1)

            logger.d { "Retrieved for key = $key  -> credentialJson: $credentialJson" }

            val credentialForTheRequest =
                IndySerializationUtils.jsonProcessor.decodeFromString<List<IndyCredentialForTheRequest>>(credentialJson)
                    .firstOrNull()
                    ?: throw NoCredentialToSatisfyPresentationRequestException("Unable to find attribute $key that satisfies proof request: ${indyPresentationRequest.requestedAttributes[key]}")

            allSchemaIds.add(IndySchemaId.fromString(credentialForTheRequest.credInfo.schemaId))
            allCredentialDefinitionIds.add(IndyCredentialDefinitionId.fromString(credentialForTheRequest.credInfo.credDefId))


            val revStateAlreadyDone =
                allRevStates.find { it.revocationRegistryIdRaw == credentialForTheRequest.credInfo.revRegId }

            if (revStateAlreadyDone != null)
                return@associate key to RequestedAttributeInfo(
                    credentialForTheRequest.credInfo.referent,
                    timestamp = revStateAlreadyDone.timestamp
                )

            if ((credentialForTheRequest.credInfo.credRevId == null) xor (indyPresentationRequest.nonRevoked == null))
                throw RuntimeException("If credential is issued using some revocation registry, it should be proved to be non-revoked")

            // if everything is ok and rev state is needed - pull it from ledger
            val requestedAttributeInfo = if (
                credentialForTheRequest.credInfo.credRevId != null &&
                credentialForTheRequest.credInfo.revRegId != null &&
                indyPresentationRequest.nonRevoked != null
            ) {
                val revocationState = provideRevocationState(
                    RevocationRegistryDefinitionId.fromString(credentialForTheRequest.credInfo.revRegId),
                    credentialForTheRequest.credInfo.credRevId,
                    indyPresentationRequest.nonRevoked,
                    ledgerConnector
                )

                allRevStates.add(revocationState)

                RequestedAttributeInfo(
                    credentialForTheRequest.credInfo.referent,
                    timestamp = revocationState.timestamp
                )
            } else { // else just give up
                RequestedAttributeInfo(credentialForTheRequest.credInfo.referent)
            }

            //key to requestedAttributeInfo
            key to requestedAttributeInfo
        }

        val requestedPredicates = indyPresentationRequest.requestedPredicates.keys.associate { key ->
            val credentialJson = searchObj.fetchNextCredentials(key, 1)

            logger.d { "Retrieved for key = $key  -> credentialJson: $credentialJson" }

            val credentialForTheRequest =
                IndySerializationUtils.jsonProcessor.decodeFromString<List<IndyCredentialForTheRequest>>(credentialJson)
                    .firstOrNull()
                    ?: throw RuntimeException("Unable to find attribute $key that satisfies proof request: ${indyPresentationRequest.requestedAttributes[key]}")

            allSchemaIds.add(IndySchemaId.fromString(credentialForTheRequest.credInfo.schemaId))
            allCredentialDefinitionIds.add(IndyCredentialDefinitionId.fromString(credentialForTheRequest.credInfo.credDefId))

            val revStateAlreadyDone =
                allRevStates.find { it.revocationRegistryIdRaw == credentialForTheRequest.credInfo.revRegId }

            if (revStateAlreadyDone != null)
                return@associate key to RequestedPredicateInfo(
                    credentialForTheRequest.credInfo.referent,
                    revStateAlreadyDone.timestamp
                )

            // if everything is ok and rev state is needed - pull it from ledger
            val requestedPredicateInfo = if (
                credentialForTheRequest.credInfo.credRevId != null &&
                credentialForTheRequest.credInfo.revRegId != null &&
                presentationRequest.nonRevoked != null
            ) {
                val revocationState = provideRevocationState(
                    RevocationRegistryDefinitionId.fromString(credentialForTheRequest.credInfo.revRegId),
                    credentialForTheRequest.credInfo.credRevId,
                    presentationRequest.nonRevoked,
                    ledgerConnector
                )

                allRevStates.add(revocationState)

                RequestedPredicateInfo(
                    credentialForTheRequest.credInfo.referent,
                    revocationState.timestamp
                )
            } else { // else just give up
                RequestedPredicateInfo(credentialForTheRequest.credInfo.referent, null)
            }

            key to requestedPredicateInfo
        }

        searchObj.closeSearch()

        val requestedCredentials = RequestedCredentials(requestedAttributes, requestedPredicates, mapOf())
        val requestedCredentialsJson = IndySerializationUtils.jsonProcessor.encodeToString(requestedCredentials)

        val allSchemas = allSchemaIds.distinct().map { ledgerConnector.retrieveSchema(it) as IndySchema }
        val allCredentialDefs = allCredentialDefinitionIds.distinct()
            .map { ledgerConnector.retrieveCredentialDefinition(it) as IndyCredentialDefinition }

        val usedSchemas = allSchemas.associate { it.id to it }
        val usedCredentialDefs = allCredentialDefs.associate { it.id to it }

        val usedRevocationStates = allRevStates
            .associate {
                val stateByTimestamp = hashMapOf<Long, RevocationState>()
                stateByTimestamp[it.timestamp] = it

                it.revocationRegistryIdRaw!! to stateByTimestamp
            }

        val usedSchemasJson = IndySerializationUtils.jsonProcessor.encodeToString(usedSchemas)
        val usedCredentialDefsJson = IndySerializationUtils.jsonProcessor.encodeToString(usedCredentialDefs)
        val usedRevStatesJson = IndySerializationUtils.jsonProcessor.encodeToString(usedRevocationStates)


        logger.d { "proofRequestJson -> $proofRequestJson" }
        logger.d { "requestedCredentialsJson -> $requestedCredentialsJson" }
        logger.d { "masterSecretId -> $masterSecretId" }
        logger.d { "usedSchemasJson -> $usedSchemasJson" }
        logger.d { "usedCredentialDefsJson -> $usedCredentialDefsJson" }
        logger.d { "usedRevStatesJson -> $usedRevStatesJson" }

        val proverProofJson = Anoncreds.proverCreateProof(
            walletHolder.getWallet() as Wallet,
            proofRequestJson,
            requestedCredentialsJson,
            masterSecretId,
            usedSchemasJson,
            usedCredentialDefsJson,
            usedRevStatesJson
        )

        logger.d { "Indy proof created: $proverProofJson" }

        val presentation = IndySerializationUtils.jsonProcessor.decodeFromString<IndyPresentation>(proverProofJson)

        return presentation
    }

    private suspend fun provideRevocationState(
        revRegId: RevocationRegistryDefinitionId,
        credRevId: String,
        interval: Interval,
        ledgerConnector: LedgerConnector
    ): RevocationState {
        val revocationRegistryDefinition = ledgerConnector.retrieveRevocationRegistryDefinition(revRegId)
            ?: throw IndyRevRegNotFoundException(revRegId, "Get revocation state has been failed")

        val response = ledgerConnector.retrieveRevocationRegistryDelta(revRegId, Interval(null, interval.to))
            ?: throw IndyRevDeltaNotFoundException(revRegId, "Interval is $interval")
        val (timestamp, revRegDelta) = response

        val revocationState = createRevocationState(revocationRegistryDefinition, revRegDelta, credRevId, timestamp)

        return revocationState

    }

    override fun extractPresentationDataFromPresentation(presentation: Presentation): RawData {

        //TODO: check if this type cast is needed here
        val presentationJson =
            IndySerializationUtils.jsonProcessor.encodeToString(presentation as IndyPresentation)
        logger.d { "extractPresentationDataFromPresentation: presentationJson = $presentationJson" }


        return RawData(base64 = Base64.plainStringToBase64String(presentationJson))

    }

    override suspend fun getParkedCredentialOffers(): Set<CredentialOfferContainer> {
        return findCredentialExchangeRecordsWithState(CredentialIssuenceState.OFFER_RECEIVED)
            .filter { it.isParked }
            .mapNotNull { it.credentialOfferContainer }
            .toSet()
    }

    override suspend fun getParkedPresentationRequests(): Set<PresentationRequestContainer> {
        return findPresentationExchangeRecordsWithState(CredentialVerificationState.REQUEST_RECEIVED)
            .filter { it.isParked }
            .mapNotNull { it.presentationRequestContainer }
            .toSet()
    }


}
