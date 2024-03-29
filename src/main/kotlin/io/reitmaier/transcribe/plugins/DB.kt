package io.reitmaier.transcribe.plugins

import com.mysql.cj.jdbc.Driver
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.asJdbcDriver
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.config.ApplicationConfig
import io.reitmaier.transcribe.db.Request
import io.reitmaier.transcribe.db.Task
import io.reitmaier.transcribe.db.TranscribeDb
import io.reitmaier.transcribe.db.Transcript
import io.reitmaier.transcribe.db.User
import kotlinx.datetime.Instant
import io.reitmaier.transcribe.data.AssignmentId
import io.reitmaier.transcribe.data.AssignmentStrategy
import io.reitmaier.transcribe.data.DeploymentId
import io.reitmaier.transcribe.data.EncryptedPassword
import io.reitmaier.transcribe.data.MobileNumber
import io.reitmaier.transcribe.data.MobileOperator
import io.reitmaier.transcribe.data.Name
import io.reitmaier.transcribe.data.RefreshToken
import io.reitmaier.transcribe.data.RequestId
import io.reitmaier.transcribe.data.TaskId
import io.reitmaier.transcribe.data.TranscriptId
import io.reitmaier.transcribe.data.UserId
import io.reitmaier.transcribe.db.Assignment
import io.reitmaier.transcribe.db.Deployment
import io.reitmaier.transcribe.db.Deployment_transcriber
import javax.sql.DataSource

private data class DBConfig(
  val host: String,
  val port: String,
  val database: String,
  val user: String,
  val password: String,
  val maxPoolSize: Int,
) {
  val url: String get() =
    "jdbc:mysql://$host:$port/$database"
  companion object {
    fun from(config: ApplicationConfig): DBConfig {
      val dbHost = config.property("database.host").getString()
      val dbPort = config.property("database.port").getString()
      val dbName = config.property("database.db").getString()
      val dbUser = config.property("database.user").getString()
      val dbPassword = config.property("database.password").getString()
      val dbMaxPoolSize = config.property("database.maxPoolSize").getString().toInt()
      return DBConfig(dbHost, dbPort, dbName, dbUser, dbPassword, dbMaxPoolSize)
    }
  }
}
fun Application.configureDB(): TranscribeDb {
  val dbConfig = DBConfig.from(environment.config)

  val datasourceConfig = HikariConfig().apply {
    jdbcUrl = dbConfig.url
    username = dbConfig.user
    password = dbConfig.password
    maximumPoolSize = dbConfig.maxPoolSize

    // Driver needs to be explicitly set in order to produce fatjar
    // https://github.com/brettwooldridge/HikariCP/issues/540
    driverClassName = Driver::class.java.name
  }
  val dataSource: DataSource = HikariDataSource(datasourceConfig)
  val driver: SqlDriver = dataSource.asJdbcDriver()


  val db = TranscribeDb(
    driver = driver,
    userAdapter = User.Adapter(
      idAdapter = userIdAdapter,
      mobile_numberAdapter = mobileNumberAdapter,
      mobile_operatorAdapter = mobileOperatorAdapter,
      nameAdapter = nameAdapter,
      passwordAdapter = encryptedPasswordAdapter,
      created_atAdapter = timestampAdapter,
      refresh_tokenAdapter = refreshTokenAdapter,
    ),
    taskAdapter = Task.Adapter(
      idAdapter = taskIdAdapter,
      user_idAdapter = userIdAdapter,
      provenanceAdapter = EnumColumnAdapter(),
      created_atAdapter = timestampAdapter,
      updated_atAdapter = timestampAdapter,
      completed_atAdapter = timestampAdapter,
      reject_reasonAdapter = EnumColumnAdapter(),
      confidenceAdapter = EnumColumnAdapter(),
      difficultyAdapter = EnumColumnAdapter(),
    ),
    transcriptAdapter = Transcript.Adapter(
      idAdapter = transcriptIdAdapter,
      task_idAdapter = taskIdAdapter,
      client_updated_atAdapter = timestampAdapter,
      created_atAdapter = timestampAdapter,
      updated_atAdapter = timestampAdapter,
    ),
    requestAdapter = Request.Adapter(
      idAdapter = requestIdAdapter,
      user_idAdapter = userIdAdapter,
      assignment_strategyAdapter = assignmentStrategyAdapter,
      created_atAdapter = timestampAdapter,
      updated_atAdapter = timestampAdapter,
      completed_atAdapter = timestampAdapter,
    ),
    assignmentAdapter = io.reitmaier.transcribe.db.Assignment.Adapter(
      idAdapter = assignmentIdAdapter,
      request_idAdapter = requestIdAdapter,
      task_idAdapter = taskIdAdapter,
      assigned_atAdapter = timestampAdapter,
    ),
    deploymentAdapter = Deployment.Adapter(
      idAdapter = deploymentIdAdapter,
      started_atAdapter = timestampAdapter,
      completed_atAdapter = timestampAdapter,
    ),
    deployment_transcriberAdapter = Deployment_transcriber.Adapter(
      deployment_idAdapter = deploymentIdAdapter,
      user_idAdapter = userIdAdapter,
      joined_atAdapter = timestampAdapter,
      finished_atAdapter = timestampAdapter,
    )
  )
  driver.migrate(db)

  environment.monitor.subscribe(ApplicationStopped) { driver.close() }

  return db
}

