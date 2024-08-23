package com.mercata.pingworks

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
import com.goterl.lazysodium.utils.Key
import com.mercata.pingworks.registration.UserData

class SharedPreferences(applicationContext: Context) {

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "ping_works_sp",
        "ping_works_sp_alias",
        applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        PrefValueEncryptionScheme.AES256_GCM
    )


    fun saveUserPrivateKeys(user: UserData) {
        sharedPreferences.edit()
            .putString(SP_ADDRESS, user.address)
            .putString(SP_PRIVATE_SIGNING_KEY, user.signingKeys.privateKey.toString())
            .putString(SP_PRIVATE_ENCRYPTION_KEY, user.encryptionKeys.privateKey.toString())
            .apply()
    }

    fun getUserAddress(): String? = sharedPreferences.getString(SP_ADDRESS, null)

    fun setAutologin(autologin: Boolean) {
        sharedPreferences.edit().putBoolean(SP_AUTOLOGIN, autologin).apply()
    }

    fun isAutologin() = sharedPreferences.getBoolean(SP_AUTOLOGIN, false)

    fun setBiometry(biometry: Boolean) {
        sharedPreferences.edit().putBoolean(SP_BIOMETRY, biometry).apply()
    }

    fun isBiometry() = sharedPreferences.getBoolean(SP_BIOMETRY, true)

    fun getUserPrivateKeys(): UserPrivateKeys? {
        val address: String = getUserAddress() ?: return null
        val signing: String =
            sharedPreferences.getString(SP_PRIVATE_SIGNING_KEY, null) ?: return null
        val encryption: String =
            sharedPreferences.getString(SP_PRIVATE_ENCRYPTION_KEY, null) ?: return null

        return UserPrivateKeys(
            address = address,
            privateSigningKey = PrivateKey(Key.fromBase64String(signing)),
            privateEncryptionKey = PrivateKey(Key.fromBase64String(encryption))
        )
    }
}

data class UserPrivateKeys(
    val address: String,
    val privateSigningKey: PrivateKey,
    val privateEncryptionKey: PrivateKey
)