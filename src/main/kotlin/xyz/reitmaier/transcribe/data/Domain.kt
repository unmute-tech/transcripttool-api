package xyz.reitmaier.transcribe.data

import com.github.michaelbull.result.*
import kotlinx.serialization.Serializable
import org.joda.time.LocalDateTime
import xyz.reitmaier.transcribe.db.Task

/*
REQUESTS
 */

@Serializable
data class CreateUserRequest(
  val email: Email,
  val password: Password,
)

@Serializable
data class CreateTaskRequest(
  val displayName: String,
  val lengthMs: Long,
)

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

  @Serializable(with = LocalDateTimeSerializer::class)
  val created_at: LocalDateTime,
  @Serializable(with = LocalDateTimeSerializer::class)
  val updated_at: LocalDateTime,
)
fun Task.toDto() : TaskDto =
  TaskDto(
    id = id,
    displayName = display_name,
    lengthMs = length,
    provenance = provenance,
    created_at = created_at,
    updated_at = updated_at
  )

@Serializable
data class UserAccount(val email: Email, val password: Password)
/*
INLINES
 */

@Serializable(with = TaskIdSerializer::class)
@JvmInline
value class TaskId(val value: Int)

@Serializable(with = UserIdSerializer::class)
@JvmInline
value class UserId(val value: Int)

@Serializable(with = PasswordSerializer::class)
@JvmInline
value class Password(val value: String)

@JvmInline
value class EncryptedPassword(val value: String)

@Serializable(with = EmailSerializer::class)
@JvmInline
value class Email(val value: String)

enum class TaskProvenance(val provenance: String) {
  LOCAL("LOCAL"),
  REMOTE("REMOTE")
}

typealias DomainResult<T> = Result<T, DomainMessage>