private fun SqlDriver.migrate(database: TranscribeDb) {
  // Settings table is version 2
  TranscribeDb.Schema.migrate(this, 1, 2)
  val settings = database.settingsQueries.getSettings().executeAsOne()
  val dbVersion = settings.version
  val schemaVersion = TranscribeDb.Schema.version
  println("Current db version: $dbVersion")
  for (version in (dbVersion until schemaVersion)) {
    println("Migrating to ${version + 1}")
    TranscribeDb.Schema.migrate(this, version, version + 1)
    database.settingsQueries.setVersion(version + 1)
  }
}

private val userIdAdapter = object : ColumnAdapter<UserId, Int> {
  override fun decode(databaseValue: Int): UserId = UserId(databaseValue)
  override fun encode(value: UserId): Int = value.value
}

private val taskIdAdapter = object : ColumnAdapter<TaskId, Int> {
  override fun decode(databaseValue: Int): TaskId = TaskId(databaseValue)
  override fun encode(value: TaskId): Int = value.value
}

private val transcriptIdAdapter = object : ColumnAdapter<TranscriptId, Int> {
  override fun decode(databaseValue: Int): TranscriptId = TranscriptId(databaseValue)
  override fun encode(value: TranscriptId): Int = value.value
}

private val deploymentIdAdapter = object : ColumnAdapter<DeploymentId, Int> {
  override fun decode(databaseValue: Int): DeploymentId = DeploymentId(databaseValue)
  override fun encode(value: DeploymentId): Int = value.value
}

private val assignmentStrategyAdapter = object : ColumnAdapter<AssignmentStrategy, Int> {
  override fun decode(databaseValue: Int): AssignmentStrategy = AssignmentStrategy(databaseValue)
  override fun encode(value: AssignmentStrategy): Int = value.value
}

private val requestIdAdapter = object : ColumnAdapter<RequestId, Int> {
  override fun decode(databaseValue: Int): RequestId = RequestId(databaseValue)
  override fun encode(value: RequestId): Int = value.value
}

private val assignmentIdAdapter = object : ColumnAdapter<AssignmentId, Int> {
  override fun decode(databaseValue: Int): AssignmentId = AssignmentId(databaseValue)
  override fun encode(value: AssignmentId): Int = value.value
}

private val mobileNumberAdapter = object : ColumnAdapter<MobileNumber, String> {
  override fun decode(databaseValue: String) = MobileNumber(databaseValue)
  override fun encode(value: MobileNumber) = value.value
}

private val mobileOperatorAdapter = object : ColumnAdapter<MobileOperator, String> {
  override fun decode(databaseValue: String) = MobileOperator(databaseValue)
  override fun encode(value: MobileOperator) = value.value
}

private val nameAdapter = object : ColumnAdapter<Name, String> {
  override fun decode(databaseValue: String) = Name(databaseValue)
  override fun encode(value: Name) = value.value
}

private val refreshTokenAdapter = object : ColumnAdapter<RefreshToken, String> {
  override fun decode(databaseValue: String) = RefreshToken(databaseValue)
  override fun encode(value: RefreshToken) = value.value
}

private val encryptedPasswordAdapter = object : ColumnAdapter<EncryptedPassword, String> {
  override fun decode(databaseValue: String) = EncryptedPassword(databaseValue)
  override fun encode(value: EncryptedPassword) = value.value
}

val timestampAdapter = object : ColumnAdapter<Instant, Long> {
  override fun decode(databaseValue: Long) =
    Instant.fromEpochMilliseconds(databaseValue)

  override fun encode(value: Instant) = value.toEpochMilliseconds()
}

