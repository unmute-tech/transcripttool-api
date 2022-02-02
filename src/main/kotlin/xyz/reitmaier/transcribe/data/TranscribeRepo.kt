package xyz.reitmaier.transcribe.data

import com.github.michaelbull.logging.InlineLogger
import xyz.reitmaier.transcribe.db.TranscribeDb
import com.github.michaelbull.result.*
import xyz.reitmaier.transcribe.auth.PasswordEncryptor
import xyz.reitmaier.transcribe.db.Task
import xyz.reitmaier.transcribe.db.User

class TranscribeRepo(private val db: TranscribeDb, private val passwordEncryptor: PasswordEncryptor) {
  private val log = InlineLogger()
  private val user = db.userQueries
  private val task = db.taskQueries
  private val settings = db.settingsQueries

  fun insertTask(userId: UserId, displayName: String, length: Long, path: String, provenance: TaskProvenance, ) : DomainResult<Task> =
    runCatching { db.transactionWithResult<Task> {
        task.addTask(
          user_id = userId,
          display_name = displayName,
          length = length,
          path = path,
          provenance = provenance,
        )
        val taskId = TaskId(lastId())
        task.selectTaskById(taskId).executeAsOne()
      }
    }.mapError { DuplicateFile }

  fun insertUser(name: Name, mobile: MobileNumber, password: Password, operator: MobileOperator) : DomainResult<User> = runCatching {
    val encryptedPassword = passwordEncryptor.encrypt(password)
    user.transactionWithResult<UserId> {
      user.addUser(
        password = encryptedPassword,
        mobile_number = mobile,
        mobile_operator = operator,
        name = name)
      UserId(lastId())
    }
  }.mapError { DuplicateUser }
    .andThen {
      user.findUserById(it).executeAsOneOrNull().toResultOr { DatabaseError }
    }

  fun findUserByMobile(mobile: MobileNumber) : DomainResult<User> = runCatching {
    user.findUserByMobile(mobile).executeAsOne()
  }.mapError { UserNotFound }

  fun findUserByMobileAndPassword(mobile: MobileNumber, password: Password) : DomainResult<User> = runCatching {
    this.user.findUserByMobileAndPassword(mobile, passwordEncryptor.encrypt(password)).executeAsOne()
  }.mapError { MobileOrPasswordIncorrect }

  private fun lastId() : Int = settings.lastInsertedIdAsLong().executeAsOne().toInt()
}