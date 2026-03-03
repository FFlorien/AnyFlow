package be.florien.anyflow.common.resources.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import be.florien.anyflow.common.resources.R


@Composable
fun TopSnackbarHost(snackbarHostState: SnackbarHostState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            snackbar = {
                Snackbar(
                    snackbarData = it,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                )
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueTopAppBar(
    title: String,
    onClose: () -> Unit,
    actions: @Composable (RowScope) -> Unit
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    painterResource(R.drawable.ic_up),
                    contentDescription = stringResource(R.string.content_description_up),
                    tint = Color.Unspecified
                )
            }
        },
        actions = actions,
        colors = TopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.secondary,
            subtitleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}