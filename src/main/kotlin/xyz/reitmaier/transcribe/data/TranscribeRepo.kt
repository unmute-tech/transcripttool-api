package xyz.reitmaier.transcribe.data

import com.github.michaelbull.logging.InlineLogger
import xyz.reitmaier.transcribe.db.TranscribeDb
import com.github.michaelbull.result.*
import xyz.reitmaier.transcribe.auth.PasswordEncryptor
import xyz.reitmaier.transcribe.db.User

class TranscribeRepo(db: TranscribeDb, private val passwordEncryptor: PasswordEncryptor) {
  private val log = InlineLogger()
  private val queries = db.userQueries

  fun insertUser(email: Email, password: Password) : DomainResult<User> = runCatching {
    val encryptedPassword = passwordEncryptor.encrypt(password)
    queries.transactionWithResult<UserId> {
      queries.addUser(email, encryptedPassword)
      UserId(lastId())
    }
  }.mapError { DuplicateUser }
    .andThen {
      queries.findUserById(it).executeAsOneOrNull().toResultOr { DatabaseError }
    }

  fun findUserByEmail(email: Email) : DomainResult<User> = runCatching {
    queries.findUserByEmail(email).executeAsOne()
  }.mapError { UserNotFound }

  fun findUserByEmailAndPassword(user: UserAccount) : DomainResult<User> = runCatching {
    queries.findUserByEmailAndPassword(user.email, passwordEncryptor.encrypt(user.password)).executeAsOne()
  }.mapError { EmailOrPasswordIncorrect }

  private fun lastId() : Int = queries.lastInsertedIdAsLong().executeAsOne().toInt()
}