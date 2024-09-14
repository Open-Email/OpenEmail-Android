package com.mercata.pingworks.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
import com.goterl.lazysodium.utils.Key
import com.goterl.lazysodium.utils.KeyPair
import com.mercata.pingworks.SP_ADDRESS
import com.mercata.pingworks.SP_AUTOLOGIN
import com.mercata.pingworks.SP_AVATAR_LINK
import com.mercata.pingworks.SP_BIOMETRY
import com.mercata.pingworks.SP_ENCRYPTION_KEYS
import com.mercata.pingworks.SP_ENCRYPTION_KEY_ID
import com.mercata.pingworks.SP_FULL_NAME
import com.mercata.pingworks.SP_SELECTED_NAV_SCREEN
import com.mercata.pingworks.SP_SIGNING_KEYS
import com.mercata.pingworks.home_screen.HomeScreen
import com.mercata.pingworks.registration.UserData

class SharedPreferences(applicationContext: Context) {

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "ping_works_sp",
        "ping_works_sp_alias",
        applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveUserKeys(user: UserData) {
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

    fun isBiometry() = sharedPreferences.getBoolean(SP_BIOMETRY, false)

    fun getUserData(): UserData? {
        val address: String = getUserAddress() ?: return null
        val name: String = sharedPreferences.getString(SP_FULL_NAME, null) ?: return null
        val publicPrivateSigning: String =
            sharedPreferences.getString(SP_SIGNING_KEYS, null) ?: return null
        val publicPrivateEncryption: String =
            sharedPreferences.getString(SP_ENCRYPTION_KEYS, null) ?: return null
        val encryptionId: String =
            sharedPreferences.getString(SP_ENCRYPTION_KEY_ID, null) ?: return null

        val signingSplit = publicPrivateSigning.split(",")
        val encryptionSplit = publicPrivateEncryption.split(",")
        val signingKeys = SigningKeys(
            KeyPair(
                Key.fromBase64String(signingSplit.first()),
                Key.fromBase64String(signingSplit.last())
            )
        )
        val encryptionKeys = EncryptionKeys(
            KeyPair(
                Key.fromBase64String(encryptionSplit.first()),
                Key.fromBase64String(encryptionSplit.last())
            ), id = encryptionId
        )
        return UserData(
            address = address,
            name = name,
            encryptionKeys = encryptionKeys,
            signingKeys = signingKeys,
            avatarLink = getUserAvatarLink()
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
