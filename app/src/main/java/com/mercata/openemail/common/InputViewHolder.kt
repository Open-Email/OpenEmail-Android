package com.mercata.openemail.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun InputViewHolder(
    modifier: Modifier = Modifier,
    focusManager: FocusManager,
    value: String,
    hint: Int,
    isLoading: Boolean,
    onChange: (str: String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { str -> onChange(str) },
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth(),
        label = {
            Text(text = stringResource(hint))
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Unspecified,
            imeAction = ImeAction.Done,
            showKeyboardOnFocus = true,
            capitalization = KeyboardCapitalization.None
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
            }
        ),
    )
}