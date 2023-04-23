package io.reitmaier.transcribe

import com.github.michaelbull.logging.InlineLogger
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.reitmaier.transcribe.auth.PasswordEncryptor
import io.reitmaier.transcribe.data.TranscribeRepo
import io.reitmaier.transcribe.plugins.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
  val log = InlineLogger()
  val jwtConfig = JWTConfig.from(environment.config)
  val passwordEncryptor = PasswordEncryptor(jwtConfig)
  val db = configureDB()
  val repo = TranscribeRepo(db, passwordEncryptor)
  configureRouting(repo)
  configureHTTP()
  configureSerialization()
}
