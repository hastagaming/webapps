package com.web.apps.core.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.web.apps.data.local.dao.ContainerDao
import com.web.apps.data.local.dao.GroupDao
import com.web.apps.data.local.entity.ContainerEntity
import com.web.apps.data.local.entity.GroupEntity
import com.web.apps.data.local.entity.OrientationMode
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseSyncManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val firebaseAuth: FirebaseAuth,
    private val containerDao: ContainerDao,
    private val groupDao: GroupDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context
) {
    private fun currentUserEmail(): String? = firebaseAuth.currentUser?.email

    suspend fun pushGroup(group: GroupEntity) {
        val email = currentUserEmail()
        com.web.apps.core.crash.DebugLogger.log(appContext, "SupabaseSync", "pushGroup called, email=$email")
        if (email == null) {
            com.web.apps.core.crash.DebugLogger.log(appContext, "SupabaseSync", "pushGroup aborted: no current user email")
            return
        }
        try {
            val remote = GroupRemote(
                user_email = email,
                cloud_id = group.cloudId,
                name = group.name,
                color_hex = group.colorHex,
                icon_uri = group.iconUri,
                position = group.position
            )
            supabaseClient.postgrest.from("groups").upsert(remote, onConflict = "user_email,cloud_id")
        } catch (e: Exception) {
            Log.e("SupabaseSync", "pushGroup failed", e)
            com.web.apps.core.crash.DebugLogger.log(appContext, "SupabaseSync", "pushGroup FAILED: ${e.message}")
        }
    }

    suspend fun deleteGroupRemote(cloudId: String) {
        val email = currentUserEmail()
        com.web.apps.core.crash.DebugLogger.log(appContext, "SupabaseSync", "deleteGroupRemote called, email=$email")
        if (email == null) {
            com.web.apps.core.crash.DebugLogger.log(appContext, "SupabaseSync", "deleteGroupRemote aborted: no current user email")
            return
        }
        try {
            supabaseClient.postgrest.from("groups").delete {
                filter {
                    eq("user_email", email)
                    eq("cloud_id", cloudId)
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "deleteGroupRemote failed", e)
            com.web.apps.core.crash.DebugLogger.log(appContext, "SupabaseSync", "deleteGroupRemote FAILED: ${e.message}")
        }
    }

    suspend fun pushContainer(container: ContainerEntity) {
        val email = currentUserEmail()
        com.web.apps.core.crash.DebugLogger.log(appContext, "SupabaseSync", "pushContainer called, email=$email")
        if (email == null) {
            com.web.apps.core.crash.DebugLogger.log(appContext, "SupabaseSync", "pushContainer aborted: no current user email")
            return
        }
        try {
            val groupCloudId = container.groupId?.let { groupDao.getGroupById(it)?.cloudId }

            val remote = ContainerRemote(
                user_email = email,
                cloud_id = container.cloudId,
                group_cloud_id = groupCloudId,
                name = container.name,
                url = container.url,
                favicon_url = container.faviconUrl,
                position = container.position,
                is_desktop_mode = container.isDesktopMode,
                orientation_mode = container.orientationMode.name,
                is_keep_alive_enabled = container.isKeepAliveEnabled,
                is_fullscreen_enabled = container.isFullscreenEnabled,
                is_http_allowed = container.isHttpAllowed,
                user_agent_override = container.userAgentOverride
            )
            supabaseClient.postgrest.from("containers").upsert(remote, onConflict = "user_email,cloud_id")
        } catch (e: Exception) {
            Log.e("SupabaseSync", "pushContainer failed", e)
            com.web.apps.core.crash.DebugLogger.log(appContext, "SupabaseSync", "pushContainer FAILED: ${e.message}")
        }
    }

    suspend fun deleteContainerRemote(cloudId: String) {
        val email = currentUserEmail()
        com.web.apps.core.crash.DebugLogger.log(appContext, "SupabaseSync", "deleteContainrrRemote called, email=$email")
        if (email == null) {
            com.web.apps.core.crash.DebugLogger.log(appContext, "SupabaseSync", "deleteContainerRemote aborted: no current user email")
            return
        }
        try {
            supabaseClient.postgrest.from("containers").delete {
                filter {
                    eq("user_email", email)
                    eq("cloud_id", cloudId)
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "deleteContainerRemote failed", e)
            com.web.apps.core.crash.DebugLogger.log(appContext, "SupabaseSync", "deleteContainerRemote FAILED: ${e.message}")
        }
    }

    suspend fun pullAndMergeAll() {
        val email = currentUserEmail()
        com.web.apps.core.crash.DebugLogger.log(appContext, "SupabaseSync", "pullAndMergeAll called, email=$email")
        if (email == null) {
            com.web.apps.core.crash.DebugLogger.log(appContext, "SupabaseSync", "pullAndMergeQll aborted: no current user email")
            return
        }
        try {
            val existingGroupCloudIds = groupDao.getAllGroupsOnce().map { it.cloudId }.toSet()
            val existingContainerCloudIds = containerDao.getAllContainersOnce().map { it.cloudId }.toSet()
            val cloudIdToLocalGroupId = mutableMapOf<String, Long>()

            val remoteGroups = supabaseClient.postgrest.from("groups").select {
                filter { eq("user_email", email) }
            }.decodeList<GroupRemote>()

            for (doc in remoteGroups) {
                if (doc.cloud_id in existingGroupCloudIds) continue

                val group = GroupEntity(
                    cloudId = doc.cloud_id,
                    name = doc.name,
                    colorHex = doc.color_hex,
                    iconUri = null,
                    position = doc.position,
                    createdAt = System.currentTimeMillis()
                )
                val newLocalId = groupDao.insertGroup(group)
                cloudIdToLocalGroupId[doc.cloud_id] = newLocalId
            }

            groupDao.getAllGroupsOnce().forEach { g ->
                cloudIdToLocalGroupId[g.cloudId] = g.groupId
            }

            val remoteContainers = supabaseClient.postgrest.from("containers").select {
                filter { eq("user_email", email) }
            }.decodeList<ContainerRemote>()

            for (doc in remoteContainers) {
                if (doc.cloud_id in existingContainerCloudIds) continue

                val localGroupId = doc.group_cloud_id?.let { cloudIdToLocalGroupId[it] }

                val orientation = try {
                    OrientationMode.valueOf(doc.orientation_mode)
                } catch (e: Exception) {
                    OrientationMode.SYSTEM
                }

                val container = ContainerEntity(
                    cloudId = doc.cloud_id,
                    name = doc.name,
                    url = doc.url,
                    faviconUrl = doc.favicon_url,
                    groupId = localGroupId,
                    position = doc.position,
                    isDesktopMode = doc.is_desktop_mode,
                    orientationMode = orientation,
                    isKeepAliveEnabled = doc.is_keep_alive_enabled,
                    isFullscreenEnabled = doc.is_fullscreen_enabled,
                    isHttpAllowed = doc.is_http_allowed,
                    userAgentOverride = doc.user_agent_override,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                containerDao.insertContainer(container)
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "pullAndMergeAll failed", e)
            com.web.apps.core.crash.DebugLogger.log(appContext, "SupabaseSync", "pullAndMergeAll FAILED: ${e.message}")
        }
    }
}