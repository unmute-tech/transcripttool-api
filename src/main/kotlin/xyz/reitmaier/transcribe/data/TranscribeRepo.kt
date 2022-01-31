package xyz.reitmaier.transcribe.data

import xyz.reitmaier.transcribe.db.TranscribeDb
import com.github.michaelbull.result.*
import xyz.reitmaier.transcribe.db.Users

class TranscribeRepo(db: TranscribeDb) {
  private val queries = db.userQueries

  fun insertUser(email: Email, password: Password) : DomainResult<Users> = runCatching {
    queries.addUser(email, password)
  }.mapError { DuplicateUser }
    .andThen {
      queries.findUserByEmail(email).executeAsOneOrNull().toResultOr { DatabaseError }
    }
}