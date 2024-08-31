package com.mercata.pingworks

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
import com.goterl.lazysodium.utils.Key
import com.mercata.pingworks.models.Address
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
            .putString(SP_PRIVATE_SIGNING_KEY, user.signingKeys.privateKey.toString())
            .putString(SP_PUBLIC_SIGNING_KEY, user.signingKeys.publicKey.toString())
            .putString(SP_PRIVATE_ENCRYPTION_KEY, user.encryptionKeys.privateKey.toString())
            .putString(SP_PUBLIC_ENCRYPTION_KEY, user.encryptionKeys.publicKey.toString())
            .apply()
    }

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
        val privateSigning: String =
            sharedPreferences.getString(SP_PRIVATE_SIGNING_KEY, null) ?: return null
        val publicSigning: String =
            sharedPreferences.getString(SP_PUBLIC_SIGNING_KEY, null) ?: return null
        val privateEncryption: String =
            sharedPreferences.getString(SP_PRIVATE_ENCRYPTION_KEY, null) ?: return null
        val publicEncryption: String =
            sharedPreferences.getString(SP_PUBLIC_ENCRYPTION_KEY, null) ?: return null
        val encryptionId: String =
            sharedPreferences.getString(SP_ENCRYPTION_KEY_ID, null) ?: return null

        return UserData(
            address = address,
            name = name,
            encryptionKeys = EncryptionKeys(
                privateKey = PrivateKey(Key.fromBase64String(privateEncryption)),
                publicKey = PublicKey(Key.fromBase64String(publicEncryption)),
                id = encryptionId
            ),
            signingKeys = SigningKeys(
                privateKey = PrivateKey(Key.fromBase64String(privateSigning)),
                publicKey = PublicKey(Key.fromBase64String(publicSigning))
            ),
        )
    }

    fun saveSelectedNavigationScreenName(screenName: String) {
        sharedPreferences.edit().putString(SP_SELECTED_NAV_SCREEN, screenName).apply()
    }

    fun getSelectedNavigationScreenName(): String =
        sharedPreferences.getString(SP_SELECTED_NAV_SCREEN, null) ?: "InboxListScreen"
}
