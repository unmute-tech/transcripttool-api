package xyz.reitmaier.transcribe.data

import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.db.SqlDriver
import xyz.reitmaier.transcribe.db.TranscribeDb

private class SqlDriverTransacter(driver: SqlDriver) : TransacterImpl(driver)

fun SqlDriver.migrateIfNeeded(schema: SqlDriver.Schema) {
    val sqlDriverTransacter = SqlDriverTransacter(this)
    val result = sqlDriverTransacter.transactionWithResult<Pair<Boolean, Int>> {
        var needsMetaTable = false
        val version = try {
            executeQuery(null, "SELECT value FROM __sqldelight__ WHERE name = 'schema_version'", 0).use {
                (if (it.next()) it.getLong(0)?.toInt() else 0) ?: 0
            }
        } catch (e: Exception) {
            needsMetaTable = true
            0
        }
        needsMetaTable to version
    }
    println("Current DB Version = ${result.second}")
    println("DB Schema Version = ${schema.version}")
    sqlDriverTransacter.transaction {
        val (needsMetaTable, version) = result
        if (version < schema.version) {
            println("Updating DB $version -> ${schema.version}")
            schema.migrate(this@migrateIfNeeded, version, schema.version)
            if (needsMetaTable) {
                execute(null, "CREATE TABLE __sqldelight__(name VARCHAR(64) NOT NULL PRIMARY KEY, value VARCHAR(64))", 0)
            }
            if (version == 0) {
                execute(null, "INSERT INTO __sqldelight__(name, value) VALUES('schema_version', ${TranscribeDb.Schema.version})", 0)
            } else {
                execute(null, "UPDATE __sqldelight__ SET value='${TranscribeDb.Schema.version}' WHERE name='schema_version'", 0)
            }
        }
    }
}