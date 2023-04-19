package xyz.reitmaier.transcribe.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.michaelbull.logging.InlineLogger
import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.binding.binding
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import xyz.reitmaier.transcribe.auth.AuthResponse
import xyz.reitmaier.transcribe.auth.AuthService
import xyz.reitmaier.transcribe.auth.CLAIM_MOBILE
import xyz.reitmaier.transcribe.data.*
import xyz.reitmaier.transcribe.templates.Layout
import xyz.reitmaier.transcribe.templates.StatusViewTemplate
import java.io.File
import java.util.*

data class JWTConfig(
  val audience: String,
  val realm: String,
  val tokenSecret: String,
  val refreshTokenSecret: String,
  val issuer: String,
) {
  companion object {
    fun from(config: ApplicationConfig): JWTConfig {
      val jwtAudience = config.property("jwt.audience").getString()
      val jwtRealm = config.property("jwt.realm").getString()
      val jwtSecret = config.property("jwt.secret").getString()
      val jwtRefreshSecret = config.property("jwt.refreshTokenSecret").getString()
      val jwtIssuer = config.property("jwt.issuer").getString()
      return JWTConfig(jwtAudience, jwtRealm, jwtSecret, jwtRefreshSecret, jwtIssuer)
    }
  }
}


fun Application.configureRouting(
  repo: TranscribeRepo,
  jwtConfig: JWTConfig = JWTConfig.from(environment.config)
) {
  val log = InlineLogger()
  val authService = AuthService(repo, jwtConfig)
  authentication {
    basic("admin-basic-auth") {
      realm = "Admin"
      validate { credentials ->
        if (credentials.name == "admin" && credentials.password == "JQWm2oSn7KXm") {
          UserIdPrincipal(credentials.name)
        } else {
          null
        }
      }
    }
    jwt("auth-jwt") {
      realm = jwtConfig.realm
      verifier(
        JWT
          .require(Algorithm.HMAC256(jwtConfig.tokenSecret))
          .withAudience(jwtConfig.audience)
          .withIssuer(jwtConfig.issuer)
          .build()
      )
      validate { credential ->
        val mobile = MobileNumber(credential.payload.getClaim(CLAIM_MOBILE).asString())
        if (repo.findUserByMobile(mobile).get() != null) {
          JWTPrincipal(credential.payload)
        } else {
          null
        }
      }
    }
  }
  routing {
    post("/error") {
      val errorMessage = call.receiveOrNull<String>().toResultOr { InvalidRequest }
      errorMessage.fold(
        success = {
          log.debug { it }
          return@post call.respond(HttpStatusCode.OK, it)
        },
        failure = {
          log.error { "Failed to log error" }
          return@post call.respond(HttpStatusCode.BadRequest)
        }
      )
    }
    static {
//      defaultResource("index.html", "static")
      resource("privacy_policy.html", "static/privacy_policy.html")
    }
    authenticate("admin-basic-auth") {
      get("/admin") {
        call.respondText("Hello, ${call.principal<UserIdPrincipal>()?.name}!")
      }

      get("/user/{$USER_ID_PARAMETER}/task/{$TASK_ID_PARAMETER}/file") {
        binding<TaskFileInfo, DomainMessage> {
          val taskId = call.parameters.readTaskId().bind()
          val userId = call.parameters.readUserId().bind()
          repo.getTaskFileInfo(taskId, userId).bind()
        }
          .fold(
            failure = { call.respondDomainMessage(it) },
            success = { taskFileInfo ->
              call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(
                  ContentDisposition.Parameters.FileName,
                  taskFileInfo.displayName
                )
                  .toString()
              )
              call.respondFile(taskFileInfo.taskFile)
            },
          )
      }
      get("/status/deployment/{$DEPLOYMENT_ID_PARAMETER}") {
        val deploymentId = call.parameters.readDeploymentId()
          .fold(
            success = { it },
            failure = {return@get call.respondDomainMessage(it) }
          )
        val deployment = repo.getDeployment(deploymentId)
            .fold(
              success = { it },
              failure = {return@get call.respondDomainMessage(it) }
            )
        val users = repo.getDeploymentUsers(deployment.id)
          .fold(
            success = { it },
            failure = {return@get call.respondDomainMessage(it) }
          )
        val tasks = repo.getDeploymentTasks(deployment.id)
          .fold(
            success = { it },
            failure = {return@get call.respondDomainMessage(it) }
          )
        call.respondHtmlTemplate(Layout()) {
          content {
            insert(StatusViewTemplate(deployment, users, tasks)) {}
          }
        }
      }
    }
    post("/register") {
      call.receiveOrNull<RegistrationRequest>().toResultOr { InvalidRequest }
        .andThen { repo.insertUser(it.name, it.mobile, it.password, it.operator) }
        .fold(
          success = {
            return@post call.respond(HttpStatusCode.Created, it.id)
          },
          failure = {
            return@post call.respondDomainMessage(it)
          }
        )
    }
    post("/refresh") {
      binding<AuthResponse, DomainMessage> {
        val refreshToken = call.receiveOrNull<RefreshToken>().toResultOr { InvalidRequest }.bind()
        val user = repo.findUserByRefreshToken(refreshToken).bind()
        val response = authService.generateResponse(user, refreshToken).bind()
        response
      }.fold(
        success = { call.respond(it) },
        failure = { call.respondDomainMessage(it) }
      )
    }
    post("/login") {
      val loginRequest = call.receive<LoginRequest>()
      log.info { "Login request $loginRequest" }
      repo.findUserByMobileAndPassword(loginRequest.mobile, loginRequest.password)
        .andThen { user ->
          authService.generateResponse(
            user = user,
            refreshToken = null
          )
        }
        .fold(
          success = { authResponse -> call.respond(authResponse) },
          failure = { call.respondDomainMessage(it) },
        )
    }

    authenticate("auth-jwt") {
      post("/ping") {
        val mobile = call.getMobileOfAuthenticatedUser()
        call.respondText("Hello, $mobile!")
      }
      route("/tasks") {
        get {
          val mobile = call.getMobileOfAuthenticatedUser()
          binding<List<TaskDto>, DomainMessage> {
            val user = repo.findUserByMobile(mobile).bind()
            val tasks = repo.getHydratedUserTasks(user.id).bind()
            tasks
          }.fold(
            success = { taskList -> call.respond(HttpStatusCode.OK, taskList) },
            failure = { call.respondDomainMessage(it) }
          )

        }
        post {
          val mobile = call.getMobileOfAuthenticatedUser()
          val user = repo.findUserByMobile(mobile).get() ?: return@post call.respondDomainMessage(UserNotFound)
          val multipartData = call.receiveMultipart().readAllParts()

          val fileItem =
            multipartData.filterIsInstance<PartData.FileItem>().firstOrNull() ?: return@post call.respondDomainMessage(
              RequestFileMissing
            )
          val displayName = fileItem.originalFileName ?: return@post call.respondDomainMessage(FileNameMissing)
            .also { fileItem.dispose() }

          val extension = displayName.substringAfterLast(".", "oga")

          val formItems = multipartData.filterIsInstance<PartData.FormItem>()
          val length = formItems.firstOrNull { it.name == "length" }?.value?.toLongOrNull()
            ?: return@post call.respondDomainMessage(ContentLengthMissing).also { fileItem.dispose() }

          val fileName = "${UUID.randomUUID()}.task"

          val file = File("data/$fileName")
          log.debug { "Writing to file: ${file.absolutePath}" }

          // use InputStream from part to save file
          fileItem.streamProvider().use { its ->
            // copy the stream to the file with buffering
            file.outputStream().buffered().use {
              // note that this is blocking
              its.copyTo(it)
            }
          }
          fileItem.dispose()
          if (user.is_admin) {
            log.debug { "Distributing task to other users" }
            // Distribute Task to all other users
            repo.insertRequest(user.id, displayName, length, file.path, extension, AssignmentStrategy.ALL)
          }
          repo.insertTask(user.id, displayName, length, file.path, TaskProvenance.LOCAL).map { task -> task.id }
            .fold(
              success = { call.respond(HttpStatusCode.Created, it.value) },
              failure = { call.respondDomainMessage(it) }
            )

        }

        get("/{$TASK_ID_PARAMETER}") {
          val mobile = call.getMobileOfAuthenticatedUser()
          binding<TaskDto, DomainMessage> {
            val user = repo.findUserByMobile(mobile).bind()
            repo.getUserTaskDto(
              taskId = call.parameters.readTaskId().bind(),
              userId = user.id
            ).bind()
          }.fold(
            success = { task -> call.respond(HttpStatusCode.Created, task) },
            failure = { call.respondDomainMessage(it) }
          )
        }
        post("/{$TASK_ID_PARAMETER}/transcripts") {
          val mobile = call.getMobileOfAuthenticatedUser()
          binding<Int, DomainMessage> {
            val user = repo.findUserByMobile(mobile).bind()
            val task = repo.getUserTaskDto(
              taskId = call.parameters.readTaskId().bind(),
              userId = user.id
            ).bind()
            val transcripts = call.receiveOrNull<List<NewTranscript>>()
              .toResultOr { InvalidRequest }.bind()
            repo.insertTranscripts(task.id, transcripts).bind()
          }.fold(
            success = { n -> call.respond(HttpStatusCode.Created, n) },
            failure = { call.respondDomainMessage(it) }
          )
        }

        post("/{$TASK_ID_PARAMETER}/complete") {
          val mobile = call.getMobileOfAuthenticatedUser()
          binding<Unit, DomainMessage> {
            val user = repo.findUserByMobile(mobile).bind()
            val task = repo.getUserTaskDto(
              taskId = call.parameters.readTaskId().bind(),
              userId = user.id
            ).bind()
            val completeTaskRequest = call.receiveOrNull<CompleteTaskRequest>()
              .toResultOr { InvalidRequest }.bind()
            repo.completeTask(task.id, completeTaskRequest).bind()
          }.fold(
            success = { call.respond(HttpStatusCode.OK) },
            failure = { call.respondDomainMessage(it) }
          )
        }

        post("/{$TASK_ID_PARAMETER}/reject") {
          val mobile = call.getMobileOfAuthenticatedUser()
          binding<Unit, DomainMessage> {
            val user = repo.findUserByMobile(mobile).bind()
            val task = repo.getUserTaskDto(
              taskId = call.parameters.readTaskId().bind(),
              userId = user.id
            ).bind()
            val rejectReason = call.receiveOrNull<RejectReason>()
              .toResultOr { InvalidRequest }.bind()
            repo.rejectTask(task.id, rejectReason).bind()
          }.fold(
            success = { call.respond(HttpStatusCode.OK) },
            failure = { call.respondDomainMessage(it) }
          )

        }

        get("/{$TASK_ID_PARAMETER}/file") {
          val mobile = call.getMobileOfAuthenticatedUser()
          binding<TaskFileInfo, DomainMessage> {
            val user = repo.findUserByMobile(mobile).bind()
            val task = repo.getUserTaskDto(
              taskId = call.parameters.readTaskId().bind(),
              userId = user.id
            ).bind()
            repo.getTaskFileInfo(task.id, user.id).bind()
          }
            .fold(
              failure = { call.respondDomainMessage(it) },
              success = { taskFileInfo ->
                call.response.header(
                  HttpHeaders.ContentDisposition,
                  ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName,
                    taskFileInfo.displayName
                  )
                    .toString()
                )
                call.respondFile(taskFileInfo.taskFile)
              },
            )
        }
      }
    }
  }
}

