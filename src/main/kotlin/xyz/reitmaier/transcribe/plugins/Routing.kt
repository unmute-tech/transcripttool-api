package xyz.reitmaier.transcribe.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.michaelbull.logging.InlineLogger
import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.binding.binding
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.MultiStatus
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.content.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import xyz.reitmaier.transcribe.auth.AuthResponse
import xyz.reitmaier.transcribe.auth.AuthService
import xyz.reitmaier.transcribe.auth.CLAIM_MOBILE
import xyz.reitmaier.transcribe.data.*
import xyz.reitmaier.transcribe.db.Task
import java.io.File
import java.util.UUID

data class JWTConfig(
  val audience: String,
  val realm: String,
  val tokenSecret: String,
  val refreshTokenSecret: String,
  val issuer: String,
) {
  companion object {
    fun from(config: ApplicationConfig) : JWTConfig {
      val jwtAudience = config.property("jwt.audience").getString()
      val jwtRealm = config.property("jwt.realm").getString()
      val jwtSecret = config.property("jwt.secret").getString()
      val jwtRefreshSecret = config.property("jwt.refreshTokenSecret").getString()
      val jwtIssuer = config.property("jwt.issuer").getString()
      return JWTConfig(jwtAudience, jwtRealm, jwtSecret, jwtRefreshSecret, jwtIssuer)
    }
  }
}


fun Application.configureRouting(repo: TranscribeRepo,
                                 jwtConfig: JWTConfig = JWTConfig.from(environment.config)
) {
  val log = InlineLogger()
  val authService = AuthService(repo, jwtConfig)
  authentication {
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
        if(repo.findUserByMobile(mobile).get() != null) {
          JWTPrincipal(credential.payload)
        } else {
          null
        }
      }
    }
  }
  routing {
    static("/") {
      staticRootFolder = File("static")
      file("privacy_policy.html")
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
        success = {call.respond(it)},
        failure = {call.respondDomainMessage(it)}
      )
    }
    post("/login") {
      val loginRequest = call.receive<LoginRequest>()
      log.info { "Login request $loginRequest"}
      repo.findUserByMobileAndPassword(loginRequest.mobile, loginRequest.password)
        .andThen { user -> authService.generateResponse(
          user = user,
          refreshToken = null
        ) }
        .fold(
          success = { authResponse ->  call.respond(authResponse) },
          failure = { call.respondDomainMessage(it)},
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
            success = { taskList -> call.respond(HttpStatusCode.OK, taskList)},
            failure = { call.respondDomainMessage(it) }
          )

        }
        post {
          val mobile = call.getMobileOfAuthenticatedUser()
          val user = repo.findUserByMobile(mobile).get() ?: return@post call.respondDomainMessage(UserNotFound)
          val multipartData = call.receiveMultipart().readAllParts()

          val fileItem = multipartData.filterIsInstance<PartData.FileItem>().firstOrNull() ?: return@post call.respondDomainMessage(RequestFileMissing)
          val displayName = fileItem.originalFileName ?: return@post call.respondDomainMessage(FileNameMissing).also { fileItem.dispose() }

          val formItems = multipartData.filterIsInstance<PartData.FormItem>()
          val length = formItems.firstOrNull { it.name == "length" }?.value?.toLongOrNull() ?: return@post call.respondDomainMessage(ContentLengthMissing).also { fileItem.dispose() }

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
//          // TODO Remove after testing
//          val file2 = file.copyTo(File("${file.path}-2"))
//          repo.insertTask(user.id,displayName,length,file2.path,TaskProvenance.REMOTE)
//          // TODO Remove until here
          repo.insertTask(user.id,displayName,length,file.path,TaskProvenance.LOCAL).map { task ->  task.id}
            .fold(
              success = { call.respond(HttpStatusCode.Created,it.value)},
              failure = { call.respondDomainMessage(it)}
            )
        }

        get("/{$TASK_ID_PARAMETER}") {
          val mobile = call.getMobileOfAuthenticatedUser()
          binding<TaskDto, DomainMessage> {
            val user = repo.findUserByMobile(mobile).bind()
            val task = repo.getUserTask(
              taskId = call.parameters.readTaskId().bind(),
              userId = user.id
            ).bind()

            // transcript may or may not exist yet
            val transcript = repo.getLatestTranscript(task.id).get()?.transcript ?: ""
            task.toDto(transcript)
          }.fold(
            success = { n -> call.respond(HttpStatusCode.Created, n)},
            failure = { call.respondDomainMessage(it) }
          )
        }
        post("/{$TASK_ID_PARAMETER}/transcript") {
          val mobile = call.getMobileOfAuthenticatedUser()
          binding<Int, DomainMessage> {
            val user = repo.findUserByMobile(mobile).bind()
            val task = repo.getUserTask(
              taskId = call.parameters.readTaskId().bind(),
              userId = user.id
            ).bind()
            val transcripts = call.receiveOrNull<List<NewTranscript>>()
              .toResultOr { InvalidRequest }.bind()
            repo.insertTranscripts(task.id, transcripts).bind()
          }.fold(
            success = { n -> call.respond(HttpStatusCode.Created, n)},
            failure = { call.respondDomainMessage(it) }
          )

        }

        get("/{$TASK_ID_PARAMETER}/file") {
          val mobile = call.getMobileOfAuthenticatedUser()
          binding<Pair<File, Task>, DomainMessage> {
            val user = repo.findUserByMobile(mobile).bind()
            val task = repo.getUserTask(
              taskId = call.parameters.readTaskId().bind(),
              userId = user.id
            ).bind()
            Pair(File(task.path), task)
          }.andThen { pair ->
              if(pair.first.exists()) {
                Ok(pair)
              } else {
                Err(FileNotFound)
              }
            }
            .fold(
              failure = { call.respondDomainMessage(it) },
              success = { pair ->
                call.response.header(
                  HttpHeaders.ContentDisposition,
                  ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, pair.second.display_name)
                    .toString()
                )
                call.respondFile(pair.first)
              },
          )
        }
      }
    }
  }
}
const val TASK_ID_PARAMETER = "taskId"
private fun Parameters.readTaskId(): Result<TaskId, DomainMessage> {
  return get(TASK_ID_PARAMETER)
    .toResultOr { TaskIdRequired }
    .andThen { it.toIntResult() }
    .mapError { TaskIdInvalid }
    .map { TaskId(it) }
}

