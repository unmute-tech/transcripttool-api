package xyz.reitmaier.transcribe.data

import com.github.michaelbull.logging.InlineLogger
import xyz.reitmaier.transcribe.db.TranscribeDb
import com.github.michaelbull.result.*
import xyz.reitmaier.transcribe.auth.PasswordEncryptor
import xyz.reitmaier.transcribe.db.User_Entity

class TranscribeRepo(db: TranscribeDb, val passwordEncryptor: PasswordEncryptor) {
  private val log = InlineLogger()
  private val queries = db.userQueries

  fun insertUser(email: Email, password: Password) : DomainResult<User_Entity> = runCatching {
    val encryptedPassword = passwordEncryptor.encrypt(password)

    queries.addUser(email, encryptedPassword)
  }.mapError { DuplicateUser }
    .andThen {
      queries.findUserByEmail(email).executeAsOneOrNull().toResultOr { DatabaseError }
    }

  fun findUserByEmail(email: Email) : DomainResult<User_Entity> = runCatching {
    queries.findUserByEmail(email).executeAsOne()
  }.mapError { UserNotFound }

  fun authenticateUser(user: User) : DomainResult<User_Entity> =
    runCatching {
      queries.findUserByEmail(user.email).executeAsOne()
    }.mapError { UserNotFound }
      .andThen {
        if(it.password == passwordEncryptor.encrypt(user.password))
          Ok(it)
        else
          Err(PasswordIncorrect)
      }
}