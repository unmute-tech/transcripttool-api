package io.reitmaier.transcribe.data

/**
 * All possible things that can happen in the use-cases
 */
sealed class DomainMessage(val message: String)

object DuplicateUser : DomainMessage("Duplicate user")
object DuplicateFile : DomainMessage("File already exists")
object UserNotFound : DomainMessage("User not found")
object FileNotFound : DomainMessage("File not found")
object TaskIdRequired : DomainMessage("Task Id is Required")
object TaskIdInvalid : DomainMessage("Task Id is Invalid")

object UserIdRequired : DomainMessage("User Id is Required")
object UserIdInvalid : DomainMessage("User Id is Invalid")

object DeploymentIdRequired : DomainMessage("Deployment Id is Required")
object DeploymentIdInvalid : DomainMessage("Deployment Id is Invalid")
object TaskNotFound : DomainMessage("Task not found")
object TranscriptNotFound : DomainMessage("Transcript not found")
object MobileOrPasswordIncorrect : DomainMessage("Mobile number or password is incorrect")
object PasswordIncorrect : DomainMessage("The password is incorrect")
object InvalidRequest : DomainMessage("The request is invalid")
object RequestFileMissing : DomainMessage("The request is missing a file item")
object FileNameMissing : DomainMessage("The request is missing a file name")
object ContentLengthMissing : DomainMessage("The request is missing a content length parameter: length")

/* internal errors */

object DatabaseError : DomainMessage("An Internal Error Occurred")
