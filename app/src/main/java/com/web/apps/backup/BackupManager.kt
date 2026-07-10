package com.web.apps.backup

import android.content.Context
import android.net.Uri
import com.web.apps.core.security.KeystoreManager
import com.web.apps.data.local.dao.ContainerDao
import com.web.apps.data.local.dao.GroupDao
import com.web.apps.data.local.entity.ContainerEntity
import com.web.apps.data.local.entity.GroupEntity
import com.web.apps.data.local.entity.OrientationMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

sealed class BackupResult {
    data class Success(val message: String) : BackupResult()
    data class Failure(val reason: String) : BackupResult()
}

@Singleton
class BackupManager @Inject constructor(
    private val groupDao: GroupDao,
    private val containerDao: ContainerDao,
    private val keystoreManager: KeystoreManager
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    companion object {
        private const val BACKUP_FILE_MAGIC = "WEBAPPS_BACKUP_V1"
    }

    suspend fun exportBackup(
        context: Context,
        destinationUri: Uri,
        encrypt: Boolean
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val groups = groupDao.observeAllGroups().first()
            val containers = containerDao.observeAllContainers().first()

            val payload = BackupPayload(
                exportedAt = System.currentTimeMillis(),
                appVersionName = getAppVersionName(context),
                groups = groups.map {
                    BackupGroupModel(
                        groupId = it.groupId,
                        name = it.name,
                        colorHex = it.colorHex,
                        position = it.position,
                        createdAt = it.createdAt
                    )
                },
                containers = containers.map {
                    BackupContainerModel(
                        containerId = it.containerId,
                        name = it.name,
                        url = it.url,
                        faviconUrl = it.faviconUrl,
                        groupId = it.groupId,
                        position = it.position,
                        isDesktopMode = it.isDesktopMode,
                        orientationMode = it.orientationMode.name,
                        isKeepAliveEnabled = it.isKeepAliveEnabled,
                        isFullscreenEnabled = it.isFullscreenEnabled,
                        isLocked = false,
                        isHttpAllowed = it.isHttpAllowed,
                        userAgentOverride = it.userAgentOverride,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                }
            )

            val jsonString = json.encodeToString(payload)
            val fileContent = if (encrypt) {
                val encrypted = keystoreManager.encrypt(jsonString.toByteArray(Charsets.UTF_8))
                buildEncryptedFileContent(encrypted)
            } else {
                "$BACKUP_FILE_MAGIC\nPLAIN\n$jsonString"
            }

            val outputStream: OutputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext BackupResult.Failure("Could not open the destination file.")

            outputStream.use { stream ->
                stream.write(fileContent.toByteArray(Charsets.UTF_8))
                stream.flush()
            }

            BackupResult.Success("Backup saved successfully (${containers.size} containers, ${groups.size} groups)")
        } catch (e: Exception) {
            BackupResult.Failure("Export failed: ${e.message}")
          }
    }

    suspend fun importBackup(
        context: Context,
        sourceUri: Uri,
        mergeStrategy: ImportMergeStrategy
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext BackupResult.Failure("Unable To Open Backup File")

            val rawContent = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }

            if (!rawContent.startsWith(BACKUP_FILE_MAGIC)) {
                return@withContext BackupResult.Failure("The file is not a WebApps backup format")
            }

            val lines = rawContent.lines()
            val mode = lines.getOrNull(1)?.trim() ?: return@withContext BackupResult.Failure("Corrupt Backup Format")

            val jsonString = when (mode) {
                "PLAIN" -> lines.drop(2).joinToString("\n")
                "ENCRYPTED" -> {
                    val encryptedBase64 = lines.getOrNull(2) ?: return@withContext BackupResult.Failure("Encrypted Data Not Decrypted")
                    val ivBase64 = lines.getOrNull(3) ?: return@withContext BackupResult.Failure("IV Not Found")
                    val cipherText = Base64.getDecoder().decode(encryptedBase64)
                    val iv = Base64.getDecoder().decode(ivBase64)
                    val decrypted = keystoreManager.decrypt(
                        KeystoreManager.EncryptedPayload(cipherText, iv)
                    )
                    String(decrypted, Charsets.UTF_8)
                }
                else -> return@withContext BackupResult.Failure("Unrecognized Coverage Mode: $mode")
            }

            val payload = json.decodeFromString<BackupPayload>(jsonString)
            applyImport(payload, mergeStrategy)

            BackupResult.Success(
                "Succesfully Restore (${payload.containers.size} container, ${payload.groups.size} group)"
            )
        } catch (e: Exception) {
            BackupResult.Failure("Import Failed: ${e.message}")
        }
    }

    suspend fun exportBackupAutoToDownloads(context: Context, encrypt: Boolean): BackupResult = withContext(Dispatchers.IO) {
        try {
            val groups = groupDao.observeAllGroups().first()
            val containers = containerDao.observeAllContainers().first()

            val payload = BackupPayload(
                exportedAt = System.currentTimeMillis(),
                appVersionName = getAppVersionName(context),
                groups = groups.map {
                    BackupGroupModel(
                        groupId = it.groupId,
                        name = it.name,
                        colorHex = it.colorHex,
                        position = it.position,
                        createdAt = it.createdAt
                    )
                },
                containers = containers.map {
                    BackupContainerModel(
                        containerId = it.containerId,
                        name = it.name,
                        url = it.url,
                        faviconUrl = it.faviconUrl,
                        groupId = it.groupId,
                        position = it.position,
                        isDesktopMode = it.isDesktopMode,
                        orientationMode = it.orientationMode.name,
                        isKeepAliveEnabled = it.isKeepAliveEnabled,
                        isFullscreenEnabled = it.isFullscreenEnabled,
                        isLocked = false,
                        isHttpAllowed = it.isHttpAllowed,
                        userAgentOverride = it.userAgentOverride,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                }
            )

            val jsonString = json.encodeToString(payload)
            val fileContent = if (encrypt) {
                val encrypted = keystoreManager.encrypt(jsonString.toByteArray(Charsets.UTF_8))
                buildEncryptedFileContent(encrypted)
            } else {
                "$BACKUP_FILE_MAGIC\nPLAIN\n$jsonString"
            }

            val fileName = "webapps_auto_backup_${System.currentTimeMillis()}.txt"
            val uri = createDownloadsFile(context, fileName)
                ?: return@withContext BackupResult.Failure("Could not create backup file in Downloads.")

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(fileContent.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: return@withContext BackupResult.Failure("Could not open output stream.")

            cleanupOldAutoBackups(context)

            BackupResult.Success("Auto backup saved (${containers.size} containers, ${groups.size} groups)")
        } catch (e: Exception) {
            BackupResult.Failure("Auto backup failed: ${e.message}")
        }
    }

    private fun createDownloadsFile(context: Context, fileName: String): Uri? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/WebApps/backups")
            }
            context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        } else {
            val dir = java.io.File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                "WebApps/backups"
            )
            if (!dir.exists()) dir.mkdirs()
            val file = java.io.File(dir, fileName)
            Uri.fromFile(file)
        }
    }

    private fun cleanupOldAutoBackups(context: Context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val projection = arrayOf(
                    android.provider.MediaStore.Downloads._ID,
                    android.provider.MediaStore.Downloads.DISPLAY_NAME,
                    android.provider.MediaStore.Downloads.DATE_ADDED
                )
                val selection = "${android.provider.MediaStore.Downloads.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf("Download/WebApps/backups/")

                val entries = mutableListOf<Pair<Long, Uri>>()
                context.contentResolver.query(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection, selection, selectionArgs,
                    "${android.provider.MediaStore.Downloads.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Downloads._ID)
                    val dateIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Downloads.DATE_ADDED)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)
                        val date = cursor.getLong(dateIndex)
                        val uri = android.content.ContentUris.withAppendedId(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                        entries.add(date to uri)
                    }
                }

                if (entries.size > 5) {
                    entries.drop(5).forEach { (_, uri) ->
                        context.contentResolver.delete(uri, null, null)
                    }
                }
            }
        } catch (e: Exception) {
            // gagal cleanup tidak boleh crash
        }
    }

    private suspend fun applyImport(payload: BackupPayload, strategy: ImportMergeStrategy) {
        if (strategy == ImportMergeStrategy.REPLACE_ALL) {
            val existingContainers = containerDao.observeAllContainers().first()
            existingContainers.forEach { containerDao.deleteContainer(it) }
            val existingGroups = groupDao.observeAllGroups().first()
            existingGroups.forEach { groupDao.deleteGroup(it) }
        }

        val groupIdRemap = mutableMapOf<Long, Long>()

        payload.groups.forEach { backupGroup ->
            val newGroupId = groupDao.insertGroup(
                GroupEntity(
                    name = backupGroup.name,
                    colorHex = backupGroup.colorHex,
                    position = backupGroup.position,
                    createdAt = backupGroup.createdAt
                )
            )
            groupIdRemap[backupGroup.groupId] = newGroupId
        }

        payload.containers.forEach { backupContainer ->
            val remappedGroupId = backupContainer.groupId?.let { groupIdRemap[it] }
            containerDao.insertContainer(
                ContainerEntity(
                    name = backupContainer.name,
                    url = backupContainer.url,
                    faviconUrl = backupContainer.faviconUrl,
                    groupId = remappedGroupId,
                    position = backupContainer.position,
                    isDesktopMode = backupContainer.isDesktopMode,
                    orientationMode = OrientationMode.valueOf(backupContainer.orientationMode),
                    isKeepAliveEnabled = backupContainer.isKeepAliveEnabled,
                    isFullscreenEnabled = backupContainer.isFullscreenEnabled,
                    isLocked = false,
                    lockPinHash = null,
                    isHttpAllowed = backupContainer.isHttpAllowed,
                    userAgentOverride = backupContainer.userAgentOverride,
                    createdAt = backupContainer.createdAt,
                    updatedAt = backupContainer.updatedAt
                )
            )
        }
    }

    private fun buildEncryptedFileContent(encrypted: KeystoreManager.EncryptedPayload): String {
        val cipherTextBase64 = Base64.getEncoder().encodeToString(encrypted.cipherText)
        val ivBase64 = Base64.getEncoder().encodeToString(encrypted.iv)
        return "$BACKUP_FILE_MAGIC\nENCRYPTED\n$cipherTextBase64\n$ivBase64"
    }

    private fun getAppVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}

enum class ImportMergeStrategy {
    MERGE_KEEP_BOTH,
    REPLACE_ALL
}