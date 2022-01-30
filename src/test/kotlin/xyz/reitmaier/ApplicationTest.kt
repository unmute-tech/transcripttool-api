package xyz.reitmaier

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*

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
}