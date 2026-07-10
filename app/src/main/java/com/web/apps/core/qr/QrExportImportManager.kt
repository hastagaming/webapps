package com.web.apps.core.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import android.graphics.Color
import com.web.apps.data.local.dao.ContainerDao
import com.web.apps.data.local.dao.GroupDao
import com.web.apps.data.local.entity.ContainerEntity
import com.web.apps.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

sealed class QrImportResult {
    data class Success(val containersImported: Int, val groupsImported: Int) : QrImportResult()
    data class Failure(val message: String) : QrImportResult()
}

@Singleton
class QrExportImportManager @Inject constructor(
    private val groupDao: GroupDao,
    private val containerDao: ContainerDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun buildExportString(): String {
        val groups = groupDao.observeAllGroups().first()
        val containers = containerDao.observeAllContainers().first()

        val payload = QrExportPayload(
            groups = groups.map { QrGroupModel(n = it.name, c = it.colorHex, p = it.position) },
            containers = containers.map {
                QrContainerModel(
                    n = it.name,
                    u = it.url,
                    g = it.groupId?.let { gid -> groups.indexOfFirst { g -> g.groupId == gid } }.takeIf { it != null && it >= 0 },
                    p = it.position
                )
            }
        )

        val jsonString = json.encodeToString(payload)
        return QrDataCodec.encode(jsonString)
    }

    fun generateQrBitmap(data: String, size: Int = 800): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    suspend fun importFromScannedString(scannedData: String): QrImportResult {
        return try {
            val jsonString = QrDataCodec.decode(scannedData)
            val payload = json.decodeFromString<QrExportPayload>(jsonString)

            val groupIndexToNewId = mutableMapOf<Int, Long>()
            payload.groups.forEachIndexed { index, group ->
                val newId = groupDao.insertGroup(
                    GroupEntity(name = group.n, colorHex = group.c, position = group.p)
                )
                groupIndexToNewId[index] = newId
            }

            payload.containers.forEach { container ->
                val remappedGroupId = container.g?.let { groupIndexToNewId[it] }
                containerDao.insertContainer(
                    ContainerEntity(
                        name = container.n,
                        url = container.u,
                        groupId = remappedGroupId,
                        position = container.p
                    )
                )
            }

            QrImportResult.Success(
                containersImported = payload.containers.size,
                groupsImported = payload.groups.size
            )
        } catch (e: Exception) {
            QrImportResult.Failure(e.message ?: "Invalid or corrupted QR data.")
        }
    }
}