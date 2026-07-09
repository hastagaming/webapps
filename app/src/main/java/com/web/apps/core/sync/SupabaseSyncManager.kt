package com.web.apps.core.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.web.apps.data.local.dao.ContainerDao
import com.web.apps.data.local.dao.GroupDao
import com.web.apps.data.local.entity.ContainerEntity
import com.web.apps.data.local.entity.GroupEntity
import com.web.apps.data.local.entity.OrientationMode
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseSyncManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val firebaseAuth: FirebaseAuth,
    private val containerDao: ContainerDao,
    private val groupDao: GroupDao
) {
    private val postgrest: Postgrest = supabaseClient.postgrest

    private fun currentUserEmail(): String? = firebaseAuth.currentUser?.email

    suspend fun pushGroup(group: GroupEntity) {
        val email = currentUserEmail() ?: return
        try {
            val data = mapOf(
                "user_email" to email,
                "cloud_id" to group.cloudId,
                "name" to group.name,
                "color_hex" to group.colorHex,
                "icon_uri" to group.iconUri,
                "position" to group.position,
                "created_at" to group.createdAt
            )

            val existing = postgrest
                .from("groups")
                .select(Columns.list("cloud_id"))
                .eq("user_email", email)
                .eq("cloud_id", group.cloudId)
                .decodeList<Map<String, Any>>()
                .isNotEmpty()

            if (existing) {
                postgrest.from("groups")
                    .update(data)
                    .eq("user_email", email)
                    .eq("cloud_id", group.cloudId)
                    .execute()
            } else {
                postgrest.from("groups").insert(data).execute()
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "pushGroup failed", e)
        }
    }

    suspend fun deleteGroupRemote(cloudId: String) {
        val email = currentUserEmail() ?: return
        try {
            postgrest.from("groups")
                .delete()
                .eq("user_email", email)
                .eq("cloud_id", cloudId)
                .execute()
        } catch (e: Exception) {
            Log.e("SupabaseSync", "deleteGroupRemote failed", e)
        }
    }

    suspend fun pushContainer(container: ContainerEntity) {
        val email = currentUserEmail() ?: return
        try {
            val groupCloudId = container.groupId?.let { groupDao.getGroupById(it)?.cloudId }

            val data = mapOf(
                "user_email" to email,
                "cloud_id" to container.cloudId,
                "group_cloud_id" to groupCloudId,
                "name" to container.name,
                "url" to container.url,
                "favicon_url" to container.faviconUrl,
                "position" to container.position,
                "is_desktop_mode" to container.isDesktopMode,
                "orientation_mode" to container.orientationMode.name,
                "is_keep_alive_enabled" to container.isKeepAliveEnabled,
                "is_fullscreen_enabled" to container.isFullscreenEnabled,
                "is_http_allowed" to container.isHttpAllowed,
                "user_agent_override" to container.userAgentOverride,
                "created_at" to container.createdAt,
                "updated_at" to container.updatedAt
            )

            val existing = postgrest
                .from("containers")
                .select(Columns.list("cloud_id"))
                .eq("user_email", email)
                .eq("cloud_id", container.cloudId)
                .decodeList<Map<String, Any>>()
                .isNotEmpty()

            if (existing) {
                postgrest.from("containers")
                    .update(data)
                    .eq("user_email", email)
                    .eq("cloud_id", container.cloudId)
                    .execute()
            } else {
                postgrest.from("containers").insert(data).execute()
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "pushContainer failed", e)
        }
    }

    suspend fun deleteContainerRemote(cloudId: String) {
        val email = currentUserEmail() ?: return
        try {
            postgrest.from("containers")
                .delete()
                .eq("user_email", email)
                .eq("cloud_id", cloudId)
                .execute()
        } catch (e: Exception) {
            Log.e("SupabaseSync", "deleteContainerRemote failed", e)
        }
    }

    suspend fun pullAndMergeAll() {
        val email = currentUserEmail() ?: return

        try {
            val existingGroupCloudIds = groupDao.getAllGroupsOnce().map { it.cloudId }.toSet()
            val existingContainerCloudIds = containerDao.getAllContainersOnce().map { it.cloudId }.toSet()

            val cloudIdToLocalGroupId = mutableMapOf<String, Long>()

            // Pull groups
            val remoteGroups = postgrest
                .from("groups")
                .select()
                .eq("user_email", email)
                .decodeList<Map<String, Any?>>()

            for (doc in remoteGroups) {
                val cloudId = doc["cloud_id"] as? String ?: continue
                if (cloudId in existingGroupCloudIds) continue

                val group = GroupEntity(
                    cloudId = cloudId,
                    name = doc["name"] as? String ?: "",
                    colorHex = doc["color_hex"] as? String ?: "#2196F3",
                    iconUri = null,
                    position = (doc["position"] as? Number)?.toInt() ?: 0,
                    createdAt = System.currentTimeMillis()
                )
                val newLocalId = groupDao.insertGroup(group)
                cloudIdToLocalGroupId[cloudId] = newLocalId
            }

            groupDao.getAllGroupsOnce().forEach { g ->
                cloudIdToLocalGroupId[g.cloudId] = g.groupId
            }

            // Pull containers
            val remoteContainers = postgrest
                .from("containers")
                .select()
                .eq("user_email", email)
                .decodeList<Map<String, Any?>>()

            for (doc in remoteContainers) {
                val cloudId = doc["cloud_id"] as? String ?: continue
                if (cloudId in existingContainerCloudIds) continue

                val groupCloudId = doc["group_cloud_id"] as? String
                val localGroupId = groupCloudId?.let { cloudIdToLocalGroupId[it] }

                val orientation = try {
                    OrientationMode.valueOf(doc["orientation_mode"] as? String ?: "SYSTEM")
                } catch (e: Exception) {
                    OrientationMode.SYSTEM
                }

                val container = ContainerEntity(
                    cloudId = cloudId,
                    name = doc["name"] as? String ?: "",
                    url = doc["url"] as? String ?: "",
                    faviconUrl = doc["favicon_url"] as? String,
                    groupId = localGroupId,
                    position = (doc["position"] as? Number)?.toInt() ?: 0,
                    isDesktopMode = doc["is_desktop_mode"] as? Boolean ?: false,
                    orientationMode = orientation,
                    isKeepAliveEnabled = doc["is_keep_alive_enabled"] as? Boolean ?: false,
                    isFullscreenEnabled = doc["is_fullscreen_enabled"] as? Boolean ?: false,
                    isHttpAllowed = doc["is_http_allowed"] as? Boolean ?: false,
                    userAgentOverride = doc["user_agent_override"] as? String,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                containerDao.insertContainer(container)
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "pullAndMergeAll failed", e)
        }
    }
}