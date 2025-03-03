package com.mercata.pingworks.models

import com.mercata.pingworks.db.contacts.DBContact
import com.mercata.pingworks.db.drafts.draft_reader.DBDraftReader
import com.mercata.pingworks.db.notifications.DBNotification
import com.mercata.pingworks.db.pending.readers.DBPendingReaderPublicData
import com.mercata.pingworks.utils.Address
import java.time.Instant
import java.util.UUID


data class PublicUserData(
    val fullName: String,
    val address: Address,
    val lastSeenPublic: Boolean,
    val lastSeen: Instant?,
    val updated: Instant?,
    val encryptionKeyId: String,
    val encryptionKeyAlgorithm: String,
    val signingKeyAlgorithm: String,
    val publicEncryptionKey: String,
    val publicSigningKey: String,
    val lastSigningKey: String?,
    val lastSigningKeyAlgorithm: String?,

    val publicAccess: Boolean?,
    val away: Boolean?,
    val publicLinks: Boolean?,
    val awayWarning: String?,
    val status: String?,
    val about: String?,
    val gender: String?,
    val language: String?,
    val relationshipStatus: String?,
    val education: String?,
    val placesLived: String?,
    val notes: String?,
    val work: String?,
    val department: String?,
    val organization: String?,
    val jobTitle: String?,
    val interests: String?,
    val books: String?,
    val music: String?,
    val movies: String?,
    val sports: String?,
    val website: String?,
    val mailingAddress: String?,
    val location: String?,
    val phone: String?,
    val streams: String?,
)

fun PublicUserData.toDBContact() = DBContact(
    address = this.address,
    lastSeenPublic = this.lastSeenPublic,
    encryptionKeyAlgorithm = this.encryptionKeyAlgorithm,
    signingKeyAlgorithm = this.signingKeyAlgorithm,
    publicEncryptionKey = this.publicEncryptionKey,
    publicSigningKey = this.publicSigningKey,
    lastSigningKey = this.lastSigningKey,
    lastSigningKeyAlgorithm = this.lastSigningKeyAlgorithm,
    updated = this.updated?.toString(),
    lastSeen = this.lastSeen?.toString(),
    name = this.fullName,
    receiveBroadcasts = true,
    publicEncryptionKeyId = this.encryptionKeyId,
    uploaded = true,
    markedToDelete = false,
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

fun PublicUserData.toDBPendingReaderPublicData(messageId: String) = DBPendingReaderPublicData(
    uuid = UUID.randomUUID().toString(),
    fullName = this.fullName,
    address = this.address,
    lastSeenPublic = this.lastSeenPublic,
    lastSeenTimestamp = this.lastSeen?.toEpochMilli(),
    updatedTimestamp = this.updated?.toEpochMilli(),
    encryptionKeyId = this.encryptionKeyId,
    encryptionKeyAlgorithm = this.encryptionKeyAlgorithm,
    signingKeyAlgorithm = this.signingKeyAlgorithm,
    publicEncryptionKey = this.publicEncryptionKey,
    publicSigningKey = this.publicSigningKey,
    lastSigningKey = this.lastSigningKey,
    lastSigningKeyAlgorithm = this.lastSigningKeyAlgorithm,
    messageId = messageId,
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

fun PublicUserData.toDBDraftReader(draftId: String) = DBDraftReader(
    fullName = fullName,
    address = address,
    lastSeenPublic = lastSeenPublic,
    lastSeenTimestamp = lastSeen?.toEpochMilli(),
    updatedTimestamp = updated?.toEpochMilli(),
    encryptionKeyId = encryptionKeyId,
    encryptionKeyAlgorithm = encryptionKeyAlgorithm,
    signingKeyAlgorithm = signingKeyAlgorithm,
    publicEncryptionKey = publicEncryptionKey,
    publicSigningKey = publicSigningKey,
    lastSigningKey = lastSigningKey,
    lastSigningKeyAlgorithm = lastSigningKeyAlgorithm,
    draftId = draftId,
    publicAccess = publicAccess,
    publicLinks = publicLinks,
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

fun PublicUserData.toDBNotification(id: String, link: String) = DBNotification(
    notificationId = id,
    receivedOnTimestamp = System.currentTimeMillis(),
    link = link,
    name = this.fullName,
    address = this.address,
    dismissed = false,
    lastSeenPublic = this.lastSeenPublic,
    lastSeen = this.lastSeen?.toEpochMilli(),
    updated = this.updated?.toEpochMilli(),
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