package com.web.apps.data.repository

import com.web.apps.data.local.dao.ContainerDao
import com.web.apps.data.local.entity.ContainerEntity
import com.web.apps.data.local.entity.OrientationMode
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContainerRepository @Inject constructor(
    private val containerDao: ContainerDao
) {

    fun observeAllContainers(): Flow<List<ContainerEntity>> = containerDao.observeAllContainers()

    fun observeContainersByGroup(groupId: Long): Flow<List<ContainerEntity>> =
        containerDao.observeContainersByGroup(groupId)

    fun observeUngroupedContainers(): Flow<List<ContainerEntity>> =
        containerDao.observeUngroupedContainers()

    fun observeContainerById(containerId: Long): Flow<ContainerEntity?> =
        containerDao.observeContainerById(containerId)

    fun searchContainers(query: String): Flow<List<ContainerEntity>> =
        containerDao.searchContainers(query)

    suspend fun getContainerById(containerId: Long): ContainerEntity? =
        containerDao.getContainerById(containerId)

    suspend fun createContainer(
        name: String,
        url: String,
        groupId: Long? = null
    ): Long {
        val validatedUrl = UrlValidator.normalize(url)
        val position = containerDao.countContainers()
        val container = ContainerEntity(
            name = name,
            url = validatedUrl,
            groupId = groupId,
            position = position
        )
        return containerDao.insertContainer(container)
    }

    suspend fun updateContainer(container: ContainerEntity) {
        containerDao.updateContainer(
            container.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteContainer(container: ContainerEntity) {
        containerDao.deleteContainer(container)
    }

    suspend fun markAccessed(containerId: Long) {
        containerDao.updateLastAccessed(containerId)
    }

    suspend fun updateFavicon(containerId: Long, faviconUrl: String?, localPath: String?) {
        containerDao.updateFavicon(containerId, faviconUrl, localPath)
    }

    suspend fun setDesktopMode(containerId: Long, enabled: Boolean) {
        containerDao.updateDesktopMode(containerId, enabled)
    }

    suspend fun setOrientationMode(containerId: Long, mode: OrientationMode) {
        containerDao.updateOrientationMode(containerId, mode)
    }

    suspend fun setKeepAlive(containerId: Long, enabled: Boolean) {
        containerDao.updateKeepAlive(containerId, enabled)
    }

    suspend fun reorderContainer(containerId: Long, newPosition: Int) {
        containerDao.updatePosition(containerId, newPosition)
    }

    suspend fun moveToGroup(containerId: Long, groupId: Long?) {
        containerDao.moveToGroup(containerId, groupId)
    }

    suspend fun getKeepAliveContainers(): List<ContainerEntity> =
        containerDao.getKeepAliveContainers()

    suspend fun lockContainer(containerId: Long, pin: String) {
        val hash = hashPin(pin)
        containerDao.updateLockState(containerId, isLocked = true, pinHash = hash)
    }

    suspend fun unlockContainer(containerId: Long, pin: String): Boolean {
        val container = containerDao.getContainerById(containerId) ?: return false
        val hash = hashPin(pin)
        return if (container.lockPinHash == hash) {
            containerDao.updateLockState(containerId, isLocked = false, pinHash = null)
            true
        } else {
            false
        }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

object UrlValidator {

    private val TYPO_DOMAIN_MAP = mapOf(
        "gogle.com" to "google.com",
        "goggle.com" to "google.com",
        "youtub.com" to "youtube.com",
        "youtue.com" to "youtube.com",
        "facbook.com" to "facebook.com",
        "facebok.com" to "facebook.com",
        "instagran.com" to "instagram.com",
        "instgram.com" to "instagram.com",
        "gmial.com" to "gmail.com",
        "twiter.com" to "twitter.com"
    )

    fun normalize(rawUrl: String): String {
        var input = rawUrl.trim()

        if (!input.contains("://")) {
            input = "https://$input"
        }

        val uri = try {
            java.net.URI(input)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid URL: $rawUrl")
        }

        var host = uri.host ?: throw IllegalArgumentException("URL has no host: $rawUrl")
        host = correctTypo(host)

        val scheme = if (uri.scheme == "http") "http" else "https"
        val path = uri.rawPath ?: ""
        val query = if (uri.rawQuery != null) "?${uri.rawQuery}" else ""

        return "$scheme://$host$path$query"
    }

    private fun correctTypo(host: String): String {
        return TYPO_DOMAIN_MAP[host] ?: host
    }

    fun isHttps(url: String): Boolean = url.startsWith("https://", ignoreCase = true)

    fun isHttp(url: String): Boolean = url.startsWith("http://", ignoreCase = true)

    fun isValid(url: String): Boolean {
        return try {
            val uri = java.net.URI(url)
            uri.host != null && (uri.scheme == "http" || uri.scheme == "https")
        } catch (e: Exception) {
            false
        }
    }
}