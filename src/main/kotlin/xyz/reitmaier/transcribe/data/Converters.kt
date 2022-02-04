package xyz.reitmaier.transcribe.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.joda.time.DateTimeZone
import org.joda.time.Instant
import org.joda.time.LocalDateTime
import xyz.reitmaier.transcribe.plugins.timestampAdapter
import java.text.SimpleDateFormat

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

object TranscriptIdSerializer : KSerializer<TranscriptId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.TranscriptId", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: TranscriptId) {
    encoder.encodeString(value.value.toString())
  }

  override fun deserialize(decoder: Decoder): TranscriptId {
    return TranscriptId(decoder.decodeInt())
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

object MobileNumberSerializer : KSerializer<MobileNumber> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.MobileNumber", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: MobileNumber) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): MobileNumber {
    return MobileNumber(decoder.decodeString())
  }
}

object MobileOperatorSerializer : KSerializer<MobileOperator> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.MobileOperator", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: MobileOperator) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): MobileOperator {
    return MobileOperator(decoder.decodeString())
  }
}

object AccessTokenSerializer : KSerializer<AccessToken> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.AccessToken", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: AccessToken) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): AccessToken {
    return AccessToken(decoder.decodeString())
  }
}
object RefreshTokenSerializer : KSerializer<RefreshToken> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.RefreshToken", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: RefreshToken) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): RefreshToken {
    return RefreshToken(decoder.decodeString())
  }
}
object NameSerializer : KSerializer<Name> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.Name", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Name) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): Name {
    return Name(decoder.decodeString())
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

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: LocalDateTime) =
    encoder.encodeString(value.toDateTime().toInstant().toString())

  override fun deserialize(decoder: Decoder): LocalDateTime =
    Instant.parse(decoder.decodeString()).toDateTime(DateTimeZone.getDefault()).toLocalDateTime()
}
