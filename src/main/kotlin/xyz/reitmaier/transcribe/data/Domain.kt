package xyz.reitmaier.transcribe.data

import com.github.michaelbull.result.*
import kotlinx.serialization.Serializable

/*
REQUESTS
 */

@Serializable
data class NewUserRequest(
  val email: Email,
  val password: Password,
)

/*
INLINES
 */

@Serializable(with = UserIdSerializer::class)
@JvmInline
value class UserId(val value: Long)

@Serializable(with = PasswordSerializer::class)
@JvmInline
value class Password(val value: String)

@Serializable(with = EmailSerializer::class)
@JvmInline
value class Email(val value: String)


typealias DomainResult<T> = Result<T, DomainMessage>

