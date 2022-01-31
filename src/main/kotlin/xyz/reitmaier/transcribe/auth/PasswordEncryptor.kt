package xyz.reitmaier.transcribe.auth

import io.ktor.util.*
import xyz.reitmaier.transcribe.data.EncryptedPassword
import xyz.reitmaier.transcribe.data.Password
import xyz.reitmaier.transcribe.plugins.JWTConfig
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class PasswordEncryptor(jwtConfig: JWTConfig) {
    private val hmacKey: SecretKeySpec = SecretKeySpec(jwtConfig.secret.toByteArray(), ALGORITHM)

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