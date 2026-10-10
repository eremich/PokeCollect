package com.yeromich.pokecollect.ui.screens

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.yeromich.pokecollect.R
import com.yeromich.pokecollect.domain.model.CardSet
import com.yeromich.pokecollect.ui.components.StatusBarScrim

private const val LOGO_ASPECT = 16f / 9f

@Composable
fun SetsScreen(
    bottomPadding: Dp,
    onSetClick: (CardSet) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: SetsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ownedBySet by viewModel.ownedBySet.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = bottomPadding + 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                SetsHeader(onSearchClick = onSearchClick)
            }
            when (val state = uiState) {
                SetsViewModel.UiState.Loading -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                SetsViewModel.UiState.Error -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.sets_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 64.dp)
                    )
                }

                is SetsViewModel.UiState.Success -> state.groups.forEach { group ->
                    item(key = "header_${group.name}", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = group.name ?: stringResource(R.string.sets_series_other),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 2.dp)
                        )
                    }
                    items(group.sets, key = { set -> set.id }) { set ->
                        SetTile(set = set, owned = ownedBySet[set.id] ?: 0, onClick = { onSetClick(set) })
                    }
                }
            }
        }
        StatusBarScrim()
    }
}

@Composable
private fun SetsHeader(onSearchClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .statusBarsPadding()
            .padding(start = 4.dp, top = 20.dp, bottom = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.screen_sets_title),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.weight(1f)
        )
        FilledIconButton(
            onClick = onSearchClick,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(painterResource(R.drawable.ic_magnifying_glass), contentDescription = stringResource(R.string.sets_search_all))
        }
    }
}

@Composable
private fun SetTile(set: CardSet, owned: Int, onClick: () -> Unit) {
    // Some sets have no logo, and some logo URLs 404; both fall back to a monogram.
    var logoFailed by remember(set.id) { mutableStateOf(set.logoUrl.isEmpty()) }

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(LOGO_ASPECT)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                    .padding(10.dp)
            ) {
                if (logoFailed) {
                    Text(
                        text = set.name.take(1),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(set.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = set.name,
                        contentScale = ContentScale.Fit,
                        onError = { logoFailed = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Text(
                text = set.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 2.dp, top = 10.dp)
            )
            // Untouched sets keep the plain count; a row of empty bars would only add noise.
            if (owned > 0 && set.total > 0) {
                SetProgress(owned = owned, total = set.total, modifier = Modifier.padding(start = 2.dp, top = 6.dp))
            } else {
                Text(
                    text = stringResource(R.string.sets_card_count, set.total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                )
            }
        }
    }
}

/** Thin progress bar with "31 / 84"; turns teal with a tick once the set is complete. */
@Composable
fun SetProgress(owned: Int, total: Int, modifier: Modifier = Modifier, label: String? = null) {
    val complete = owned >= total
    val tint = if (complete) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((owned.toFloat() / total).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(tint)
            )
        }
        Text(
            text = (label ?: stringResource(R.string.sets_progress, owned, total)) + if (complete) " ✓" else "",
            style = MaterialTheme.typography.bodySmall,
            color = if (complete) tint else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
