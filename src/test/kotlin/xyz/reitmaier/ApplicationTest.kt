package xyz.reitmaier

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import io.netty.handler.codec.http.HttpHeaders.addHeader
import kotlinx.coroutines.delay
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert
import xyz.reitmaier.transcribe.auth.AuthResponse
import xyz.reitmaier.transcribe.data.*
import java.io.File
import kotlin.time.Duration.Companion.seconds

class ApplicationTest {
  private val file = File("beep.opus")
  private val fileContent = file.readBytes()
  @Test
  fun testRoot() = testApplication {
    val response = client.get("/")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Hello World!", response.bodyAsText())
  }

  @Test
  fun testRegister() = testApplication {
    val client = createClient {
      expectSuccess = false
    }
      val response = client.post("/register") {
        contentType(ContentType.Application.Json)
        setBody(Json.encodeToString(RegistrationRequest.TEST))
      }
    assertEquals(DuplicateUser.message, response.bodyAsText())
    assertEquals(HttpStatusCode.BadRequest, response.status)
  }

  @Test
  fun `login test`() = testApplication {
    assertNotNull(client.login())
  }

  @Test
  fun `invalid login test`() = testApplication {
    val client = createClient {
      expectSuccess = false
    }
    val response = client.post("/login") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(LoginRequest.TEST.copy(password = Password("wrong password"))))
    }
    assertEquals(HttpStatusCode.Unauthorized, response.status)
  }

  @Test
  fun `auth declined test`() = testApplication {
    val client = createClient {
      expectSuccess = false
    }
    val response = client.get("/tasks") {
    }
    assertEquals(HttpStatusCode.Unauthorized, response.status)
  }

  @Test
  fun `auth test`() = testApplication {
    val token = client.login()
    val response = client.get("/tasks") {
      bearerAuth(token.accessToken.value)
    }
    assertEquals(HttpStatusCode.OK, response.status)
  }

  @Test
  fun `refresh test`() = testApplication {
    val auth = client.login()
    delay(2.seconds)
    val response = client.post("/refresh") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(auth.refreshToken))
    }
    assertEquals(HttpStatusCode.OK, response.status)
    val authResponse = Json.decodeFromString<AuthResponse>(response.bodyAsText())
    assertNotNull(authResponse)
    assertNotEquals(auth.accessToken, authResponse.accessToken)
    assertEquals(auth.refreshToken, authResponse.refreshToken)
  }


  // This test times out because of bug in auth
//  @Test
//  fun `test create new task`() = testApplication {
//    val boundary = "WebAppBoundary"
//    val token = client.login()
//    delay(1.seconds)
//
//    val client = createClient {
//      expectSuccess = false
//    }
//    val response = client.post("/task") {
//      bearerAuth(token.accessToken.value)
//      setBody(
//        MultiPartFormDataContent(
//          formData {
//            append("length", 5)
//            append("file", fileContent, Headers.build {
//              append(HttpHeaders.ContentDisposition, "filename=${file.name}")
//            })
//          },
//          boundary,
//          ContentType.MultiPart.FormData.withParameter("boundary", boundary)
//        )
//      )
//    }
//    assertEquals(HttpStatusCode.Created, response.status)
//  }

}


private suspend fun HttpClient.login() : AuthResponse {
  val response = post("/login") {
    contentType(ContentType.Application.Json)
    setBody(Json.encodeToString(LoginRequest.TEST))
  }
  assertEquals(HttpStatusCode.OK, response.status)
  val authResponse = Json.decodeFromString<AuthResponse>(response.bodyAsText())
  assertNotNull(authResponse)

  return authResponse
}