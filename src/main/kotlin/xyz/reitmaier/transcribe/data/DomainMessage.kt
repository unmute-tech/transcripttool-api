package xyz.reitmaier.transcribe.data

/**
 * All possible things that can happen in the use-cases
 */
sealed class DomainMessage(val message: String)

object DuplicateUser : DomainMessage("Duplicate user")
object UserNotFound : DomainMessage("User not found")
object PasswordIncorrect : DomainMessage("The password is incorrect")
object InvalidRequest : DomainMessage("The request is invalid")

/* internal errors */

object DatabaseError : DomainMessage("An Internal Error Occurred")
