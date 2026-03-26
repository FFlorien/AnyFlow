package be.florien.anyflow.feature.library.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import be.florien.anyflow.common.resources.R
import be.florien.anyflow.common.resources.component.SlideRightToAction
import coil3.compose.AsyncImage

@Immutable
data class FilterDisplay(
    val id: Long,
    val title: String,
    val isSelected: Boolean,
    val artUrl: String? = null,
    val duration: String? = null,
    val subtitle: String? = null,
    val subSubtitle: String? = null
)

@Composable
fun LibraryListScreen(
    itemCount: Int,
    getItem: (Int) -> FilterDisplay?,
    onClick: (FilterDisplay) -> Unit,
    onNavigation: (FilterDisplay) -> Unit
) {
    LazyColumn(Modifier.background(MaterialTheme.colorScheme.surface)) {
        items(
            count = itemCount,
            key = { getItem(it)?.id ?: -1L }
        ) { position ->
            val item = getItem(position)
            if (item == null) {
                return@items
            }
            FilterItem(item, onNavigation, onClick)
        }
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
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = "todo",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
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
        if (item.isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        val artUrl = item.artUrl
        if (artUrl != null) {
            AsyncImage(
                modifier = Modifier.size(75.dp),
                model = artUrl,
                contentDescription = "todo"
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
                color = MaterialTheme.colorScheme.onSurface
            )
            item.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(modifier = Modifier.align(Alignment.End)) {
                item.subSubtitle?.let {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                item.duration?.let {
                    Text(
                        modifier = Modifier
                            .align(Alignment.Bottom)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        text = it
                    )
                }
            }
        }
    }
}
