package com.yeromich.pokecollect.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yeromich.pokecollect.R
import com.yeromich.pokecollect.data.local.CollectionValue
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.domain.model.formatEur
import com.yeromich.pokecollect.ui.components.CardGridItem
import com.yeromich.pokecollect.ui.components.FolderStrip
import com.yeromich.pokecollect.ui.components.StatusBarScrim

private const val VALUE_COUNT_MS = 600

@Composable
fun CollectionScreen(
    bottomPadding: Dp,
    onCardClick: (Card) -> Unit,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val value by viewModel.value.collectAsStateWithLifecycle()
    val allValue by viewModel.allValue.collectAsStateWithLifecycle()
    val allPreviewImages by viewModel.allPreviewImages.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val setCount = remember(cards) { cards.distinctBy { it.setName }.size }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = bottomPadding + 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                CollectionHeader(
                    cardCount = value?.entries ?: 0,
                    setCount = setCount,
                    value = value
                )
            }
            item(key = "folders", span = { GridItemSpan(maxLineSpan) }) {
                FolderStrip(
                    folders = folders,
                    selectedFolderId = selectedFolderId,
                    allCount = allValue?.entries ?: 0,
                    allTotalEur = allValue?.totalEur ?: 0.0,
                    allPreviewImages = allPreviewImages,
                    onSelect = viewModel::selectFolder,
                    onCreate = viewModel::createFolder,
                    onRename = viewModel::renameFolder,
                    onDelete = viewModel::deleteFolder,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            // Pick the collection first, then how to order it.
            if (cards.size > 1) {
                item(key = "sort", span = { GridItemSpan(maxLineSpan) }) {
                    SortPills(sort = sort, onSortChange = viewModel::setSort, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                }
            }
            if (cards.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(
                                if (selectedFolderId == null) R.string.collection_empty_hint else R.string.folder_empty_hint
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(cards, key = { "${it.id}_${it.variant}" }) { card ->
                    CardGridItem(
                    card = card,
                    onClick = { onCardClick(card) },
                    priceLabel = card.priceEur?.takeIf { sort == CollectionSort.MostValuable }?.let(::formatEur),
                    modifier = Modifier.animateItem()
                )
                }
            }
        }
        StatusBarScrim()
    }
}

@Composable
private fun CollectionHeader(
    cardCount: Int,
    setCount: Int,
    value: CollectionValue?
) {
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.collection_title),
            style = MaterialTheme.typography.displaySmall
        )
        // Wraps onto a second line once the total grows long, rather than squeezing the stats.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            if (value != null && value.pricedEntries > 0) {
                ValueStat(totalEur = value.totalEur)
            }
            Stat(value = cardCount.toString(), label = countLabel(cardCount, R.string.collection_stat_card, R.string.collection_stat_cards))
            Stat(value = setCount.toString(), label = countLabel(setCount, R.string.collection_stat_set, R.string.collection_stat_sets))
        }
        // Say so when the total leaves cards out, so it never reads as the full worth.
        if (value != null && value.pricedEntries in 1 until value.entries) {
            Text(
                text = stringResource(R.string.collection_value_coverage, value.pricedEntries, value.entries),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

// English and German only ever use the singular for exactly one. Android plurals would follow the
// phone's language instead, so on a Ukrainian phone "31" came out as "31 card".
@Composable
private fun countLabel(count: Int, @StringRes one: Int, @StringRes other: Int): String =
    stringResource(if (count == 1) one else other)

@Composable
private fun SortPills(sort: CollectionSort, onSortChange: (CollectionSort) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.selectableGroup()) {
        SortPill(stringResource(R.string.collection_sort_newest), sort == CollectionSort.Newest) {
            onSortChange(CollectionSort.Newest)
        }
        SortPill(stringResource(R.string.collection_sort_valuable), sort == CollectionSort.MostValuable) {
            onSortChange(CollectionSort.MostValuable)
        }
    }
}

@Composable
private fun SortPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(if (selected) colors.primaryContainer else colors.surfaceContainerHigh, label = "pillBg")
    val content by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, label = "pillFg")
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = content,
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun ValueStat(totalEur: Double) {
    // Counts toward a new total after a price refresh instead of jumping; the first total just appears.
    val shown = remember { Animatable(totalEur.toFloat()) }
    LaunchedEffect(totalEur) {
        shown.animateTo(totalEur.toFloat(), tween(VALUE_COUNT_MS, easing = FastOutSlowInEasing))
    }
    Stat(
        value = formatEur(shown.value.toDouble()),
        label = stringResource(R.string.collection_value_label),
        valueColor = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun Stat(value: String, label: String, valueColor: Color = Color.Unspecified) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = valueColor)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }
}
