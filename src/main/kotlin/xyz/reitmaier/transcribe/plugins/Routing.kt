package xyz.reitmaier.transcribe.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.michaelbull.logging.InlineLogger
import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.binding.binding
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import xyz.reitmaier.transcribe.data.*
private const val claimMobile = "mobile"
data class JWTConfig(
  val audience: String,
  val realm: String,
  val secret: String,
  val issuer: String,
) {
  companion object {
    fun from(config: ApplicationConfig) : JWTConfig {
      val jwtAudience = config.property("jwt.audience").getString()
      val jwtRealm = config.property("jwt.realm").getString()
      val jwtSecret = config.property("jwt.secret").getString()
      val jwtIssuer = config.property("jwt.issuer").getString()
      return JWTConfig(jwtAudience, jwtRealm, jwtSecret, jwtIssuer)
    }
  }
}


fun Application.configureRouting(repo: TranscribeRepo,
                                 jwtConfig: JWTConfig = JWTConfig.from(environment.config)
) {
  val log = InlineLogger()
  authentication {
    jwt("auth-jwt") {
      realm = jwtConfig.realm
      verifier(
        JWT
          .require(Algorithm.HMAC256(jwtConfig.secret))
          .withAudience(jwtConfig.audience)
          .withIssuer(jwtConfig.issuer)
          .build()
      )
      validate { credential ->
        val mobile = MobileNumber(credential.payload.getClaim(claimMobile).asString())
        if(repo.findUserByMobile(mobile).get() != null) {
            JWTPrincipal(credential.payload)
          } else {
            null
          }
      }
    }
  }
  routing {
    get("/") {
      call.respondText("Hello World!")
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
    post("/login") {
      val loginRequest = call.receive<LoginRequest>()
      log.info { "Login request $loginRequest"}
      repo.findUserByMobileAndPassword(loginRequest.mobile, loginRequest.password)
        .map {
          JWT.create()
            .withAudience(jwtConfig.audience)
            .withIssuer(jwtConfig.issuer)
            .withClaim(claimMobile, loginRequest.mobile.value)
            // TODO possibly expire tokens
//            .withExpiresAt(Date(System.currentTimeMillis() + 600000))
            .sign(Algorithm.HMAC256(jwtConfig.secret))
        }
        .fold(
          success = { call.respond(hashMapOf("token" to it)) },
          failure = { call.respondDomainMessage(it)},
        )
    }

    authenticate("auth-jwt") {
      get("/auth-test") {
        val email = call.getMobileOfAuthenticatedUser()
        call.respondText("Hello, $email!")
      }


      post("/task") {
        val mobile = call.getMobileOfAuthenticatedUser()
        binding <TaskDto, DomainMessage> {
          val user = repo.findUserByMobile(mobile).bind()
          val taskRequest = call.receiveOrNull<CreateTaskRequest>().toResultOr { InvalidRequest }.bind()
          repo.insertTask(user.id, displayName = taskRequest.displayName, length = taskRequest.lengthMs, path = taskRequest.displayName, provenance = TaskProvenance.REMOTE).bind().toDto()
        }.fold(
          success = { call.respond(HttpStatusCode.Created, it) },
          failure = { call.respondDomainMessage(it)}
        )
      }
    }
  }
}

fun ApplicationCall.getMobileOfAuthenticatedUser() : MobileNumber {
  val principal = principal<JWTPrincipal>()
  val email = principal!!.payload.getClaim(claimMobile).asString()
  val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
  return MobileNumber(email)
}

suspend fun ApplicationCall.respondDomainMessage(domainMessage: DomainMessage) {
  when(domainMessage) {
    DatabaseError ->  respond(HttpStatusCode.InternalServerError,domainMessage.message)
    DuplicateFile -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    DuplicateUser -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    InvalidRequest -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    UserNotFound -> respond(HttpStatusCode.NotFound, domainMessage.message)
    PasswordIncorrect -> respond(HttpStatusCode.Forbidden, domainMessage.message)
    MobileOrPasswordIncorrect -> respond(HttpStatusCode.Forbidden, domainMessage.message)
  }
}
