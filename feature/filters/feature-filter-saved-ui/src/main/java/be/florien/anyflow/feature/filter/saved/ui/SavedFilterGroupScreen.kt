package be.florien.anyflow.feature.filter.saved.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList

@Composable
fun SavedFilterGroupScreen(groups: PersistentList<FilterGroupItem>, onGroupClick: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        this.items(items = groups, key = { it.id }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onGroupClick(it.id)
                    }
                    .padding(16.dp)) {
                Text(
                    text = it.name, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = it.filtersDescription,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}