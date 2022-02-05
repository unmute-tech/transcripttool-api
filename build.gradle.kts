val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val coroutines_version: String by project
val postgres_version: String by project
val sqldelight_version: String by project
val hikaricp_version: String by project

plugins {
  application
  kotlin("jvm") version "1.6.10"
  id("org.jetbrains.kotlin.plugin.serialization") version "1.6.10"

  // DB
  id("com.squareup.sqldelight") version "1.5.3"
}

sqldelight {
  database("TranscribeDb") {
    packageName = "xyz.reitmaier.transcribe.db"
    dialect = "mysql"
    deriveSchemaFromMigrations = true
//    migrationOutputDirectory = file("$buildDir/resources/main/migrations")
//    migrationOutputFileFormat = ".sql"
  }
}
group = "xyz.reitmaier"
version = "0.0.1"
application {
  mainClass.set("xyz.reitmaier.ApplicationKt")
}

repositories {
  mavenCentral()
  maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
  // ktor
  implementation("io.ktor:ktor-server-core:$ktor_version")
  implementation("io.ktor:ktor-server-auth:$ktor_version")
  implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
  implementation("io.ktor:ktor-server-auto-head-response:$ktor_version")
  implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
  implementation("io.ktor:ktor-server-default-headers:$ktor_version")
  implementation("io.ktor:ktor-server-partial-content:$ktor_version")
  implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
  implementation("io.ktor:ktor-server-netty:$ktor_version")

  // db -- config
  implementation("com.zaxxer:HikariCP:$hikaricp_version")

  // db -- postgres
//  implementation("org.postgresql:postgresql:$postgres_version")

  // db -- mysql
  implementation("mysql:mysql-connector-java:8.0.28")

  // db -- sqldelight
  implementation("com.squareup.sqldelight:runtime-jvm:$sqldelight_version")
  implementation("com.squareup.sqldelight:jdbc-driver:$sqldelight_version")
  implementation("com.squareup.sqldelight:coroutines-extensions:$sqldelight_version")

  // utilities
  implementation("joda-time:joda-time:2.10.13")

  // logging
  implementation("io.ktor:ktor-server-call-logging:$ktor_version")
  implementation("ch.qos.logback:logback-classic:$logback_version")
  implementation("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger:1.0.4")

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")


  // Result monad for modelling success/failure operations
  implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.14")
  implementation("com.michael-bull.kotlin-result:kotlin-result-coroutines:1.1.14")

  // testing
  testImplementation("io.ktor:ktor-server-tests:$ktor_version")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}