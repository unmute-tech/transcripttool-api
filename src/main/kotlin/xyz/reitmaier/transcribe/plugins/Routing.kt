package xyz.reitmaier.transcribe.plugins

import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.toResultOr
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import xyz.reitmaier.transcribe.data.*

fun Application.configureRouting(repo: TranscribeRepo) {

  routing {
    get("/") {
      call.respondText("Hello World!")
    }
    post("/register") {
      call.receiveOrNull<NewUserRequest>().toResultOr { InvalidRequest }
        .andThen { repo.insertUser(it.email, it.password) }
        .fold(
          success = {
            return@post call.respond(it.id)
          },
          failure = {
            return@post call.respondDomainMessage(it)
          }
        )
    }
  }
}

suspend fun ApplicationCall.respondDomainMessage(domainMessage: DomainMessage) {
  when(domainMessage) {
    DatabaseError ->  respond(HttpStatusCode.InternalServerError,domainMessage.message)
    DuplicateUser -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    InvalidRequest -> respond(HttpStatusCode.BadRequest, domainMessage.message)
  }
}
