package com.mercata.openemail.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
import com.goterl.lazysodium.utils.Key
import com.goterl.lazysodium.utils.KeyPair
import com.mercata.openemail.SP_ADDRESS
import com.mercata.openemail.SP_AUTOLOGIN
import com.mercata.openemail.SP_BIOMETRY
import com.mercata.openemail.SP_ENCRYPTION_KEYS
import com.mercata.openemail.SP_ENCRYPTION_KEY_ID
import com.mercata.openemail.SP_FIRST_TIME
import com.mercata.openemail.SP_FULL_NAME
import com.mercata.openemail.SP_SELECTED_NAV_SCREEN
import com.mercata.openemail.SP_SIGNING_KEYS
import com.mercata.openemail.db.AppDatabase
import com.mercata.openemail.home_screen.HomeScreen
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.models.toDBContact
import com.mercata.openemail.registration.UserData
import androidx.core.content.edit

class SharedPreferences(applicationContext: Context, val db: AppDatabase) {

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "ping_works_sp",
        "ping_works_sp_alias",
        applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        PrefValueEncryptionScheme.AES256_GCM
    )

    suspend fun saveUserKeys(user: UserData) {
        sharedPreferences.edit {
            putString(SP_ADDRESS, user.address.trim())
                .putString(SP_FULL_NAME, user.name.trim())
                .putString(SP_ENCRYPTION_KEY_ID, user.encryptionKeys.id.trim())
                .putString(
                    SP_ENCRYPTION_KEYS,
                    arrayOf(
                        user.encryptionKeys.pair.publicKey,
                        user.encryptionKeys.pair.secretKey
                    ).joinToString(separator = ",") { it.asBytes.encodeToBase64().trim() }
                )
                .putString(
                    SP_SIGNING_KEYS, arrayOf(
                        user.signingKeys.pair.publicKey,
                        user.signingKeys.pair.secretKey
                    ).joinToString(separator = ",") { it.asBytes.encodeToBase64().trim() })
        }

        val publicUserData: PublicUserData? =
            when (val call = safeApiCall { getProfilePublicData(user.address) }) {
                is HttpResult.Error -> null
                is HttpResult.Success -> call.data
            }

        publicUserData?.let { db.userDao().insert(it.toDBContact()) }
    }

    fun getUserAddress(): Address? = sharedPreferences.getString(SP_ADDRESS, null)

    fun setAutologin(autologin: Boolean) {
        sharedPreferences.edit { putBoolean(SP_AUTOLOGIN, autologin) }
    }

    fun isAutologin() = sharedPreferences.getBoolean(SP_AUTOLOGIN, false)

    fun setBiometry(biometry: Boolean) {
        sharedPreferences.edit { putBoolean(SP_BIOMETRY, biometry) }
    }

    fun clear() {
        sharedPreferences.edit { clear() }
    }

    fun isFirstTime() = sharedPreferences.getBoolean(SP_FIRST_TIME, true)
    fun setFirstTime(isFirstTime: Boolean) =
        sharedPreferences.edit { putBoolean(SP_FIRST_TIME, isFirstTime) }

    fun isBiometry() = sharedPreferences.getBoolean(SP_BIOMETRY, false)

    fun getUserData(): UserData? {
        val address: String = getUserAddress() ?: return null
        val name: String = sharedPreferences.getString(SP_FULL_NAME, null) ?: return null
        val signingKeys = getSigningKeys() ?: return null
        val encryptionKeys = getEncryptionKeys() ?: return null

        return UserData(
            address = address,
            name = name,
            encryptionKeys = encryptionKeys,
            signingKeys = signingKeys,
        )
    }

    private fun getSigningKeys(): SigningKeys? {
        val publicPrivateSigning: String =
            sharedPreferences.getString(SP_SIGNING_KEYS, null) ?: return null
        val signingSplit = publicPrivateSigning.split(",")
        return SigningKeys(
            KeyPair(
                Key.fromBytes(signingSplit.first().decodeFromBase64()),
                Key.fromBytes(signingSplit.last().decodeFromBase64())
            )
        )
    }

    private fun getEncryptionKeys(): EncryptionKeys? {
        val publicPrivateEncryption: String =
            sharedPreferences.getString(SP_ENCRYPTION_KEYS, null) ?: return null
        val encryptionSplit = publicPrivateEncryption.split(",")
        val encryptionId: String =
            sharedPreferences.getString(SP_ENCRYPTION_KEY_ID, null) ?: return null
        return EncryptionKeys(
            KeyPair(
                Key.fromBytes(encryptionSplit.first().decodeFromBase64()),
                Key.fromBytes(encryptionSplit.last().decodeFromBase64())
            ), id = encryptionId
        )
    }

    fun saveSelectedNavigationScreen(screen: HomeScreen) {
        sharedPreferences.edit { putString(SP_SELECTED_NAV_SCREEN, screen.name) }
    }

    fun getSelectedNavigationScreen(): HomeScreen =
        HomeScreen.entries.first { screen ->
            screen.name == sharedPreferences.getString(
                SP_SELECTED_NAV_SCREEN,
                HomeScreen.Inbox.name
            )
        }
}
