package com.mercata.openemail.db.contacts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mercata.openemail.db.HomeItem
import com.mercata.openemail.models.PublicUserData
import java.time.Instant

@Entity
data class DBContact(
    @ColumnInfo(name = "updated") val updated: String?,
    @ColumnInfo(name = "uploaded") val uploaded: Boolean,
    @ColumnInfo(name = "marked_to_delete") val markedToDelete: Boolean,
    @ColumnInfo(name = "last_seen") val lastSeen: String?,
    @PrimaryKey @ColumnInfo(name = "address") override val address: String,
    @ColumnInfo(name = "name") override val name: String?,
    @ColumnInfo(name = "last_seen_public") val lastSeenPublic: Boolean,
    @ColumnInfo(name = "receive_broadcasts") val receiveBroadcasts: Boolean,
    @ColumnInfo(name = "signing_key_algorithm") val signingKeyAlgorithm: String,
    @ColumnInfo(name = "encryption_key_algorithm") val encryptionKeyAlgorithm: String,
    @ColumnInfo(name = "public_encryption_key") val publicEncryptionKey: String,
    @ColumnInfo(name = "public_encryption_key_id") val publicEncryptionKeyId: String,
    @ColumnInfo(name = "public_signing_key") val publicSigningKey: String,
    @ColumnInfo(name = "last_signing_key") val lastSigningKey: String?,
    @ColumnInfo(name = "last_signing_key_algorithm") val lastSigningKeyAlgorithm: String?,
    @ColumnInfo(name = "public_access") val publicAccess: Boolean?,
    @ColumnInfo(name = "public_links") val publicLinks: Boolean?,
    @ColumnInfo(name = "away") val away: Boolean?,
    @ColumnInfo(name = "away_warning") val awayWarning: String?,
    @ColumnInfo(name = "status") val status: String?,
    @ColumnInfo(name = "about") val about: String?,
    @ColumnInfo(name = "gender") val gender: String?,
    @ColumnInfo(name = "language") val language: String?,
    @ColumnInfo(name = "relationship_status") val relationshipStatus: String?,
    @ColumnInfo(name = "education") val education: String?,
    @ColumnInfo(name = "places_lived") val placesLived: String?,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "work") val work: String?,
    @ColumnInfo(name = "department") val department: String?,
    @ColumnInfo(name = "organization") val organization: String?,
    @ColumnInfo(name = "job_title") val jobTitle: String?,
    @ColumnInfo(name = "interests") val interests: String?,
    @ColumnInfo(name = "books") val books: String?,
    @ColumnInfo(name = "music") val music: String?,
    @ColumnInfo(name = "movies") val movies: String?,
    @ColumnInfo(name = "sports") val sports: String?,
    @ColumnInfo(name = "website") val website: String?,
    @ColumnInfo(name = "mailing_address") val mailingAddress: String?,
    @ColumnInfo(name = "location") val location: String?,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "streams") val streams: String?
) : ContactItem {

    override suspend fun getContacts(): List<PublicUserData> = listOf()

    override fun getAddressValue(): String = address

    override suspend fun getTitle(): String = name ?: ""

    override fun getSubtitle(): String? = null

    override fun getTextBody(): String = address

    override fun getMessageId(): String = "${DBContact::class} " + address

    override fun getAttachmentsAmount(): Int? = null

    override fun isUnread(): Boolean = false

    override fun getTimestamp(): Long? = null
}

fun DBContact.toPublicUserData(): PublicUserData =
    PublicUserData(
        fullName = this.name ?: "",
        address = this.address,
        lastSeenPublic = this.lastSeenPublic,
        lastSeen = this.lastSeen?.let { Instant.parse(it) },
        updated = this.lastSeen?.let { Instant.parse(it) },
        encryptionKeyId = this.publicEncryptionKeyId,
        encryptionKeyAlgorithm = this.encryptionKeyAlgorithm,
        signingKeyAlgorithm = this.signingKeyAlgorithm,
        publicEncryptionKey = this.publicEncryptionKey,
        publicSigningKey = this.publicSigningKey,
        lastSigningKey = this.lastSigningKey,
        lastSigningKeyAlgorithm = this.lastSigningKeyAlgorithm,
        publicAccess = this.publicAccess,
        publicLinks = this.publicLinks,
        away = this.away,
        awayWarning = this.awayWarning,
        status = this.status,
        about = this.about,
        gender = this.gender,
        language = this.language,
        relationshipStatus = this.relationshipStatus,
        education = this.education,
        placesLived = this.placesLived,
        notes = this.notes,
        work = this.work,
        department = this.department,
        organization = this.organization,
        jobTitle = this.jobTitle,
        interests = this.interests,
        books = this.books,
        music = this.music,
        movies = this.movies,
        sports = this.sports,
        website = this.website,
        mailingAddress = this.mailingAddress,
        location = this.location,
        phone = this.phone,
        streams = this.streams
    )

interface ContactItem : HomeItem {
    val name: String?
    val address: String
}