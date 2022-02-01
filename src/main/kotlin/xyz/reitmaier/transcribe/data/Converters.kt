package xyz.reitmaier.transcribe.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

// UserId

object UserIdSerializer : KSerializer<UserId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.UserId", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: UserId) {
    encoder.encodeString(value.value.toString())
  }

  override fun deserialize(decoder: Decoder): UserId {
    return UserId(decoder.decodeInt())
  }
}

object TaskIdSerializer : KSerializer<TaskId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.TaskId", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: TaskId) {
    encoder.encodeString(value.value.toString())
  }

  override fun deserialize(decoder: Decoder): TaskId {
    return TaskId(decoder.decodeInt())
  }
}

object EmailSerializer : KSerializer<Email> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.Email", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Email) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): Email {
    return Email(decoder.decodeString())
  }
}

object PasswordSerializer : KSerializer<Password> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.Password", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Password) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): Password {
    return Password(decoder.decodeString())
  }
}
