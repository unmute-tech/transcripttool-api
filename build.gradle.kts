import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_SRC_DIR_JAVA
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_SRC_DIR_KOTLIN
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_TEST_SRC_DIR_JAVA
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_TEST_SRC_DIR_KOTLIN
import io.ktor.plugin.features.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val main_class by extra("io.ktor.server.netty.EngineMain")

plugins {
  application
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.sqldelight)
  alias(libs.plugins.detekt)
  alias(libs.plugins.ktor)
}

sqldelight {
  database("TranscribeDb") {
    packageName = "io.reitmaier.transcribe.db"
    dialect = "mysql"
    deriveSchemaFromMigrations = true
  }
}
group = "io.reitmaier"
version = "0.0.1"
application {
  mainClass.set(main_class)
}

repositories {
  mavenCentral()
}

java {
  // align with ktor/docker JRE
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}
tasks {
  withType<KotlinCompile>().configureEach {
    kotlinOptions {
      jvmTarget = "${JavaVersion.VERSION_17}"
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

class PrivateImageRegistry(
  registryUrl: Provider<String>,
  imageName: String,
  override val username: Provider<String>,
  override val password: Provider<String>,
): DockerImageRegistry {
  override val toImage: Provider<String> = provider {
    "${registryUrl.get().substringAfter("://").trim('/')}/$imageName"
  }
}
ktor {
  docker {
    // align with java config
    val dockerImageName = "transcriptapi"
    jreVersion.set(io.ktor.plugin.features.JreVersion.JRE_17)
    localImageName.set(dockerImageName)
    imageTag.set("latest")
    externalRegistry.set(
      PrivateImageRegistry(
        providers.environmentVariable("PRIVATE_DOCKER_REGISTRY_URL"),
        dockerImageName,
        providers.environmentVariable("PRIVATE_DOCKER_REGISTRY_USER"),
        providers.environmentVariable("PRIVATE_DOCKER_REGISTRY_PASSWORD"),
      )
    )
    // can also use DockerImageRegistry.dockerHub()
    // or just use local image and delete external registry
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
