package com.yeromich.pokecollect.ui.screens

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
import androidx.compose.ui.res.pluralStringResource
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
import com.yeromich.pokecollect.ui.components.StatusBarScrim

private const val VALUE_COUNT_MS = 600

@Composable
fun CollectionScreen(
    bottomPadding: Dp,
    onCardClick: (Card) -> Unit,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val count by viewModel.count.collectAsStateWithLifecycle()
    val value by viewModel.value.collectAsStateWithLifecycle()
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
                CollectionHeader(cardCount = count, setCount = setCount, value = value)
            }
            if (cards.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.collection_empty_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(cards, key = { "${it.id}_${it.variant}" }) { card ->
                    CardGridItem(card = card, onClick = { onCardClick(card) })
                }
            }
        }
        StatusBarScrim()
    }
}

@Composable
private fun CollectionHeader(cardCount: Int, setCount: Int, value: CollectionValue?) {
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
            Stat(value = cardCount.toString(), label = pluralStringResource(R.plurals.collection_stat_cards, cardCount))
            Stat(value = setCount.toString(), label = pluralStringResource(R.plurals.collection_stat_sets, setCount))
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
