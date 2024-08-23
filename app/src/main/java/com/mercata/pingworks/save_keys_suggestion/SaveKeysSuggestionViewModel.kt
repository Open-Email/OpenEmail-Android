package com.mercata.pingworks.save_keys_suggestion

import com.mercata.pingworks.AbstractViewModel

class SaveKeysSuggestionViewModel: AbstractViewModel<SaveKeysSuggestionState>(SaveKeysSuggestionState()) {

    init {
        //TODO get keys from datastore
    }

}

data class SaveKeysSuggestionState(val privateSigningKey: String = "", val privateEncryptionKey: String = "")