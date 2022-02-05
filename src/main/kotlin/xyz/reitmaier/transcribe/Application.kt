package xyz.reitmaier.transcribe

import com.github.michaelbull.logging.InlineLogger
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.datetime.Instant
import xyz.reitmaier.transcribe.auth.PasswordEncryptor
import xyz.reitmaier.transcribe.data.TranscribeRepo
import xyz.reitmaier.transcribe.plugins.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
  val log = InlineLogger()
  val instant = Instant.parse("2022-02-05T15:28:20.429Z")
  log.debug { instant }
  val jwtConfig = JWTConfig.from(environment.config)
  val passwordEncryptor = PasswordEncryptor(jwtConfig)
  val db = configureDB()
  val repo = TranscribeRepo(db, passwordEncryptor)
  configureRouting(repo)
  configureHTTP()
  configureSerialization()
}
