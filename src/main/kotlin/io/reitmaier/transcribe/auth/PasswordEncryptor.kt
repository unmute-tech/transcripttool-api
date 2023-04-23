package io.reitmaier.transcribe.auth

import io.ktor.util.hex
import io.reitmaier.transcribe.data.EncryptedPassword
import io.reitmaier.transcribe.data.Password
import io.reitmaier.transcribe.plugins.JWTConfig
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class PasswordEncryptor(jwtConfig: JWTConfig) {
  private val hmacKey: SecretKeySpec = SecretKeySpec(jwtConfig.tokenSecret.toByteArray(), ALGORITHM)

  /**
   * Function which encrypts password and return
   */
  fun encrypt(password: Password): EncryptedPassword {
    val hmac = Mac.getInstance(ALGORITHM).apply { init(hmacKey) }
    return EncryptedPassword(hex(hmac.doFinal(password.value.toByteArray(Charsets.UTF_8))))
  }

  companion object {
    const val ALGORITHM = "HmacSHA256"
  }
}
