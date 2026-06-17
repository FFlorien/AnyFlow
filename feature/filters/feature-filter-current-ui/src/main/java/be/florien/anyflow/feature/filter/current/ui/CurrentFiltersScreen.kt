package be.florien.anyflow.feature.filter.current.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import be.florien.anyflow.common.resources.utils.forwardingPainter
import coil3.compose.AsyncImage
import kotlinx.collections.immutable.PersistentList

data class Filter(
    val id: Int,
    val imageUrl: String?,
    val displayText: String,
    val fallbackRes: Int?
)

@Composable
fun CurrentFiltersScreen(
    state: PersistentList<Filter>,
    onDelete: (Int) -> Unit,
    onDeleteAll: () -> Unit,
    navigateToLibrary: () -> Unit,
    navigateToPodcast: () -> Unit
) {
    if (state.isEmpty()) {
        EmptyScreen(navigateToLibrary, navigateToPodcast)
    } else {
        FilterList(state, onDeleteAll, onDelete)
    }
}

@Composable
private fun EmptyScreen(navigateToLibrary: () -> Unit, navigateToPodcast: () -> Unit) {
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Text(stringResource(R.string.filter_empty_text))
        Button(onClick = navigateToLibrary) { Text(stringResource(R.string.filter_navigate_library)) }
        Button(onClick = navigateToPodcast) { Text(stringResource(R.string.filter_navigate_podcasts)) }
    }
}

@Composable
private fun FilterList(
    state: PersistentList<Filter>,
    onDeleteAll: () -> Unit,
    onDelete: (Int) -> Unit
) {
    LazyColumn(Modifier.background(MaterialTheme.colorScheme.surface)) {
        item(key = "deleteAll") {
            FilterItem(stringResource(R.string.action_filter_clear), onDeleteAll)
        }
        items(items = state, key = { it.displayText }) {
            FilterItem(
                imageUrl = it.imageUrl,
                text = it.displayText,
                onDelete = { onDelete(it.id) },
                fallbackRes = it.fallbackRes
            )
        }
    }
}

@Composable
private fun DeleteButton(onDelete: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(modifier = modifier, onClick = onDelete) {
        Icon(
            modifier = Modifier
                .padding(8.dp)
                .size(32.dp),
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = stringResource(R.string.filter_delete_content_description),
            tint = MaterialTheme.colorScheme.primary

        )
    }
}

@Composable
private fun FilterItem(
    text: String,
    onDelete: () -> Unit,
    imageUrl: String? = null,
    fallbackRes: Int? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imageUrl != null) {
            val fallback = fallbackRes?.let {
                forwardingPainter(
                    painter = painterResource(it),
                    colorFilter = ColorFilter.tint(
                        MaterialTheme.colorScheme.primary,
                        BlendMode.SrcIn
                    )
                )
            }
            AsyncImage(
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 8.dp),
                model = imageUrl,
                contentDescription = stringResource(R.string.filter_image_content_description),
                error = fallback,
                fallback = fallback
            )
        }
        Text(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            text = AnnotatedString.fromHtml(text),
            color = MaterialTheme.colorScheme.onSurface
        )
        DeleteButton(onDelete = onDelete)
    }
}


