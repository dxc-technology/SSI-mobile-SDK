package com.dxc.ssi.agent.model

import com.dxc.ssi.agent.didcomm.model.issue.container.Attribute
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import kotlin.test.Test
import kotlin.test.assertEquals

class AttributeTest {

    @Test
    fun testSerializationOfSimpleValue() {

        val attributeJson = "{\n" +
                "        \"name\": \"vaccination\",\n" +
                "        \"mime-type\": \"application/json\",\n" +
                "        \"value\": \"yes\"\n" +
                "      }"


        val attribute = Json.decodeFromString<Attribute>(attributeJson)

        val serializedAttributeJson = Json.encodeToString(attribute)
        println("Serialized attribute: $serializedAttributeJson")
        assertEquals(attributeJson.replace("\n","").replace(" ",""), serializedAttributeJson)

    }


    @Test
    fun testSerializationOfJsonLikeString() {

        val attributeJson = " {\n" +
                "            \"name\": \"\$Metadata\",\n" +
                "            \"mime-type\": \"application/json\",\n" +
                "            \"value\": \"{\\\"HolderID\\\":1}\"\n" +
                "        }"


        println("Initial attributeJson: $attributeJson")

        val attribute = Json.decodeFromString<Attribute>(attributeJson)


        println("decoded attribute: $attribute")

        val serializedAttributeJson = Json.encodeToString(attribute)

        println("Serialized attributeJson: $attributeJson")

        assertEquals(attributeJson.replace("\n","").replace(" ",""), serializedAttributeJson)


    }


}