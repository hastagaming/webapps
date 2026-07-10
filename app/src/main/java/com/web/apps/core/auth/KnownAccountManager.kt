package com.web.apps.core.auth

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.web.apps.core.sync.KnownAccountRemote
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

enum class KnownAccountType {
    GOOGLE, EMAIL_PASSWORD
}

@Serializable
data class KnownAccount(
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val type: KnownAccountType
)

private val Context.knownAccountsDataStore by preferencesDataStore(name = "known_accounts")

@Singleton
class KnownAccountManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient
) {
    private val ACCOUNTS_KEY = stringPreferencesKey("accounts_json")
    private val json = Json { ignoreUnknownKeys = true }

    private val deviceId: String
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"

    val knownAccounts: Flow<List<KnownAccount>> = context.knownAccountsDataStore.data.map { prefs ->
        val raw = prefs[ACCOUNTS_KEY] ?: return@map emptyList()
        try {
            json.decodeFromString<List<KnownAccount>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveAccount(account: KnownAccount) {
        context.knownAccountsDataStore.edit { prefs ->
            val existingRaw = prefs[ACCOUNTS_KEY]
            val existing = if (existingRaw != null) {
                try {
                    json.decodeFromString<List<KnownAccount>>(existingRaw)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            val updated = existing.filterNot { it.email.equals(account.email, ignoreCase = true) } + account
            prefs[ACCOUNTS_KEY] = json.encodeToString(updated)
        }

        pushAccountToSupabase(account)
    }

    suspend fun removeAccount(email: String) {
        context.knownAccountsDataStore.edit { prefs ->
            val existingRaw = prefs[ACCOUNTS_KEY] ?: return@edit
            val existing = try {
                json.decodeFromString<List<KnownAccount>>(existingRaw)
            } catch (e: Exception) {
                emptyList()
            }
            val updated = existing.filterNot { it.email.equals(email, ignoreCase = true) }
            prefs[ACCOUNTS_KEY] = json.encodeToString(updated)
        }

        try {
            supabaseClient.postgrest.from("known_accounts").delete {
                filter {
                    eq("device_id", deviceId)
                    eq("email", email)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    suspend fun pullKnownAccountsFromSupabase() {
        try {
            val remoteAccounts = supabaseClient.postgrest.from("known_accounts")
                .select {
                    filter { eq("device_id", deviceId) }
                }
                .decodeList<KnownAccountRemote>()

            val localAccounts = knownAccounts.first()
            val localEmails = localAccounts.map { it.email.lowercase() }.toSet()

            val toAdd = remoteAccounts
                .filter { it.email.lowercase() !in localEmails }
                .map {
                    KnownAccount(
                        email = it.email,
                        displayName = it.display_name,
                        photoUrl = it.photo_url,
                        type = try {
                            KnownAccountType.valueOf(it.account_type)
                        } catch (e: Exception) {
                            KnownAccountType.EMAIL_PASSWORD
                        }
                    )
                }

            if (toAdd.isNotEmpty()) {
                context.knownAccountsDataStore.edit { prefs ->
                    val merged = localAccounts + toAdd
                    prefs[ACCOUNTS_KEY] = json.encodeToString(merged)
                }
            }
        } catch (e: Exception) {
            // gagal sync tidak boleh crash; tetap pakai data lokal
        }
    }

    private suspend fun pushAccountToSupabase(account: KnownAccount) {
        try {
            val remote = KnownAccountRemote(
                device_id = deviceId,
                email = account.email,
                display_name = account.displayName,
                photo_url = account.photoUrl,
                account_type = account.type.name
            )
            supabaseClient.postgrest.from("known_accounts").upsert(remote, onConflict = "device_id,email")
        } catch (e: Exception) {
            // gagal push tidak boleh crash
        }
    }
}