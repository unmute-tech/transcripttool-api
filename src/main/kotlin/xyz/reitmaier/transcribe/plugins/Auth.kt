package xyz.reitmaier.transcribe.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.michaelbull.result.fold
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import xyz.reitmaier.transcribe.data.Email
import xyz.reitmaier.transcribe.data.TranscribeRepo

fun Application.configureAuth(repo: TranscribeRepo) {
}
