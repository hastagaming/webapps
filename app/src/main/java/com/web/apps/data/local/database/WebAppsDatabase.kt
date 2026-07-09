package com.web.apps.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.web.apps.data.local.converter.Converters
import com.web.apps.data.local.dao.ContainerDao
import com.web.apps.data.local.dao.DownloadDao
import com.web.apps.data.local.dao.GroupDao
import com.web.apps.data.local.entity.ContainerEntity
import com.web.apps.data.local.entity.DownloadEntity
import com.web.apps.data.local.entity.GroupEntity

@Database(
    entities = [
        GroupEntity::class,
        ContainerEntity::class,
        DownloadEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WebAppsDatabase : RoomDatabase() {

    abstract fun groupDao(): GroupDao
    abstract fun containerDao(): ContainerDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        const val DATABASE_NAME = "webapps_database"
    }
}