package xyz.reitmaier.transcribe.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import kotlinx.datetime.Clock
import xyz.reitmaier.transcribe.auth.PasswordEncryptor
import xyz.reitmaier.transcribe.db.Deployment
import xyz.reitmaier.transcribe.db.Hydrated_task
import xyz.reitmaier.transcribe.db.Request
import xyz.reitmaier.transcribe.db.TranscribeDb
import xyz.reitmaier.transcribe.db.Transcript
import xyz.reitmaier.transcribe.db.User
import java.io.File

class TranscribeRepo(private val db: TranscribeDb, private val passwordEncryptor: PasswordEncryptor) {
  private val users = db.userQueries
  private val tasks = db.taskQueries
  private val deployments = db.deploymentQueries
  private val transcripts = db.transcriptQueries
  private val requests = db.requestQueries
  private val settings = db.settingsQueries

  fun updateRefreshToken(id: UserId, refreshToken: RefreshToken = RefreshToken.create()) : DomainResult<RefreshToken> =
    runCatching {
      users.updateRefreshToken(refreshToken, id)
      refreshToken
    }.mapError { DatabaseError }

  private fun transcriptExists(newTranscript: NewTranscript, existingTranscripts: List<Transcript>) : Boolean {
    return existingTranscripts.any { it.region_start == newTranscript.regionStart && it.region_end == newTranscript.regionEnd && it.transcript == newTranscript.transcript }
  }

  fun insertTranscripts(taskId: TaskId, newTranscripts: List<NewTranscript>) : DomainResult<Int> =
    runCatching {
      db.transactionWithResult {
        val existingTranscripts = transcripts.transcriptsByTaskId(taskId).executeAsList()
        val count =
          newTranscripts
          .filter { !transcriptExists(it, existingTranscripts) } // ignore existing Transcripts
          .map {
            val timestamp = Clock.System.now()
            transcripts.addTranscript(
              task_id = taskId,
              region_start = it.regionStart,
              region_end = it.regionEnd,
              transcript = it.transcript,
              client_updated_at = it.updatedAt,
              created_at = timestamp,
              updated_at = timestamp,
            )
          }
          .count()
        if(count > 0) {
          // Set Task updated at time
          db.taskQueries.updateTaskTimestamp(Clock.System.now(), taskId)
        }
        count
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

  fun insertTask(userId: UserId, displayName: String, length: Long, path: String, provenance: TaskProvenance) : DomainResult<Hydrated_task> =
    runCatching { db.transactionWithResult {
      val timestamp = Clock.System.now()
      requests.addRequest(userId,path,displayName.substringAfterLast(".", "audio"),length, AssignmentStrategy.OWNER,timestamp,timestamp)
      val requestId = RequestId(lastId())
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
      requests.assignRequestToTask(requestId,taskId,timestamp)
      tasks.selectTaskById(taskId).executeAsOne()
    }
    }.mapError { DatabaseError }

  fun insertRequest(
    userId: UserId,
    displayName: String,
    length: Long,
    path: String,
    extension: String,
    assignmentStrategy: AssignmentStrategy,
  ) : DomainResult<Request> =
    runCatching {
      val users = users.allUsers().executeAsList()
      db.transactionWithResult {
        val timestamp = Clock.System.now()
        requests.addRequest(
          user_id = userId,
          length = length,
          path = path,
          assignment_strategy = assignmentStrategy,
          extension = extension,
          created_at = timestamp,
          updated_at = timestamp,
        )
        val requestId = RequestId(lastId())
        val request = requests.getRequest(requestId).executeAsOne()
        // TODO Handle other Assignment Strategies
        if(assignmentStrategy == AssignmentStrategy.ALL) {
          // TODO generate sensible displayName
          // For every user other than the requester
          users.filter { it.id != userId }.forEach {  user ->
            // TODO Length
            // TODO display Name
            tasks.addTask(
              user_id = user.id,
              path = path,
              length = length,
              provenance = TaskProvenance.REMOTE,
              display_name = displayName,
              updated_at = timestamp,
              created_at = timestamp
            )
            val taskId = TaskId(lastId())
            requests.assignRequestToTask(
              request_id = requestId,
              task_id = taskId,
              assigned_at = timestamp,
            )
          }
        }
        request
      }
    }.mapError { DatabaseError }

  fun insertUser(name: Name, mobile: MobileNumber, password: Password, operator: MobileOperator) : DomainResult<User> = runCatching {
    val encryptedPassword = passwordEncryptor.encrypt(password)
    users.transactionWithResult {
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

  fun getUserTaskDto(taskId: TaskId, userId: UserId) : DomainResult<TaskDto> =
    getUserTask(taskId,userId).map { it.toDto() }

  fun getTaskFileInfo(taskId: TaskId, userId: UserId) : DomainResult<TaskFileInfo> =
    getUserTask(taskId, userId)
      .andThen { task ->
        val file = File(task.path)
        if (file.exists()) {
          return Ok(TaskFileInfo(file, task.display_name))
        } else {
          Err(FileNotFound)
        }
      }

  fun getTaskFilePath(taskId: TaskId) : DomainResult<String> =
    runCatching {
      tasks.selectTaskById(taskId).executeAsOne().path
      ""
    }.mapError { DatabaseError }

  private fun getUserTask(taskId: TaskId, userId: UserId) : DomainResult<Hydrated_task> =
    tasks.selectTaskByIdAndUserId(userId, taskId).executeAsOneOrNull().toResultOr { TaskNotFound }

  fun getDeployments() : DomainResult<List<Deployment>> =
    runCatching {
      deployments.allDeployments().executeAsList()
    }.mapError { DatabaseError }

  fun getDeploymentTasks(id: DeploymentId) : DomainResult<List<Hydrated_task>> =
    runCatching {
      deployments.getDeploymentTasks(id).executeAsList()
    }.mapError { DatabaseError }
  fun getDeployment(id: DeploymentId) : DomainResult<Deployment> =
    runCatching {
      deployments.getDeployment(id).executeAsOne()
    }.mapError { DatabaseError }
  fun getDeploymentUsers(id: DeploymentId) : DomainResult<List<User>> =
    runCatching {
      deployments.getDeploymentUsers(id).executeAsList()
    }.mapError { DatabaseError }

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
  fun rejectTask(taskId: TaskId, rejectReason: RejectReason): DomainResult<Unit> =
    runCatching {
      val timestamp = Clock.System.now()
      tasks.rejectTask(rejectReason, timestamp, timestamp, taskId)
    }.mapError { DatabaseError }

  fun completeTask(taskId: TaskId, completeTaskRequest: CompleteTaskRequest): DomainResult<Unit> =
    runCatching {
      val timestamp = Clock.System.now()
      tasks.completeTask(completeTaskRequest.difficulty, timestamp, timestamp, taskId)
    }.mapError { DatabaseError }
}

