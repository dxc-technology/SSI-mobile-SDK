package com.dxc.ssi.agent.wallet.indy

import com.dxc.ssi.agent.didcomm.model.issue.data.CredentialDefinitionId
import com.dxc.ssi.agent.didcomm.model.verify.data.SchemaId
import com.dxc.ssi.agent.wallet.indy.model.issue.temp.RevocationRegistryDefinitionId


class IndyCredentialDefinitionAlreadyExistsException(schemaId: SchemaId, msg: String) :
    IllegalArgumentException("Credential definition for schema id $schemaId is already exists: $msg")

class IndyCredentialMaximumReachedException(revRegId: RevocationRegistryDefinitionId) :
    IllegalArgumentException("Revocation registry with id $revRegId cannot hold more credentials")

class IndySchemaAlreadyExistsException(name: String, version: String) :
    IllegalArgumentException("Schema with name $name and version $version already exists")

class IndySchemaNotFoundException(id: SchemaId, msg: String) :
    IllegalArgumentException("There is no schema with id $id: $msg")

class IndyRevRegNotFoundException(id: RevocationRegistryDefinitionId, msg: String) :
    IllegalArgumentException("There is no revocation registry with id $id: $msg")

class IndyRevDeltaNotFoundException(id: RevocationRegistryDefinitionId, msg: String) :
    IllegalArgumentException("Revocation registry delta $id for definition doesn't exist in ledger. $msg")

class IndyCredentialDefinitionNotFoundException(id: CredentialDefinitionId, msg: String) :
    IllegalArgumentException("There is no credential definition with id $id: $msg")