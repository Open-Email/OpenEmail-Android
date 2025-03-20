package com.mercata.openemail.db.notifications

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.mercata.openemail.db.contacts.ContactItem
import com.mercata.openemail.db.contacts.DBContact
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.utils.Address
import java.time.Instant

const val SEVEN_DAYS_MILLIS = 1000 * 60 * 60 * 24 * 7

@Entity
(
    foreignKeys = [
        ForeignKey(
            entity = DBContact::class,
            parentColumns = ["address"],
            childColumns = ["address"],
            onDelete = ForeignKey.CASCADE // Cascade delete
        )
    ]
)
data class DBNotification(
    @PrimaryKey @ColumnInfo("notification_id") val notificationId: String,
    @ColumnInfo("received_on_timestamp") val receivedOnTimestamp: Long,
    @ColumnInfo("link") val link: String,
    @ColumnInfo("full_name") override val name: String,
    @ColumnInfo("address") override val address: Address,
    @ColumnInfo("dismissed") val dismissed: Boolean,
    @ColumnInfo("last_seen_public") val lastSeenPublic: Boolean,
    @ColumnInfo("last_seen") val lastSeen: Long?,
    @ColumnInfo("updated") val updated: Long?,
    @ColumnInfo("encryption_key_id") val encryptionKeyId: String,
    @ColumnInfo("encryption_key_algorithm") val encryptionKeyAlgorithm: String,
    @ColumnInfo("signing_key_algorithm") val signingKeyAlgorithm: String,
    @ColumnInfo("public_encryption_key") val publicEncryptionKey: String,
    @ColumnInfo("public_signing_key") val publicSigningKey: String,
    @ColumnInfo("last_signing_key") val lastSigningKey: String?,
    @ColumnInfo("last_signing_key_algorithm") val lastSigningKeyAlgorithm: String?,
    @ColumnInfo("public_access") val publicAccess: Boolean?,
    @ColumnInfo("public_links") val publicLinks: Boolean?,
    @ColumnInfo("away") val away: Boolean?,
    @ColumnInfo("away_warning") val awayWarning: String?,
    @ColumnInfo("status") val status: String?,
    @ColumnInfo("about") val about: String?,
    @ColumnInfo("gender") val gender: String?,
    @ColumnInfo("language") val language: String?,
    @ColumnInfo("relationship_status") val relationshipStatus: String?,
    @ColumnInfo("education") val education: String?,
    @ColumnInfo("places_lived") val placesLived: String?,
    @ColumnInfo("notes") val notes: String?,
    @ColumnInfo("work") val work: String?,
    @ColumnInfo("department") val department: String?,
    @ColumnInfo("organization") val organization: String?,
    @ColumnInfo("job_title") val jobTitle: String?,
    @ColumnInfo("interests") val interests: String?,
    @ColumnInfo("books") val books: String?,
    @ColumnInfo("music") val music: String?,
    @ColumnInfo("movies") val movies: String?,
    @ColumnInfo("sports") val sports: String?,
    @ColumnInfo("website") val website: String?,
    @ColumnInfo("mailing_address") val mailingAddress: String?,
    @ColumnInfo("location") val location: String?,
    @ColumnInfo("phone") val phone: String?,
    @ColumnInfo("streams") val streams: String?
) : ContactItem {

    fun isExpired(): Boolean {
        val currentTimestamp = System.currentTimeMillis()
        return currentTimestamp - SEVEN_DAYS_MILLIS > receivedOnTimestamp
    }

    override suspend fun getContacts(): List<PublicUserData> = listOf()

    override suspend fun getTitle(): String = name

    override fun getAddressValue(): String = address

    override fun getSubtitle(): String? = null

    override fun getTextBody(): String = address

    override fun getMessageId(): String = "${DBNotification::class} " + notificationId

    override fun getAttachmentsAmount(): Int? = null

    override fun isUnread(): Boolean = false

    override fun getTimestamp(): Long? = null
}

fun DBNotification.toPublicUserData(): PublicUserData {
    return PublicUserData(
        fullName = this.name,
        address = this.address,
        lastSeenPublic = this.lastSeenPublic,
        lastSeen = this.lastSeen?.let { Instant.ofEpochMilli(it) },
        updated = this.updated?.let { Instant.ofEpochMilli(it) },
        encryptionKeyId = this.encryptionKeyId,
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
}
