import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_SRC_DIR_JAVA
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_SRC_DIR_KOTLIN
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_TEST_SRC_DIR_JAVA
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_TEST_SRC_DIR_KOTLIN
val main_class by extra("io.ktor.server.netty.EngineMain")
val docker_image = "transcriptapi:0.0.1"

@Suppress("DSL_SCOPE_VIOLATION") plugins {
  application
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.sqldelight)
  alias(libs.plugins.detekt)

  // Docker
  // TODO https://github.com/ktorio/ktor-build-plugins better alternative?
  alias(libs.plugins.jib)
}

sqldelight {
  database("TranscribeDb") {
    packageName = "xyz.reitmaier.transcribe.db"
    dialect = "mysql"
    deriveSchemaFromMigrations = true
  }
}
group = "xyz.reitmaier"
version = "0.0.1"
application {
  mainClass.set(main_class)
}

repositories {
  mavenCentral()
}

// make all when statements exhaustive by default and opt-in to errors instead of warnings
kotlin {
  sourceSets.all {
    languageSettings {
      languageVersion = "1.6"
      progressiveMode = true
    }
  }
}

dependencies {
  // ktor
  implementation(libs.ktor.server.auth)
  implementation(libs.ktor.server.auth.jwt)
  implementation(libs.ktor.server.auto.head.response)
  implementation(libs.ktor.server.content.negotiation)
  implementation(libs.ktor.server.default.headers)
  implementation(libs.ktor.server.partial.content)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.ktor.server.core.jvm)
  implementation(libs.ktor.server.netty.jvm)
  implementation(libs.ktor.server.html.builder)

  // db -- config
  implementation(libs.hikari)

  // db -- mysql
  implementation(libs.mysql.connector.java)

  // db -- sqldelight
  implementation(libs.squareup.sqldelight.runtime.jvm)
  implementation(libs.squareup.sqldelight.jdbc.driver)
  implementation(libs.squareup.sqldelight.coroutine.extensions)

  // utilities
  implementation(libs.kotlinx.datetime)

  // logging
  implementation(libs.ktor.server.call.logging)
  implementation(libs.logback.classic)
  implementation(libs.kotlin.inline.logger)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)

  // Result monad for modelling success/failure operations
  implementation(libs.kotlin.result)
  implementation(libs.kotlin.result.coroutines)

  // testing
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.ktor.server.tests.jvm)
}

jib {
  container {
    ports = listOf("8088")
    mainClass = main_class
    to.image = docker_image
    appRoot = "/app"
    // good defauls intended for Java 8 (>= 8u191) containers
    jvmFlags = listOf(
      "-server",
      "-Djava.awt.headless=true",
      "-XX:InitialRAMFraction=2",
      "-XX:MinRAMFraction=2",
      "-XX:MaxRAMFraction=2",
      "-XX:+UseG1GC",
      "-XX:MaxGCPauseMillis=100",
      "-XX:+UseStringDeduplication"
    )
  }
}

allprojects {
  apply {
    plugin(rootProject.libs.plugins.detekt.get().pluginId)
  }

  dependencies {
    detektPlugins(rootProject.libs.io.gitlab.arturbosch.detekt.formatting)
  }

  detekt {
    source = files(
      "src",
      DEFAULT_SRC_DIR_JAVA,
      DEFAULT_TEST_SRC_DIR_JAVA,
      DEFAULT_SRC_DIR_KOTLIN,
      DEFAULT_TEST_SRC_DIR_KOTLIN,
    )
    toolVersion = rootProject.libs.versions.detekt.get()
    config = rootProject.files("config/detekt/detekt.yml")
    parallel = true
  }
}
