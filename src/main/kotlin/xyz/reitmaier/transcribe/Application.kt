package xyz.reitmaier.transcribe

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import kotlinx.serialization.json.Json
import xyz.reitmaier.transcribe.data.TranscribeRepo
import xyz.reitmaier.transcribe.plugins.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
  val db = configureDB()
  val repo = TranscribeRepo(db)
  configureRouting(repo)
  configureSecurity()
  configureHTTP()
  configureSerialization()
}
