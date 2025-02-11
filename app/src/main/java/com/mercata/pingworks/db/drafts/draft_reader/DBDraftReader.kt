package com.mercata.pingworks.db.drafts.draft_reader

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.mercata.pingworks.db.drafts.DBDraft
import com.mercata.pingworks.models.PublicUserData
import java.time.Instant

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = DBDraft::class,
            parentColumns = ["draft_id"],
            childColumns = ["draft_id"],
            onDelete = ForeignKey.CASCADE // Cascade delete
        )
    ]
)
data class DBDraftReader(
    @ColumnInfo(name = "draft_id") val draftId: String,
    @ColumnInfo(name = "address") @PrimaryKey val address: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    @ColumnInfo(name = "last_seen_public") val lastSeenPublic: Boolean,
    @ColumnInfo(name = "last_seen") val lastSeenTimestamp: Long?,
    @ColumnInfo(name = "updated") val updatedTimestamp: Long?,
    @ColumnInfo(name = "encryption_key_id") val encryptionKeyId: String,
    @ColumnInfo(name = "encryption_algorithm") val encryptionKeyAlgorithm: String,
    @ColumnInfo(name = "signing_key_algorithm") val signingKeyAlgorithm: String,
    @ColumnInfo(name = "public_encryption_key") val publicEncryptionKey: String,
    @ColumnInfo(name = "public_signing_key") val publicSigningKey: String,
    @ColumnInfo(name = "last_signing_key") val lastSigningKey: String?,
    @ColumnInfo(name = "last_signing_key_algorithm") val lastSigningKeyAlgorithm: String?,
    @ColumnInfo(name = "public_access") val publicAccess: Boolean?,
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
)

fun DBDraftReader.toPublicUserData() = PublicUserData(
    fullName = fullName,
    address = address,
    lastSeenPublic = lastSeenPublic,
    lastSeen = lastSeenTimestamp?.let { Instant.ofEpochMilli(it) },
    updated = updatedTimestamp?.let { Instant.ofEpochMilli(it) },
    encryptionKeyId = encryptionKeyId,
    encryptionKeyAlgorithm = encryptionKeyAlgorithm,
    signingKeyAlgorithm = signingKeyAlgorithm,
    publicEncryptionKey = publicEncryptionKey,
    publicSigningKey = publicSigningKey,
    lastSigningKey = lastSigningKey,
    lastSigningKeyAlgorithm = lastSigningKeyAlgorithm,
    publicAccess = publicAccess,
    away = away,
    awayWarning = awayWarning,
    status = status,
    about = about,
    gender = gender,
    language = language,
    relationshipStatus = relationshipStatus,
    education = education,
    placesLived = placesLived,
    notes = notes,
    work = work,
    department = department,
    organization = organization,
    jobTitle = jobTitle,
    interests = interests,
    books = books,
    music = music,
    movies = movies,
    sports = sports,
    website = website,
    mailingAddress = mailingAddress,
    location = location,
    phone = phone,
    streams = streams
)