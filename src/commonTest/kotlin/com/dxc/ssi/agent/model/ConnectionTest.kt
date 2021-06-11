package com.dxc.ssi.agent.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectionTest {

    @Test
    fun testSerialization() {

        val connection = Connection(
            id = "id",
            state = "state",
            invitation = "invitati",
            isSelfInitiated = true,
            peerRecipientKeys = listOf("keys"),
            endpoint = "ws:\\endpoint:111\\ws"
        )

        val jsonString = connection.toJson()

        println(jsonString)

        val connection2 = Connection.fromJson(jsonString)

        assertEquals(connection, connection2)

    }
}