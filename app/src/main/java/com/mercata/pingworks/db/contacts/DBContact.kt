package com.mercata.pingworks.db.contacts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DBContact(
    @ColumnInfo(name = "updated") val updated: String?,
    @ColumnInfo(name = "last_seen") val lastSeen: String?,
    @PrimaryKey @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "receive_broadcasts") val receiveBroadcasts: Boolean,
    @ColumnInfo(name = "signing_key_algorithm") val signingKeyAlgorithm: String,
    @ColumnInfo(name = "encryption_key_algorithm") val encryptionKeyAlgorithm: String,
    @ColumnInfo(name = "public_encryption_key") val publicEncryptionKey: String,
    @ColumnInfo(name = "public_signing_key") val publicSigningKey: String)
