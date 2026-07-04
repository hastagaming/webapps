package com.web.apps.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
        }
    }

    fun getAllMigrations(): Array<Migration> = arrayOf(
        MIGRATION_1_2
    )
}