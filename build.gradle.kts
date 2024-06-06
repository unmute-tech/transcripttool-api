import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.ktor.plugin.features.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val main_class by extra("io.ktor.server.netty.EngineMain")

plugins {
  application
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.sqldelight)
  alias(libs.plugins.ktor)
  alias(libs.plugins.version.catalog.update)
  alias(libs.plugins.gradle.versions)
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

val javaVersion = JavaVersion.VERSION_17;
java {
  sourceCompatibility = javaVersion
  targetCompatibility = javaVersion
}
tasks {
  withType<KotlinCompile>().configureEach {
    kotlinOptions {
      jvmTarget = "$javaVersion"
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
    val dockerImageName = "transcriptapi"
    jreVersion.set(javaVersion)
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

versionCatalogUpdate {
  // sort the catalog by key (default is true)
  sortByKey.set(true)

  keep {
    // keep versions without any library or plugin reference
    keepUnusedVersions.set(true)
    // keep all libraries that aren't used in the project
    keepUnusedLibraries.set(true)
    // keep all plugins that aren't used in the project
    keepUnusedPlugins.set(true)
  }
}
fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isStable = stableKeyword || regex.matches(version)
  return isStable.not()
}
// https://github.com/ben-manes/gradle-versions-plugin
tasks.withType<DependencyUpdatesTask> {
  resolutionStrategy {
    componentSelection {
      all {
        if (isNonStable(candidate.version) && !isNonStable(currentVersion)) {
          reject("Release candidate")
        }
      }
    }
  }
}
