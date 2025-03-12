package com.mercata.openemail.profile_screen

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.mercata.openemail.AbstractViewModel
import com.mercata.openemail.R
import com.mercata.openemail.models.PublicUserData
import com.mercata.openemail.utils.FileUtils
import com.mercata.openemail.utils.HttpResult
import com.mercata.openemail.utils.deleteUserImage
import com.mercata.openemail.utils.getProfilePictureUrl
import com.mercata.openemail.utils.getProfilePublicData
import com.mercata.openemail.utils.safeApiCall
import com.mercata.openemail.utils.updateCall
import com.mercata.openemail.utils.uploadUserImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class ProfileViewModel : AbstractViewModel<ProfileState>(ProfileState()) {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            updateState(currentState.copy(loading = true))
            listOf(
                launch { updateCall() }
            ).joinAll()
            updateState(currentState.copy(loading = false))
        }

        currentState.tabs.addAll(
            listOf(
                GeneralTabData(),
                PersonalTabData(),
                WorkTabData(),
                InterestsTabData(),
                ContactsTabData(),
                ConfigurationTabData()
            )
        )
        updateState(currentState.copy())

    }

    private val fu: FileUtils by inject()
    private var instantPhotoUri: Uri? = null

    private suspend fun updateCall() {
        when (val call = safeApiCall { getProfilePublicData(sp.getUserAddress()!!) }) {
            is HttpResult.Error -> {
                //ignore
            }

            is HttpResult.Success -> {
                updateState(
                    currentState.copy(
                        saved = call.data?.copy(), current = call.data?.copy()
                    )
                )
            }
        }
    }

    fun setUserImage(uri: Uri?) {
        updateState(currentState.copy(selectedNewImage = uri))
    }

    fun toggleAttachmentBottomSheet(shown: Boolean) {
        updateState(currentState.copy(attachmentBottomSheetShown = shown))
    }

    fun getNewFileUri(): Uri {
        instantPhotoUri = fu.getUriForFile(fu.createImageFile())
        return instantPhotoUri!!
    }

    fun addInstantPhotoAsAttachment(success: Boolean) {
        if (success) {
            updateState(currentState.copy(selectedNewImage = instantPhotoUri))
        } else {
            instantPhotoUri = null
        }
    }

    fun saveChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            updateState(currentState.copy(loading = true))
            val results = arrayListOf(
                async {
                    when (safeApiCall { updateCall(sp.getUserData()!!, currentState.current!!) }) {
                        is HttpResult.Error -> {
                            return@async false
                        }

                        is HttpResult.Success -> {
                            return@async true
                        }
                    }
                }
            )

            if (currentState.selectedNewImage != null) {
                results.add(async {
                    when (safeApiCall {
                        uploadUserImage(
                            currentState.selectedNewImage!!,
                            sp,
                            fu
                        )
                    }) {
                        is HttpResult.Error -> {
                            return@async false
                        }

                        is HttpResult.Success -> {
                            return@async true
                        }
                    }
                })
            }

            val successful = results.awaitAll().any { !it }.not()

            if (successful) {
                updateCall()
                setUserImage(null)
            }
            updateState(currentState.copy(loading = false))
        }
    }

    suspend fun deleteUserpic(): String? {
        return withContext(Dispatchers.IO) {
            updateState(currentState.copy(loading = true))
            val rv: String? = when (safeApiCall { deleteUserImage(sp) }) {
                is HttpResult.Error -> {
                    null
                }

                is HttpResult.Success -> {
                    sp.getUserAddress()?.getProfilePictureUrl()
                }
            }

            updateState(currentState.copy(loading = false))
            rv
        }
    }

    interface TabData {
        val titleResId: Int
        val listItems: ArrayList<TabListItem>
    }

    data class GeneralTabData(
        override val titleResId: Int = R.string.general,
        override val listItems: ArrayList<TabListItem> = arrayListOf(
            //UserPicListItem(),
            InputListItem(
                hintResId = R.string.full_name_hint,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(fullName = str)
                        )
                    )
                },
                getValue = { state -> state.current?.fullName ?: "" },
                getSavedData = { state -> state.saved?.fullName ?: "" }
            ),
            InputListItem(
                hintResId = R.string.status,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(status = str)
                        )
                    )
                },
                getValue = { state -> state.current?.status ?: "" },
                getSavedData = { state -> state.saved?.status ?: "" }
            ),
            InputListItem(
                hintResId = R.string.about,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(about = str)
                        )
                    )
                },
                getValue = { state -> state.current?.about ?: "" },
                getSavedData = { state -> state.saved?.about ?: "" }
            ),
            InputWithSwitchListItem(
                switchTitle = R.string.away,
                getSwitchValue = { state -> state.current?.away ?: false },
                getSwitchSavedData = { state -> state.saved?.away ?: false },
                onSwitchChanged = { viewModel, isChecked ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(
                                away = isChecked
                            )
                        )
                    )
                },
                hintResId = R.string.away_warning,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(awayWarning = str)
                        )
                    )
                },
                getValue = { state -> state.current?.awayWarning ?: "" },
                getSavedData = { state -> state.saved?.awayWarning ?: "" }
            ),
        )
    ) : TabData

    data class ConfigurationTabData(
        override val titleResId: Int = R.string.configuration,
        override val listItems: ArrayList<TabListItem> = arrayListOf(
            SwitchListItem(
                R.string.last_seen_public,
                getValue = { state -> state.current?.lastSeenPublic ?: true },
                getHintResId = { state -> if (state.current?.lastSeenPublic != false) R.string.enabled_last_seen_public_hint else R.string.disabled_last_seen_public_hint },
                getSavedData = { state -> state.saved?.lastSeenPublic ?: true },
                onChanged = { viewModel, isChecked ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(
                                lastSeenPublic = isChecked
                            )
                        )
                    )
                }),
            SwitchListItem(
                R.string.public_links_querying,
                getValue = { state -> state.current?.publicLinks ?: true },
                getHintResId = { _ -> R.string.public_links_querying_hint },
                getSavedData = { state -> state.saved?.publicLinks ?: true },
                onChanged = { viewModel, isChecked ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(
                                publicLinks = isChecked
                            )
                        )
                    )
                }),
            SwitchListItem(
                R.string.public_access,
                getValue = { state -> state.current?.publicAccess ?: true },
                getSavedData = { state -> state.saved?.publicAccess ?: true },
                getHintResId = { state -> if (state.current?.publicAccess != false) R.string.enabled_public_access_hint else R.string.disabled_public_access_hint },
                onChanged = { viewModel, isChecked ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(
                                publicAccess = isChecked
                            )
                        )
                    )
                })
        )
    ) : TabData

    data class ContactsTabData(
        override val titleResId: Int = R.string.contacts,
        override val listItems: ArrayList<TabListItem> = arrayListOf(
            InputListItem(
                hintResId = R.string.website,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(website = str)
                        )
                    )
                },
                getValue = { state -> state.current?.website ?: "" },
                getSavedData = { state -> state.saved?.website ?: "" }
            ),
            InputListItem(
                hintResId = R.string.location,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(location = str)
                        )
                    )
                },
                getValue = { state -> state.current?.location ?: "" },
                getSavedData = { state -> state.saved?.location ?: "" }
            ),
            InputListItem(
                hintResId = R.string.mailingAddress,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(mailingAddress = str)
                        )
                    )
                },
                getValue = { state -> state.current?.mailingAddress ?: "" },
                getSavedData = { state -> state.saved?.mailingAddress ?: "" }
            ),
            InputListItem(
                hintResId = R.string.phone,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(phone = str)
                        )
                    )
                },
                getValue = { state -> state.current?.phone ?: "" },
                getSavedData = { state -> state.saved?.phone ?: "" }
            ),
            InputListItem(
                hintResId = R.string.streams,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(streams = str)
                        )
                    )
                },
                getValue = { state -> state.current?.streams ?: "" },
                getSavedData = { state -> state.saved?.streams ?: "" }
            ),
        )
    ) : TabData

    data class InterestsTabData(
        override val titleResId: Int = R.string.interests,
        override val listItems: ArrayList<TabListItem> = arrayListOf(
            InputListItem(
                hintResId = R.string.interests,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(interests = str)
                        )
                    )
                },
                getValue = { state -> state.current?.interests ?: "" },
                getSavedData = { state -> state.saved?.interests ?: "" }
            ),
            InputListItem(
                hintResId = R.string.books,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(books = str)
                        )
                    )
                },
                getValue = { state -> state.current?.books ?: "" },
                getSavedData = { state -> state.saved?.books ?: "" }
            ),
            InputListItem(
                hintResId = R.string.movies,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(movies = str)
                        )
                    )
                },
                getValue = { state -> state.current?.movies ?: "" },
                getSavedData = { state -> state.saved?.movies ?: "" }
            ),
            InputListItem(
                hintResId = R.string.music,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(music = str)
                        )
                    )
                },
                getValue = { state -> state.current?.music ?: "" },
                getSavedData = { state -> state.saved?.music ?: "" }
            ),
            InputListItem(
                hintResId = R.string.sports,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(sports = str)
                        )
                    )
                },
                getValue = { state -> state.current?.sports ?: "" },
                getSavedData = { state -> state.saved?.sports ?: "" }
            )
        )
    ) : TabData

    data class PersonalTabData(
        override val titleResId: Int = R.string.personal,
        override val listItems: ArrayList<TabListItem> = arrayListOf(

            InputListItem(
                hintResId = R.string.gender,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(gender = str)
                        )
                    )
                },
                getValue = { state -> state.current?.gender ?: "" },
                getSavedData = { state -> state.saved?.gender ?: "" }
            ),
            InputListItem(
                hintResId = R.string.relationshipStatus,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(relationshipStatus = str)
                        )
                    )
                },
                getValue = { state -> state.current?.relationshipStatus ?: "" },
                getSavedData = { state -> state.saved?.relationshipStatus ?: "" }
            ),
            InputListItem(
                hintResId = R.string.education,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(education = str)
                        )
                    )
                },
                getValue = { state -> state.current?.education ?: "" },
                getSavedData = { state -> state.saved?.education ?: "" }
            ),
            InputListItem(
                hintResId = R.string.language,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(language = str)
                        )
                    )
                },
                getValue = { state -> state.current?.language ?: "" },
                getSavedData = { state -> state.saved?.language ?: "" }
            ),
            InputListItem(
                hintResId = R.string.placesLived,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(placesLived = str)
                        )
                    )
                },
                getValue = { state -> state.current?.placesLived ?: "" },
                getSavedData = { state -> state.saved?.placesLived ?: "" }
            ),
            InputListItem(
                hintResId = R.string.notes,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(notes = str)
                        )
                    )
                },
                getValue = { state -> state.current?.notes ?: "" },
                getSavedData = { state -> state.saved?.notes ?: "" }
            ),
        )
    ) : TabData

    data class WorkTabData(
        override val titleResId: Int = R.string.work,
        override val listItems: ArrayList<TabListItem> = arrayListOf(
            InputListItem(
                hintResId = R.string.work,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(work = str)
                        )
                    )
                },
                getValue = { state -> state.current?.work ?: "" },
                getSavedData = { state -> state.saved?.work ?: "" }
            ),
            InputListItem(
                hintResId = R.string.organization,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(organization = str)
                        )
                    )
                },
                getValue = { state -> state.current?.organization ?: "" },
                getSavedData = { state -> state.saved?.organization ?: "" }
            ),
            InputListItem(
                hintResId = R.string.department,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(department = str)
                        )
                    )
                },
                getValue = { state -> state.current?.department ?: "" },
                getSavedData = { state -> state.saved?.department ?: "" }
            ),
            InputListItem(
                hintResId = R.string.jobTitle,
                onChanged = { viewModel, str ->
                    viewModel.updateState(
                        viewModel.currentState.copy(
                            current = viewModel.currentState.current?.copy(jobTitle = str)
                        )
                    )
                },
                getValue = { state -> state.current?.jobTitle ?: "" },
                getSavedData = { state -> state.saved?.jobTitle ?: "" }
            ),
        )
    ) : TabData

    interface TabListItem {
        fun hasChanges(state: ProfileState): Boolean
    }

    class UserPicListItem : TabListItem {
        override fun hasChanges(state: ProfileState): Boolean = state.selectedNewImage != null
    }

    data class InputWithSwitchListItem(
        val switchTitle: Int,
        val switchHint: Int? = null,
        val getSwitchValue: (state: ProfileState) -> Boolean,
        val onSwitchChanged: (viewModel: ProfileViewModel, isChecked: Boolean) -> Unit,
        val getSwitchSavedData: (state: ProfileState) -> Boolean,

        val hintResId: Int,
        val supportingStringResId: Int? = null,
        val onChanged: (viewModel: ProfileViewModel, str: String) -> Unit,
        val getValue: (state: ProfileState) -> String,
        val getSavedData: (state: ProfileState) -> String
    ) : TabListItem {
        override fun hasChanges(state: ProfileState): Boolean =
            getValue(state) != getSavedData(state) || getSwitchValue(state) != getSwitchSavedData(
                state
            )
    }

    data class InputListItem(
        val hintResId: Int,
        val supportingStringResId: Int? = null,
        val onChanged: (viewModel: ProfileViewModel, str: String) -> Unit,
        val getValue: (state: ProfileState) -> String,
        val getSavedData: (state: ProfileState) -> String
    ) : TabListItem {
        override fun hasChanges(state: ProfileState): Boolean =
            getValue(state) != getSavedData(state)

    }

    data class SwitchListItem(
        val titleResId: Int,
        val getHintResId: (state: ProfileState) -> Int?,
        val getValue: (state: ProfileState) -> Boolean,
        val onChanged: (viewModel: ProfileViewModel, isChecked: Boolean) -> Unit,
        val getSavedData: (state: ProfileState) -> Boolean
    ) : TabListItem {
        override fun hasChanges(state: ProfileState): Boolean =
            getValue(state) != getSavedData(state)
    }
}

data class ProfileState(
    val loading: Boolean = false,
    val attachmentBottomSheetShown: Boolean = false,
    val saved: PublicUserData? = null,
    val current: PublicUserData? = null,
    val selectedNewImage: Uri? = null,
    val tabs: ArrayList<ProfileViewModel.TabData> = arrayListOf()
) {
    fun hasChanges(): Boolean =
        selectedNewImage != null || tabs.any { it.listItems.any { item -> item.hasChanges(this) } }
}
