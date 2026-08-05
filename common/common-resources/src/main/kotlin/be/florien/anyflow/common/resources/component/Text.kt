package be.florien.anyflow.common.resources.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import be.florien.anyflow.common.resources.R
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce


@OptIn(FlowPreview::class)
@Composable
fun SearchBar(
    isSearching: Boolean,
    searchTotal: Int,
    searchPosition: Int,
    onSearchPositionChange: (Int) -> Unit,
    onSearchChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val searchState = rememberTextFieldState()

    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }
            .debounce { 300L }
            .collect { onSearchChange(it) }
    }
    if (isSearching) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                modifier = Modifier
                    .weight(1F, fill = true)
                    .focusRequester(focusRequester),
                state = searchState
            )
            Text(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                text = if (searchTotal > 0) {
                    "${searchPosition + 1}/$searchTotal"
                } else {
                    stringResource(R.string.search_no_result)
                },
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )
            IconButton(onClick = {
                onSearchPositionChange(searchPosition + 1)
            }) {
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    tint = MaterialTheme.colorScheme.tertiary,
                    contentDescription = null
                )
            }
            IconButton(onClick = {
                onSearchPositionChange(searchPosition - 1)
            }) {
                Icon(
                    Icons.Outlined.KeyboardArrowUp,
                    tint = MaterialTheme.colorScheme.tertiary,
                    contentDescription = null
                )
            }
        }
    }
}