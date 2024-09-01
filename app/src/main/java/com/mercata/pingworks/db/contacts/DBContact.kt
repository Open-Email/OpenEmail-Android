package com.mercata.pingworks.db.contacts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DBContact(
    @ColumnInfo(name = "updated") val updated: String?,
    @ColumnInfo(name = "lastSeen") val lastSeen: String?,
    @PrimaryKey @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "imageUrl") val imageUrl: String?,
    @ColumnInfo(name = "receiveBroadcasts") val receiveBroadcasts: Boolean,
    @ColumnInfo(name = "signingKeyAlgorithm") val signingKeyAlgorithm: String,
    @ColumnInfo(name = "encryptionKeyAlgorithm") val encryptionKeyAlgorithm: String,
    @ColumnInfo(name = "publicEncryptionKey") val publicEncryptionKey: String,
    @ColumnInfo(name = "publicSigningKey") val publicSigningKey: String,

    )
