package xyz.reitmaier.transcribe

import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import xyz.reitmaier.transcribe.auth.PasswordEncryptor
import xyz.reitmaier.transcribe.data.TranscribeRepo
import xyz.reitmaier.transcribe.plugins.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
  install(CallLogging)
  val jwtConfig = JWTConfig.from(environment.config)
  val passwordEncryptor = PasswordEncryptor(jwtConfig)
  val db = configureDB()
  val repo = TranscribeRepo(db, passwordEncryptor)
  configureRouting(repo)
  configureHTTP()
  configureSerialization()
}
