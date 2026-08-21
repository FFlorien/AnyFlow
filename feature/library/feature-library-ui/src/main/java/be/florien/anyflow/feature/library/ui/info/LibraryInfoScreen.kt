package be.florien.anyflow.feature.library.ui.info

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import be.florien.anyflow.common.resources.R
import coil3.compose.AsyncImage
import kotlinx.collections.immutable.PersistentList

@Composable
fun LibraryInfoScreen(
    list: PersistentList<InfoRowDisplay>,
    executeAction: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.background(color = MaterialTheme.colorScheme.surface),
    ) {
        itemsIndexed(items = list, key = { _, item ->
            item.key
        }) { index, item ->
            when (item) {
                is InfoRowDisplay.Action -> Action(executeAction, index, item)
                is InfoRowDisplay.Item -> Item(executeAction, index, item)
                is InfoRowDisplay.List -> List(executeAction, index, item)
            }
        }
    }
}

@Composable
private fun LazyItemScope.List(
    executeAction: (Int) -> Unit,
    index: Int,
    item: InfoRowDisplay.List
) {
    Row(
        modifier = Modifier
            .animateItem()
            .fillMaxWidth()
            .clickable {
                executeAction(index)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.size(30.dp),
            painter = painterResource(item.leftImage),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            contentDescription = null
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = stringResource(item.title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = item.countText.getText(LocalResources.current),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_go),
            contentDescription = "todo",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun LazyItemScope.Item(
    executeAction: (Int) -> Unit,
    index: Int,
    item: InfoRowDisplay.Item
) {
    Row(
        modifier = Modifier
            .animateItem()
            .fillMaxWidth()
            .clickable {
                executeAction(index)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (item.leftImage.url == null) {
            item.leftImage.resource?.let {
                Image(
                    modifier = Modifier.size(30.dp),
                    painter = painterResource(it),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    contentDescription = null
                )
            }
        }
        item.leftImage.url?.let {
            AsyncImage(
                modifier = Modifier.size(100.dp),
                model = it,
                contentDescription = null
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = stringResource(item.title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = item.info.getText(LocalResources.current),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        item.rowType.iconRes?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = "todo",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LazyItemScope.Action(
    executeAction: (Int) -> Unit,
    index: Int,
    item: InfoRowDisplay.Action
) {
    Row(
        modifier = Modifier
            .animateItem()
            .fillMaxWidth()
            .clickable {
                executeAction(index)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.size(30.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = stringResource(item.title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = item.actionDescription.getText(LocalResources.current),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        item.rowType.iconRes?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = "todo",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}