const val TASK_ID_PARAMETER = "taskId"
const val USER_ID_PARAMETER = "userId"
const val DEPLOYMENT_ID_PARAMETER = "deploymentId"
private fun Parameters.readTaskId(): Result<TaskId, DomainMessage> {
  return get(TASK_ID_PARAMETER)
    .toResultOr { TaskIdRequired }
    .andThen { it.toIntResult() }
    .mapError { TaskIdInvalid }
    .map { TaskId(it) }
}

private fun Parameters.readUserId(): Result<UserId, DomainMessage> {
  return get(USER_ID_PARAMETER)
    .toResultOr { UserIdRequired }
    .andThen { it.toIntResult() }
    .mapError { UserIdInvalid }
    .map { UserId(it) }
}

private fun Parameters.readDeploymentId(): Result<DeploymentId, DomainMessage> {
  return get(DEPLOYMENT_ID_PARAMETER)
    .toResultOr { DeploymentIdRequired }
    .andThen { it.toIntResult() }
    .mapError { DeploymentIdInvalid }
    .map { DeploymentId(it) }
}

fun String.toIntResult(): Result<Int, Throwable> =
  runCatching {
    this.toInt()
  }

fun ApplicationCall.getMobileOfAuthenticatedUser(): MobileNumber {
  val principal = principal<JWTPrincipal>()
  val email = principal!!.payload.getClaim(CLAIM_MOBILE).asString()
  return MobileNumber(email)
}

