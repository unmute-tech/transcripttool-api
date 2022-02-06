package xyz.reitmaier.transcribe.data

import com.github.michaelbull.result.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import xyz.reitmaier.transcribe.db.Hydrated_task
import xyz.reitmaier.transcribe.db.Task
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
{
  companion object {
    val TEST = NewTranscript("Test Transcript", 0, 5, Clock.System.now())
    val TEST1 = NewTranscript("Test Transcript continued", 5, 10, Clock.System.now())
    val TEST2 = NewTranscript("Test Transcript continued further", 10, 15, Clock.System.now())
    val TESTS = listOf(TEST, TEST1, TEST2)
  }
}


@Serializable
data class TaskRequest(
  val displayName: String,
  val lengthMs: Long,
) {
  companion object {
    val TEST = TaskRequest("DISPLAY NAME", 60*1000L)
  }
}

//public val id: TaskId,
//public val user_id: UserId,
//public val path: String,
//public val length: Long,
//public val provenance: TaskProvenance,
//public val display_name: String,
//public val created_at: LocalDateTime,
//public val updated_at: LocalDateTime
@Serializable
data class TaskDto(
  val id: TaskId,
  val displayName: String,
  val lengthMs: Long,
  val provenance: TaskProvenance,
  val transcript: String,

  @Serializable(with = InstantEpochSerializer::class)
  val created_at: Instant,
  @Serializable(with = InstantEpochSerializer::class)
  val updated_at: Instant,
)
fun Hydrated_task.toDto() =
  TaskDto(
    id = id,
    displayName = display_name,
    lengthMs = length,
    provenance = provenance,
    transcript = transcript ?: "",
    created_at = created_at,
    updated_at = updated_at
  )
fun Task.toDto(transcript: String) : TaskDto =
  TaskDto(
    id = id,
    displayName = display_name,
    lengthMs = length,
    provenance = provenance,
    transcript = transcript,
    created_at = created_at,
    updated_at = updated_at
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
value class TaskId(val value: Int) {
  companion object {
    val TEST = TaskId(1)
  }
}

@Serializable(with = UserIdSerializer::class)
@JvmInline
value class UserId(val value: Int) {
  companion object {
    val TEST = UserId(1)
  }
}

@Serializable(with = TranscriptIdSerializer::class)
@JvmInline
value class TranscriptId(val value: Int) {
  companion object {
    val TEST = TranscriptId(1)
  }
}

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
    val TEST = RefreshToken("REFRESH TOKEN")
  }
}

@Serializable(with = AccessTokenSerializer::class)
@JvmInline
value class AccessToken(val value: String) {
  companion object {
    val TEST = AccessToken("ACCESS TOKEN")
  }
}


@Serializable(with = MobileOperatorSerializer::class)
@JvmInline
value class MobileOperator(val value: String) {
  companion object {
    val TEST = MobileOperator("MOBILE OPERATOR")
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

typealias DomainResult<T> = Result<T, DomainMessage>

