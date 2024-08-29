package com.mercata.pingworks.db.contacts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mercata.pingworks.models.Person
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
data class DBContact(
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @PrimaryKey @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "imageUrl") val imageUrl: String?,
    @ColumnInfo(name = "receiveBroadcasts") val receiveBroadcasts: Boolean,
    @ColumnInfo(name = "signingKeyAlgorithm") val signingKeyAlgorithm: String,
    @ColumnInfo(name = "encryptionKeyAlgorithm") val encryptionKeyAlgorithm: String,
    @ColumnInfo(name = "publicEncryptionKey") val publicEncryptionKey: String,
    @ColumnInfo(name = "publicSigningKey") val publicSigningKey: String,

)

fun DBContact.toPerson(): Person {
    val instant = Instant.ofEpochMilli(this.timestamp)
    LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return Person(
        name = this.name,
        address = this.address,
        imageUrl = this.imageUrl,
        createdAt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()),
        receiveBroadcasts = this.receiveBroadcasts,
        publicSigningKey = this.publicSigningKey,
        publicEncryptionKey = this.publicEncryptionKey,
        signingKeyAlgorithm = this.signingKeyAlgorithm,
        encryptionKeyAlgorithm = this.encryptionKeyAlgorithm,
    )
}

fun Person.toDbContact(): DBContact {
    return DBContact(
        timestamp = this.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        address = this.address,
        name = this.name,
        imageUrl = this.imageUrl,
        receiveBroadcasts = this.receiveBroadcasts,
        publicSigningKey = this.publicSigningKey,
        publicEncryptionKey = this.publicEncryptionKey,
        signingKeyAlgorithm = this.signingKeyAlgorithm,
        encryptionKeyAlgorithm = this.encryptionKeyAlgorithm,
    )
}