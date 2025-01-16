package com.mercata.pingworks.profile_screen

import androidx.lifecycle.viewModelScope
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.R
import com.mercata.pingworks.models.PublicUserData
import com.mercata.pingworks.utils.HttpResult
import com.mercata.pingworks.utils.SharedPreferences
import com.mercata.pingworks.utils.getProfilePublicData
import com.mercata.pingworks.utils.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ProfileViewModel : AbstractViewModel<ProfileState>(ProfileState()) {

    init {
        val sp: SharedPreferences by inject()
        viewModelScope.launch(Dispatchers.IO) {
            updateState(currentState.copy(loading = true))
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

    fun saveChanges() {
        //TODO("Not yet implemented")
    }

    interface TabData {
        val titleResId: Int
        val listItems: List<TabListItem>
    }

    data class GeneralTabData(
        override val titleResId: Int = R.string.general,
        override val listItems: List<TabListItem> = listOf()
    ) : TabData

    data class ConfigurationTabData(
        override val titleResId: Int = R.string.configuration,
        override val listItems: List<TabListItem> = listOf(
            SwitchListItem(
                R.string.last_seen_public,
                getValue = { state -> state.current?.lastSeenPublic ?: false },
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
                R.string.public_access,
                getValue = { state -> state.current?.publicAccess ?: false },
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
        override val listItems: List<TabListItem> = listOf(
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
            ),
        )
    ) : TabData

    data class InterestsTabData(
        override val titleResId: Int = R.string.interests,
        override val listItems: List<TabListItem> = listOf(
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
            )
        )
    ) : TabData

    data class PersonalTabData(
        override val titleResId: Int = R.string.personal,
        override val listItems: List<TabListItem> = listOf(

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
            ),
        )
    ) : TabData

    data class WorkTabData(
        override val titleResId: Int = R.string.work,
        override val listItems: List<TabListItem> = listOf(
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
            ),
        )
    ) : TabData

    interface TabListItem

    data class InputListItem(
        val hintResId: Int,
        val supportingStringResId: Int? = null,
        val onChanged: (viewModel: ProfileViewModel, str: String) -> Unit,
        val getValue: (state: ProfileState) -> String
    ) : TabListItem

    data class SwitchListItem(
        val titleResId: Int,
        val hintResId: Int? = null,
        val getValue: (state: ProfileState) -> Boolean,
        val onChanged: (viewModel: ProfileViewModel, isChecked: Boolean) -> Unit
    ) : TabListItem
}

data class ProfileState(
    val loading: Boolean = false,
    val saved: PublicUserData? = null,
    val current: PublicUserData? = null,
    val tabs: ArrayList<ProfileViewModel.TabData> = arrayListOf()
) {
    //TODO compare current state with initial
    fun hasChanges(): Boolean = true
}
