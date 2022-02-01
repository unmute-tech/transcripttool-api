package xyz.reitmaier

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.reitmaier.transcribe.data.*

class ApplicationTest {
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
        setBody(Json.encodeToString(CreateUserRequest(Email("test@email.com"), Password("bla"))))
      }
    assertEquals(DuplicateUser.message, response.bodyAsText())
    assertEquals(HttpStatusCode.BadRequest, response.status)
  }

  @Test
  fun `login test`() = testApplication {

    val client = createClient {
      expectSuccess = false
    }
    val response = client.post("/login") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(UserAccount(Email("test@email.com"), Password("bla"))))
    }
    assertEquals(HttpStatusCode.OK, response.status)
    val hashMap = Json.decodeFromString<HashMap<String,String>>(response.bodyAsText())
    assertContains(hashMap,"token")
  }

  @Test
  fun `auth declined test`() = testApplication {
    val client = createClient {
      expectSuccess = false
    }
    val response = client.get("/auth-test") {
    }
    assertEquals(HttpStatusCode.Unauthorized, response.status)
  }

  @Test
  fun `auth test`() = testApplication {
    val token = client.loginToken()
    val response = client.get("/auth-test") {
      bearerAuth(token)
    }
    assertEquals(HttpStatusCode.OK, response.status)
  }


  @Test
  fun `create new task by user`() = testApplication {
    val token = client.loginToken()
    val client = createClient {
      expectSuccess = false
    }
    val response = client.post("/task") {
      bearerAuth(token)
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(
        CreateTaskRequest(
          displayName = "test-task.oga",
          lengthMs = 60*1000L
        )))
    }
    assertEquals(DuplicateFile.message, response.bodyAsText())
    assertEquals(HttpStatusCode.BadRequest, response.status)
  }

}

private suspend fun HttpClient.loginToken() : String {
  val response = post("/login") {
    contentType(ContentType.Application.Json)
    setBody(Json.encodeToString(UserAccount(Email("test@email.com"), Password("bla"))))
  }
  val hashMap = Json.decodeFromString<HashMap<String,String>>(response.bodyAsText())
  assertContains(hashMap,"token")

  return hashMap["token"]!!
}