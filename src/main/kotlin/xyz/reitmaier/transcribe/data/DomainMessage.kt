package xyz.reitmaier.transcribe.data

/**
 * All possible things that can happen in the use-cases
 */
sealed class DomainMessage(val message: String)

object DuplicateUser : DomainMessage("Duplicate user")
object InvalidRequest : DomainMessage("The request is invalid")

/* internal errors */

object DatabaseError : DomainMessage("An Internal Error Occurred")