fun String.toIntResult() : Result<Int, Throwable> =
  runCatching {
    this.toInt()
  }
fun ApplicationCall.getMobileOfAuthenticatedUser() : MobileNumber {
  val principal = principal<JWTPrincipal>()
  val email = principal!!.payload.getClaim(CLAIM_MOBILE).asString()
  return MobileNumber(email)
}
val log = InlineLogger()
suspend fun ApplicationCall.respondDomainMessage(domainMessage: DomainMessage) {
  log.debug { "Responding with Error: $domainMessage" }
  when(domainMessage) {
    DatabaseError ->  respond(HttpStatusCode.InternalServerError,domainMessage.message)
    DuplicateFile -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    DuplicateUser -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    InvalidRequest -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    UserNotFound -> respond(HttpStatusCode.Forbidden, domainMessage.message)
    PasswordIncorrect -> respond(HttpStatusCode.Forbidden, domainMessage.message)
    MobileOrPasswordIncorrect -> respond(HttpStatusCode.Unauthorized, domainMessage.message)
    RequestFileMissing -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    FileNameMissing -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    ContentLengthMissing -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    TaskIdInvalid -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    TaskIdRequired -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    TaskNotFound -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    TranscriptNotFound -> respond(HttpStatusCode.NotFound, domainMessage.message)
    FileNotFound -> respond(HttpStatusCode.NotFound, domainMessage.message)
  }
}
