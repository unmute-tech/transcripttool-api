package xyz.reitmaier.transcribe.data

import kotlinx.datetime.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// UserId

object UserIdSerializer : KSerializer<UserId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.UserId", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: UserId) {
    encoder.encodeInt(value.value)
  }

  override fun deserialize(decoder: Decoder): UserId {
    return UserId(decoder.decodeInt())
  }
}

object DeploymentIdSerializer : KSerializer<DeploymentId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.DeploymentId", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: DeploymentId) {
    encoder.encodeInt(value.value)
  }

  override fun deserialize(decoder: Decoder): DeploymentId {
    return DeploymentId(decoder.decodeInt())
  }
}
object TranscriptIdSerializer : KSerializer<TranscriptId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.TranscriptId", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: TranscriptId) {
    encoder.encodeInt(value.value)
  }

  override fun deserialize(decoder: Decoder): TranscriptId {
    return TranscriptId(decoder.decodeInt())
  }
}
object AssignmentStrategySerializer : KSerializer<AssignmentStrategy> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.AssignmentStrategy", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: AssignmentStrategy) {
    encoder.encodeInt(value.value)
  }

  override fun deserialize(decoder: Decoder): AssignmentStrategy {
    return AssignmentStrategy(decoder.decodeInt())
  }
}
object AssignmentIdSerializer : KSerializer<AssignmentId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.AssignmentId", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: AssignmentId) {
    encoder.encodeInt(value.value)
  }

  override fun deserialize(decoder: Decoder): AssignmentId {
    return AssignmentId(decoder.decodeInt())
  }
}
object RequestIdSerializer : KSerializer<RequestId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.RequestId", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: RequestId) {
    encoder.encodeInt(value.value)
  }

  override fun deserialize(decoder: Decoder): RequestId {
    return RequestId(decoder.decodeInt())
  }
}

object TaskIdSerializer : KSerializer<TaskId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.TaskId", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: TaskId) {
    encoder.encodeInt(value.value)
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

object InstantEpochSerializer : KSerializer<Instant> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

  override fun serialize(encoder: Encoder, value: Instant) =
    encoder.encodeLong(value.toEpochMilliseconds())

  override fun deserialize(decoder: Decoder): Instant =
    Instant.fromEpochMilliseconds(decoder.decodeLong())
}
