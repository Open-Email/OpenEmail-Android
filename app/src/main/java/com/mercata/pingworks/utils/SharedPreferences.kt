package com.mercata.pingworks.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
import com.goterl.lazysodium.utils.Key
import com.goterl.lazysodium.utils.KeyPair
import com.mercata.pingworks.ANONYMOUS_ENCRYPTION_CIPHER
import com.mercata.pingworks.SIGNING_ALGORITHM
import com.mercata.pingworks.SP_ADDRESS
import com.mercata.pingworks.SP_AUTOLOGIN
import com.mercata.pingworks.SP_AVATAR_LINK
import com.mercata.pingworks.SP_BIOMETRY
import com.mercata.pingworks.SP_ENCRYPTION_KEYS
import com.mercata.pingworks.SP_ENCRYPTION_KEY_ID
import com.mercata.pingworks.SP_FIRST_TIME
import com.mercata.pingworks.SP_FULL_NAME
import com.mercata.pingworks.SP_SELECTED_NAV_SCREEN
import com.mercata.pingworks.SP_SIGNING_KEYS
import com.mercata.pingworks.db.AppDatabase
import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.home_screen.HomeScreen
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.registration.UserData
import java.time.Instant

class SharedPreferences(applicationContext: Context, val db: AppDatabase) {

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "ping_works_sp",
        "ping_works_sp_alias",
        applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        PrefValueEncryptionScheme.AES256_GCM
    )

    suspend fun saveUserKeys(user: UserData) {
        sharedPreferences.edit()
            .putString(SP_ADDRESS, user.address)
            .putString(SP_FULL_NAME, user.name)
            .putString(SP_ENCRYPTION_KEY_ID, user.encryptionKeys.id)
            .putString(
                SP_ENCRYPTION_KEYS,
                arrayOf(
                    user.encryptionKeys.pair.publicKey,
                    user.encryptionKeys.pair.secretKey
                ).joinToString(separator = ",") { it.asBytes.encodeToBase64() }
            )
            .putString(
                SP_SIGNING_KEYS, arrayOf(
                    user.signingKeys.pair.publicKey,
                    user.signingKeys.pair.secretKey
                ).joinToString(separator = ",") { it.asBytes.encodeToBase64() })
            .apply()

        db.userDao().insert(
            DBContact(
                updated = null,
                lastSeen = null,
                address = user.address,
                name = user.name,
                //TODO
                imageUrl = null,
                lastSeenPublic = true,
                receiveBroadcasts = true,
                signingKeyAlgorithm = SIGNING_ALGORITHM,
                encryptionKeyAlgorithm = ANONYMOUS_ENCRYPTION_CIPHER,
                publicEncryptionKey = user.encryptionKeys.pair.publicKey.asBytes.encodeToBase64(),
                publicEncryptionKeyId = user.encryptionKeys.id,
                publicSigningKey = user.signingKeys.pair.publicKey.asBytes.encodeToBase64(),
                markedToDelete = false,
                uploaded = true
            )
        )
    }

    fun saveUserAvatarLink(link: String) =
        sharedPreferences.edit().putString(SP_AVATAR_LINK, link).apply()

    fun getUserAvatarLink() = sharedPreferences.getString(SP_AVATAR_LINK, null)

    fun getUserAddress(): Address? = sharedPreferences.getString(SP_ADDRESS, null)

    fun setAutologin(autologin: Boolean) {
        sharedPreferences.edit().putBoolean(SP_AUTOLOGIN, autologin).apply()
    }

    fun isAutologin() = sharedPreferences.getBoolean(SP_AUTOLOGIN, false)

    fun setBiometry(biometry: Boolean) {
        sharedPreferences.edit().putBoolean(SP_BIOMETRY, biometry).apply()
    }

    fun isFirstTime() = sharedPreferences.getBoolean(SP_FIRST_TIME, true)
    fun setFirstTime(isFirstTime: Boolean) = sharedPreferences.edit().putBoolean(SP_FIRST_TIME, isFirstTime).apply()

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
            avatarLink = getUserAvatarLink()
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

    fun getPublicUserData(): PublicUserData? {
        val address: String = getUserAddress() ?: return null
        val name: String = sharedPreferences.getString(SP_FULL_NAME, null) ?: return null
        val signingKeys = getSigningKeys() ?: return null
        val encryptionKeys = getEncryptionKeys() ?: return null

        return PublicUserData(
            fullName = name,
            address = address,
            lastSeenPublic = true,
            lastSeen = Instant.now(),
            updated = Instant.now(),
            encryptionKeyId = encryptionKeys.id,
            encryptionKeyAlgorithm = ANONYMOUS_ENCRYPTION_CIPHER,
            signingKeyAlgorithm = SIGNING_ALGORITHM,
            publicEncryptionKey = encryptionKeys.pair.publicKey.asBytes.encodeToBase64(),
            publicSigningKey = signingKeys.pair.publicKey.asBytes.encodeToBase64(),
            imageUrl = null
        )
    }

    fun saveSelectedNavigationScreen(screen: HomeScreen) {
        sharedPreferences.edit().putString(SP_SELECTED_NAV_SCREEN, screen.name).apply()
    }

    fun getSelectedNavigationScreen(): HomeScreen =
        HomeScreen.entries.first { screen ->
            screen.name == sharedPreferences.getString(
                SP_SELECTED_NAV_SCREEN,
                HomeScreen.Inbox.name
            )
        }
}
