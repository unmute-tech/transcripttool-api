package xyz.reitmaier.transcribe.data

import com.github.michaelbull.result.Result
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.reitmaier.transcribe.db.Hydrated_task
import java.io.File
import java.util.UUID

/*
REQUESTS
 */

@Serializable
data class RegistrationRequest(
  val mobile: MobileNumber,
  val operator: MobileOperator,
  val name: Name,
  val password: Password,
) {
  companion object {
    val TEST = RegistrationRequest(MobileNumber.TEST, MobileOperator.TEST, Name.TEST, Password.TEST)
  }
}

@Serializable
data class NewTranscript(
  val transcript: String,
  val regionStart: Int,
  val regionEnd: Int,
  @Serializable(with = InstantEpochSerializer::class)
  val updatedAt: Instant,
)

@Serializable
data class TaskDto(
  val id: TaskId,
  val displayName: String,
  val lengthMs: Long,
  val provenance: TaskProvenance,
  val transcript: String,
  val rejectReason: RejectReason?,

  @Serializable(with = InstantEpochSerializer::class)
  @SerialName("created_at")
  val createdAt: Instant,

  @Serializable(with = InstantEpochSerializer::class)
  @SerialName("updated_at")
  val updatedAt: Instant,

  @Serializable(with = InstantEpochSerializer::class)
  @SerialName("completed_at")
  val completedAt: Instant?,
)
fun Hydrated_task.toDto() =
  TaskDto(
    id = id,
    displayName = display_name,
    lengthMs = length,
    provenance = provenance,
    transcript = transcript ?: "",
    createdAt = created_at,
    updatedAt = updated_at,
    completedAt = completed_at,
    rejectReason = reject_reason,
  )

class TaskFileInfo(
  val taskFile: File,
  val displayName: String,
)

@Serializable
data class LoginRequest(val mobile: MobileNumber, val password: Password) {
  companion object {
    val TEST = LoginRequest(MobileNumber.TEST, Password.TEST)
  }
}
/*
INLINES
 */

@Serializable(with = TaskIdSerializer::class)
@JvmInline
value class TaskId(val value: Int)

@Serializable(with = RequestIdSerializer::class)
@JvmInline
value class RequestId(val value: Int)

@Serializable(with = AssignmentIdSerializer::class)
@JvmInline
value class AssignmentId(val value: Int)

@Serializable(with = AssignmentStrategySerializer::class)
@JvmInline
value class AssignmentStrategy(val value: Int) {
  companion object {
    val OWNER = AssignmentStrategy(-1)
    val ALL = AssignmentStrategy(0)
  }
}

@Serializable(with = UserIdSerializer::class)
@JvmInline
value class UserId(val value: Int)

@Serializable(with = TranscriptIdSerializer::class)
@JvmInline
value class TranscriptId(val value: Int)

@Serializable(with = DeploymentIdSerializer::class)
@JvmInline
value class DeploymentId(val value: Int)

@Serializable(with = PasswordSerializer::class)
@JvmInline
value class Password(val value: String) {
  companion object {
    val TEST = Password("PASSWORD")
  }
}

@Serializable(with = NameSerializer::class)
@JvmInline
value class Name(val value: String) {
  companion object {
    val TEST = Name("NAME")
  }
}

@Serializable(with = RefreshTokenSerializer::class)
@JvmInline
value class RefreshToken(val value: String) {
  companion object {
    fun create() : RefreshToken = RefreshToken(UUID.randomUUID().toString())
  }
}

@Serializable(with = AccessTokenSerializer::class)
@JvmInline
value class AccessToken(val value: String)


@Serializable(with = MobileOperatorSerializer::class)
@JvmInline
value class MobileOperator(val value: String) {
  companion object {
    val TEST = MobileOperator("NETWORK")
  }
}

@JvmInline
value class EncryptedPassword(val value: String)

@Serializable(with = MobileNumberSerializer::class)
@JvmInline
value class MobileNumber(val value: String) {
  companion object {
    val TEST = MobileNumber("000000000")
  }
}

enum class TaskProvenance(val provenance: String) {
  LOCAL("LOCAL"),
  REMOTE("REMOTE")
}

@Suppress("unused")
@Serializable
enum class RejectReason(val value: String) {
  BLANK("BLANK"),
  INAPPROPRIATE("INAPPROPRIATE"),
  UNDERAGE("UNDERAGE"),
}

@Serializable
data class CompleteTaskRequest(
  val difficulty: Difficulty?,

  @SerialName("completed_at")
  val completedAt: Instant
)

@Suppress("unused")
enum class Difficulty {
  EASY,
  MEDIUM,
  HARD,
}

@Suppress("unused")
enum class Confidence {
  LOW,
  MEDIUM,
  HIGH,
}

typealias DomainResult<T> = Result<T, DomainMessage>

