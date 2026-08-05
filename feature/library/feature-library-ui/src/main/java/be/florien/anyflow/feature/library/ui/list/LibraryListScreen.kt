package be.florien.anyflow.feature.library.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import be.florien.anyflow.common.resources.R
import be.florien.anyflow.common.resources.component.ScrollBar
import be.florien.anyflow.common.resources.component.SlideRightToAction
import be.florien.anyflow.common.resources.component.handlerWidth
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow

@Immutable
data class FilterDisplay(
    val id: Long,
    val title: String,
    val isSelected: Boolean,
    val section: String,
    val artUrl: String? = null,
    val duration: String? = null,
    val subtitle: String? = null,
    val subSubtitle: String? = null
)

@Composable
fun LibraryListScreen(
    itemsPager: Flow<PagingData<FilterDisplay>>,
    onClick: (FilterDisplay) -> Unit,
    onNavigation: (FilterDisplay) -> Unit
) {
    val items = itemsPager.collectAsLazyPagingItems()
    val lazyListState = rememberLazyListState()
    val loadingLabel = stringResource(R.string.general_loading_label)
    val loadingLabelShort = stringResource(R.string.general_loading_label_short)
    Box {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(end = handlerWidth),
        ) {
            items(
                count = items.itemCount,
                key = items.itemKey { it.id }
            ) { position ->
                val item = items[position]
                if (item == null) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        text = loadingLabel
                    )
                } else {
                    FilterItem(item, onNavigation, onClick)
                }
            }
        }
        ScrollBar(
            items = items,
            lazyListState = lazyListState,
            getSection = {
                this?.section ?: loadingLabelShort
            }
        )
    }
}

@Composable
private fun FilterItem(
    item: FilterDisplay,
    onNavigation: (FilterDisplay) -> Unit,
    onClick: (FilterDisplay) -> Unit
) {
    SlideRightToAction(
        modifier = Modifier
            .combinedClickable(onLongClick = {
                onNavigation(item)
            }) {
                onClick(item)
            },
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.information_content_description),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        onSlideComplete = { onNavigation(item) }
    ) {
        ForeGround(item)
    }
}

@Composable
private fun ForeGround(
    item: FilterDisplay
) {
    val backgroundColor =
        if (item.isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val textColor =
        if (item.isSelected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        val artUrl = item.artUrl
        if (artUrl != null) {
            AsyncImage(
                modifier = Modifier
                    .size(75.dp)
                    .align(Alignment.CenterVertically),
                model = artUrl,
                contentDescription = stringResource(
                    R.string.cover_content_description,
                    item.title
                )
            )
        }
        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            Row(modifier = Modifier.align(Alignment.End)) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    item.subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                    item.subSubtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }

                }
                item.duration?.let {
                    Text(
                        modifier = Modifier
                            .align(Alignment.Bottom)
                            .padding(bottom = 8.dp, end = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.large
                            )
                            .padding(horizontal = 4.dp),
                        text = it,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
