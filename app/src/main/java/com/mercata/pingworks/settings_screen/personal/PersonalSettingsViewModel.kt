package com.mercata.pingworks.settings_screen.personal

import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.registration.UserData
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.getProfilePublicData
import com.mercata.pingworks.utils.safeApiCall
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class PersonalSettingsViewModel :
    AbstractViewModel<PersonalSettingsState>(PersonalSettingsState()) {
    init {
        val sp: SharedPreferences by inject()

        viewModelScope.launch {
            updateState(
                currentState.copy(
                    localData = sp.getUserData()!!,
                    loading = true
                )
            )
            when (val call = safeApiCall { getProfilePublicData(sp.getUserAddress()!!) }) {
                is HttpResult.Error -> {
                    //ignore
                }

                is HttpResult.Success -> {
                    call.data?.let {
                        updateState(currentState.copy(data = it, tmpData = it.copy()))
                    }
                }
            }
            updateState(currentState.copy(loading = false))
        }
    }

    fun onStatusChange(status: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(status = status.takeIf { it.isNotBlank() })))
    }

    fun onAboutChange(about: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(about = about.takeIf { it.isNotBlank() })))
    }

    fun onGenderChange(gender: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(gender = gender.takeIf { it.isNotBlank() })))
    }

    fun onLanguageChange(language: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(language = language.takeIf { it.isNotBlank() })))
    }

    fun onRelationshipStatusChange(relationshipStatus: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(relationshipStatus = relationshipStatus.takeIf { it.isNotBlank() })))
    }

    fun onEducationChange(education: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(education = education.takeIf { it.isNotBlank() })))
    }

    fun onPacesLivedChange(pacesLived: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(pacesLived = pacesLived.takeIf { it.isNotBlank() })))
    }

    fun onNotesChange(notes: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(notes = notes.takeIf { it.isNotBlank() })))
    }

    fun onWorkChange(work: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(work = work.takeIf { it.isNotBlank() })))
    }

    fun onDepartmentChange(department: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(department = department.takeIf { it.isNotBlank() })))
    }

    fun onOrganizationChange(organization: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(organization = organization.takeIf { it.isNotBlank() })))
    }

    fun onJobTitleChange(jobTitle: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(jobTitle = jobTitle.takeIf { it.isNotBlank() })))
    }

    fun onInterestsChange(interests: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(interests = interests.takeIf { it.isNotBlank() })))
    }

    fun onBooksChange(books: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(books = books.takeIf { it.isNotBlank() })))
    }

    fun onMusicChange(music: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(music = music.takeIf { it.isNotBlank() })))
    }

    fun onMoviesChange(movies: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(movies = movies.takeIf { it.isNotBlank() })))
    }

    fun onSportsChange(sports: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(sports = sports.takeIf { it.isNotBlank() })))
    }

    fun onWebsiteChange(website: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(website = website.takeIf { it.isNotBlank() })))
    }

    fun onMailingAddressChange(mailingAddress: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(mailingAddress = mailingAddress.takeIf { it.isNotBlank() })))
    }

    fun onLocationChange(location: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(location = location.takeIf { it.isNotBlank() })))
    }

    fun onPhoneChange(phone: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(phone = phone.takeIf { it.isNotBlank() })))
    }

    fun onStreamsChange(streams: String) {
        updateState(currentState.copy(tmpData = currentState.tmpData!!.copy(streams = streams.takeIf { it.isNotBlank() })))
    }

    fun saveData() {
        //TODO("Not yet implemented")
    }

}

data class PersonalSettingsState(
    val localData: UserData? = null,
    val data: PublicUserData? = null,
    val tmpData: PublicUserData? = null,
    val loading: Boolean = false,
    val currentOpened: Boolean = false,
)