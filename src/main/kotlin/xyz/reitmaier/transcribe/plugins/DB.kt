package xyz.reitmaier.transcribe.plugins

import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.asJdbcDriver
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.postgresql.Driver
import xyz.reitmaier.transcribe.data.*
import xyz.reitmaier.transcribe.db.TranscribeDb
import xyz.reitmaier.transcribe.db.User_Entity
import javax.sql.DataSource

fun Application.configureDB(): TranscribeDb {
  val dbConfig = environment.config.config("database")
  val host = dbConfig.property("host").getString()
  val port = dbConfig.property("port").getString()
  val database = dbConfig.property("db").getString()
  val user = dbConfig.property("user").getString()
  val passwd = dbConfig.property("password").getString()
  val maxPoolSize = dbConfig.property("maxPoolSize").getString().toInt()
  val testing = dbConfig.propertyOrNull("testing")

  // See https://github.com/AlecStrong/sql-psi/issues/153
  // Re: ?stringtype=unspecified
  val url = "jdbc:postgresql://$host:$port/$database?stringtype=unspecified"

  val datasourceConfig = HikariConfig().apply {
    jdbcUrl = url
    username = user
    password = passwd
    maximumPoolSize = maxPoolSize

    // Driver needs to be explicitly set in order to produce fatjar
    // https://github.com/brettwooldridge/HikariCP/issues/540
    driverClassName = Driver::class.java.name
  }
  val dataSource : DataSource = HikariDataSource(datasourceConfig)
  val driver : SqlDriver = dataSource.asJdbcDriver()

  driver.migrateIfNeeded(TranscribeDb.Schema)

  val db = TranscribeDb(
    driver = driver,
    User_EntityAdapter = User_Entity.Adapter(
      idAdapter = userIdAdapter,
      emailAdapter = emailAdapter,
      passwordAdapter = encryptedPasswordAdapter,
      created_atAdapter = timestampAdapter
    )
  )

  environment.monitor.subscribe(ApplicationStopped) { driver.close() }

  return db

}
private val timestampFormat: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

private val userIdAdapter = object : ColumnAdapter<UserId, Long> {
  override fun decode(databaseValue: Long): UserId = UserId(databaseValue)
  override fun encode(value: UserId): Long = value.value
}

private val emailAdapter = object : ColumnAdapter<Email, String> {
  override fun decode(databaseValue: String) = Email(databaseValue)
  override fun encode(value: Email) = value.value
}

private val encryptedPasswordAdapter = object : ColumnAdapter<EncryptedPassword, String> {
  override fun decode(databaseValue: String) = EncryptedPassword(databaseValue)
  override fun encode(value: EncryptedPassword) = value.value
}

private val timestampAdapter = object : ColumnAdapter<LocalDateTime, String> {
  override fun decode(databaseValue: String) = LocalDateTime.parse(databaseValue, timestampFormat)
  override fun encode(value: LocalDateTime) = value.toString(timestampFormat)
}
