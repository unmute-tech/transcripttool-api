package xyz.reitmaier.transcribe.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.map
import org.joda.time.DateTime
import xyz.reitmaier.transcribe.data.AccessToken
import xyz.reitmaier.transcribe.data.DomainResult
import xyz.reitmaier.transcribe.data.RefreshToken
import xyz.reitmaier.transcribe.data.TranscribeRepo
import xyz.reitmaier.transcribe.db.User
import xyz.reitmaier.transcribe.plugins.JWTConfig

// TODO place in (private) companion object
const val EXPIRES_IN_SECONDS = 120
const val CLAIM_MOBILE = "mobile"

@kotlinx.serialization.Serializable
data class AuthResponse(
  val accessToken: AccessToken,
  val refreshToken: RefreshToken,
  val expiresIn: Int = EXPIRES_IN_SECONDS,
)
class AuthService(
  private val repo: TranscribeRepo,
  private val jwtConfig: JWTConfig,
) {
  fun generateResponse(user: User, refreshToken: RefreshToken?) : DomainResult<AuthResponse> {
    val now = DateTime.now()
    val expiresAt = now.plusSeconds(EXPIRES_IN_SECONDS)

    val accessToken = AccessToken(
      JWT.create()
      .withAudience(jwtConfig.audience)
      .withIssuer(jwtConfig.issuer)
      .withClaim(CLAIM_MOBILE, user.mobile_number.value)
      .withExpiresAt(expiresAt.toDate())
      .withNotBefore(now.toDate())
      .withIssuedAt(now.toDate())
      .sign(Algorithm.HMAC256(jwtConfig.tokenSecret))
    )

    val tokenResult = if(refreshToken == null) {
      repo.updateRefreshToken(user.id, RefreshToken.create())
    } else {
      Ok(refreshToken)
    }

    return tokenResult.map { token ->
      AuthResponse(accessToken = accessToken, refreshToken = token)
    }
  }
}