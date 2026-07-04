package com.web.apps.data.local.converter

import androidx.room.TypeConverter
import com.web.apps.data.local.entity.DownloadStatus
import com.web.apps.data.local.entity.OrientationMode

class Converters {

    @TypeConverter
    fun fromOrientationMode(value: OrientationMode): String = value.name

    @TypeConverter
    fun toOrientationMode(value: String): OrientationMode = OrientationMode.valueOf(value)

    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String = value.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)
}