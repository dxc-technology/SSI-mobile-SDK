package com.dxc.ssi.agent.utils

import io.ktor.http.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UrlSerializer : KSerializer<Url> {
    override fun deserialize(decoder: Decoder): Url = Url(decoder.decodeString())

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("url", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Url) = encoder.encodeString(value.toString())


}