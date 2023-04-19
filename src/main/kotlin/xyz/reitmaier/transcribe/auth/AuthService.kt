package xyz.reitmaier.transcribe.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Serializable
import xyz.reitmaier.transcribe.data.AccessToken
import xyz.reitmaier.transcribe.data.DomainResult
import xyz.reitmaier.transcribe.data.InstantEpochSerializer
import xyz.reitmaier.transcribe.data.RefreshToken
import xyz.reitmaier.transcribe.data.TranscribeRepo
import xyz.reitmaier.transcribe.db.User
import xyz.reitmaier.transcribe.plugins.JWTConfig
import java.util.Date
import kotlin.time.Duration.Companion.seconds

// TODO place in (private) companion object
const val EXPIRES_IN_SECONDS = 86400
const val CLAIM_MOBILE = "mobile"

@Serializable
data class AuthResponse(
  val accessToken: AccessToken,
  val refreshToken: RefreshToken,
  @Serializable(with = InstantEpochSerializer::class)
  val expiresAt: Instant,
)
class AuthService(
  private val repo: TranscribeRepo,
  private val jwtConfig: JWTConfig,
) {
  fun generateResponse(user: User, refreshToken: RefreshToken?) : DomainResult<AuthResponse> {
    val now = Clock.System.now()
    val expiresAt = now + EXPIRES_IN_SECONDS.seconds

    val accessToken = AccessToken(
      JWT.create()
      .withAudience(jwtConfig.audience)
      .withIssuer(jwtConfig.issuer)
      .withClaim(CLAIM_MOBILE, user.mobile_number.value)
      .withExpiresAt(Date.from(expiresAt.toJavaInstant()))
      .withNotBefore(Date.from(now.toJavaInstant()))
      .withIssuedAt(Date.from(now.toJavaInstant()))
      .sign(Algorithm.HMAC256(jwtConfig.tokenSecret))
    )

    val tokenResult = if(refreshToken == null) {
      repo.updateRefreshToken(user.id, RefreshToken.create())
    } else {
      Ok(refreshToken)
    }

    return tokenResult.map { token ->
      AuthResponse(accessToken = accessToken, refreshToken = token, expiresAt = expiresAt)
    }
  }
}