val log = InlineLogger()
suspend fun ApplicationCall.respondDomainMessage(domainMessage: DomainMessage) {
  log.debug { "Responding with Error: $domainMessage" }
  when (domainMessage) {
    DatabaseError -> respond(HttpStatusCode.InternalServerError, domainMessage.message)
    DuplicateFile -> respond(HttpStatusCode.Conflict, domainMessage.message)
    DuplicateUser -> respond(HttpStatusCode.Conflict, domainMessage.message)
    InvalidRequest -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    UserNotFound -> respond(HttpStatusCode.Forbidden, domainMessage.message)
    PasswordIncorrect -> respond(HttpStatusCode.Forbidden, domainMessage.message)
    MobileOrPasswordIncorrect -> respond(HttpStatusCode.Unauthorized, domainMessage.message)
    RequestFileMissing -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    FileNameMissing -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    ContentLengthMissing -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    DeploymentIdInvalid -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    DeploymentIdRequired -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    TaskIdInvalid -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    TaskIdRequired -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    UserIdInvalid -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    UserIdRequired -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    TaskNotFound -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    TranscriptNotFound -> respond(HttpStatusCode.NotFound, domainMessage.message)
    FileNotFound -> respond(HttpStatusCode.NotFound, domainMessage.message)
  }
}
