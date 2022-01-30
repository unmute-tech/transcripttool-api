package xyz.reitmaier.transcribe

import io.ktor.server.application.*
import io.ktor.server.netty.*
import xyz.reitmaier.transcribe.plugins.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
  configureRouting()
  configureSecurity()
  configureHTTP()
  configureSerialization()
}
