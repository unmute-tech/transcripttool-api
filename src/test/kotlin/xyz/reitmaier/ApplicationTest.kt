package xyz.reitmaier

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.*
import kotlin.test.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.reitmaier.transcribe.data.DuplicateUser
import xyz.reitmaier.transcribe.data.Email
import xyz.reitmaier.transcribe.data.NewUserRequest
import xyz.reitmaier.transcribe.data.Password

class ApplicationTest {
  @Test
  fun testRoot() = testApplication {
    val response = client.get("/")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Hello World!", response.bodyAsText())
  }

  @Test
  fun testAuth() = testApplication {
    val response = client.get("/protected/route/basic") {
      basicAuth("auth1","auth1")
    }
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Hello auth1", response.bodyAsText())
  }

  @Test
  fun testRegister() = testApplication {
    val client = createClient {
//      install(ContentNegotiation) {
//        json()
//      }
      expectSuccess = false
    }
      val response = client.post("/register") {
        contentType(ContentType.Application.Json)
        setBody(Json.encodeToString(NewUserRequest(Email("test@email.com"), Password("bla"))))
      }
    assertEquals(DuplicateUser.message, response.bodyAsText())
    assertEquals(HttpStatusCode.BadRequest, response.status)
  }
}