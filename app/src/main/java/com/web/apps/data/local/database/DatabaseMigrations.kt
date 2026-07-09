package com.web.apps.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE groups ADD COLUMN iconUri TEXT")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE groups ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE containers ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")

            val groupCursor = db.query("SELECT groupId FROM groups")
            while (groupCursor.moveToNext()) {
                val id = groupCursor.getLong(0)
                db.execSQL("UPDATE groups SET cloudId = ? WHERE groupId = ?", arrayOf(UUID.randomUUID().toString(), id))
            }
            groupCursor.close()

            val containerCursor = db.query("SELECT containerId FROM containers")
            while (containerCursor.moveToNext()) {
                val id = containerCursor.getLong(0)
                db.execSQL("UPDATE containers SET cloudId = ? WHERE containerId = ?", arrayOf(UUID.randomUUID().toString(), id))
            }
            containerCursor.close()

            db.execSQL("CREATE INDEX IF NOT EXISTS index_containers_cloudId ON containers(cloudId)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE containers ADD COLUMN openCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE containers ADD COLUMN totalUsageMillis INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE containers ADD COLUMN isNotificationEnabled INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE containers ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
        }
    }

    fun getAllMigrations(): Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6
    )
}