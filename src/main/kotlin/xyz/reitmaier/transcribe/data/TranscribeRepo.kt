package xyz.reitmaier.transcribe.data

import com.github.michaelbull.logging.InlineLogger
import com.github.michaelbull.result.*
import kotlinx.datetime.Clock
import xyz.reitmaier.transcribe.auth.PasswordEncryptor
import xyz.reitmaier.transcribe.db.*

class TranscribeRepo(private val db: TranscribeDb, private val passwordEncryptor: PasswordEncryptor) {
  private val log = InlineLogger()
  private val users = db.userQueries
  private val tasks = db.taskQueries
  private val transcripts = db.transcriptQueries
  private val settings = db.settingsQueries

  fun updateRefreshToken(id: UserId, refreshToken: RefreshToken = RefreshToken.create()) : DomainResult<RefreshToken> =
    runCatching {
      users.updateRefreshToken(refreshToken, id)
      refreshToken
    }.mapError { DatabaseError }

  fun insertTranscripts(taskId: TaskId, newTranscripts: List<NewTranscript>) : DomainResult<Int> =
    runCatching {
      db.transactionWithResult<Int> {
        newTranscripts.forEach { transcript ->
          val timestamp = Clock.System.now()
          transcripts.addTranscript(
            task_id = taskId,
            region_start = transcript.regionStart,
            region_end = transcript.regionEnd,
            transcript = transcript.transcript,
            client_updated_at = transcript.updatedAt,
            created_at = timestamp,
            updated_at = timestamp,
          )
        }
        newTranscripts.size
      }
    }.mapError { DatabaseError }

//  private fun insertTranscript(taskId: TaskId, transcript: String, regionStart: Int, regionEnd: Int) : DomainResult<Transcript>  =
//    runCatching {
//      val timestamp = Clock.System.now()
//      transcripts.addTranscript(
//        task_id = taskId,
//        region_start = regionStart,
//        region_end = regionEnd,
//        transcript = transcript,
//        created_at = timestamp,
//        updated_at = timestamp
//      )
//      val transcriptId = TranscriptId(lastId())
//      transcripts.selectTranscript(transcriptId).executeAsOne()
//    }.mapError { DatabaseError }

  fun insertTask(userId: UserId, displayName: String, length: Long, path: String, provenance: TaskProvenance, ) : DomainResult<Task> =
    runCatching { db.transactionWithResult<Task> {
      val timestamp = Clock.System.now()
        tasks.addTask(
          user_id = userId,
          display_name = displayName,
          length = length,
          path = path,
          provenance = provenance,
          created_at = timestamp,
          updated_at = timestamp,
        )
        val taskId = TaskId(lastId())
        tasks.selectTaskById(taskId).executeAsOne()
      }
    }.mapError { DuplicateFile }

  fun insertUser(name: Name, mobile: MobileNumber, password: Password, operator: MobileOperator) : DomainResult<User> = runCatching {
    val encryptedPassword = passwordEncryptor.encrypt(password)
    users.transactionWithResult<UserId> {
      val timestamp = Clock.System.now()
      users.addUser(
        password = encryptedPassword,
        mobile_number = mobile,
        mobile_operator = operator,
        name = name,
        created_at = timestamp,
      )
      UserId(lastId())
    }
  }.mapError { DuplicateUser }
    .andThen {
      users.findUserById(it).executeAsOneOrNull().toResultOr { DatabaseError }
    }

  fun findUserByMobile(mobile: MobileNumber) : DomainResult<User> = runCatching {
    users.findUserByMobile(mobile).executeAsOne()
  }.mapError { UserNotFound }

  fun getUserTask(taskId: TaskId, userId: UserId) : DomainResult<Task> =
    tasks.selectTaskByIdAndUserId(userId, taskId).executeAsOneOrNull().toResultOr { TaskNotFound }

  fun getHydratedUserTasks(userId: UserId) : DomainResult<List<TaskDto>> =
    runCatching {
      tasks.hydratedUserTasks(userId).executeAsList().map { it.toDto() }
    }.mapError { DatabaseError }

  fun getLatestTranscript(taskId: TaskId) : DomainResult<Transcript> =
    transcripts.selectLatestTranscript(taskId).executeAsOneOrNull().toResultOr { TranscriptNotFound }

  fun findUserByRefreshToken(refreshToken: RefreshToken) : DomainResult<User> = runCatching {
    users.findUserByRefreshToken(refreshToken).executeAsOne()
  }.mapError { UserNotFound }

  fun findUserByMobileAndPassword(mobile: MobileNumber, password: Password) : DomainResult<User> = runCatching {
    this.users.findUserByMobileAndPassword(mobile, passwordEncryptor.encrypt(password)).executeAsOne()
  }.mapError { MobileOrPasswordIncorrect }

  private fun lastId() : Int = settings.lastInsertedIdAsLong().executeAsOne().toInt()
}

