package com.yeromich.pokecollect.ui.screens

import androidx.compose.material3.TextButton
import com.yeromich.pokecollect.ui.components.SadPokeball
import com.yeromich.pokecollect.ui.components.ChoicePills
import com.yeromich.pokecollect.domain.model.CardOrder
import com.yeromich.pokecollect.domain.model.setIdOfCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.yeromich.pokecollect.R
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.ui.components.CardGridItem

@Composable
fun CatalogScreen(
    onCardClick: (Card) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val ownedIds by viewModel.ownedIds.collectAsStateWithLifecycle()
    val setTotal by viewModel.setTotal.collectAsStateWithLifecycle()
    val order by viewModel.order.collectAsStateWithLifecycle()
    val inSet = viewModel.setId != null
    val ownedInSet = remember(ownedIds, viewModel.setId) {
        viewModel.setId?.let { id -> ownedIds.count { setIdOfCard(it) == id } } ?: 0
    }
    val cards = viewModel.cards.collectAsLazyPagingItems()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
            ) {
                if (viewModel.setId != null) {
                    FilledIconButton(
                        onClick = onNavigateBack,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = stringResource(R.string.detail_back))
                    }
                }
                Text(
                    text = viewModel.setName ?: stringResource(R.string.screen_catalog_title),
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val total = setTotal
            if (inSet && total != null && total > 0) {
                SetProgress(
                    owned = ownedInSet,
                    total = total,
                    label = stringResource(R.string.catalog_progress, ownedInSet, total),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                )
            }
            TextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text(stringResource(R.string.catalog_search_hint)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_magnifying_glass), contentDescription = null) },
                singleLine = true,
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            if (inSet) {
                ChoicePills(
                    options = listOf(
                        CardOrder.Number to stringResource(R.string.catalog_order_number),
                        CardOrder.Name to stringResource(R.string.catalog_order_name)
                    ),
                    selected = order,
                    onSelect = viewModel::onOrderChange,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    cards.loadState.refresh is LoadState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    cards.loadState.refresh is LoadState.Error -> {
                        val error = (cards.loadState.refresh as LoadState.Error).error
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = error.localizedMessage
                                    ?: stringResource(R.string.catalog_error_generic),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(onClick = { cards.retry() }) {
                                Text(stringResource(R.string.catalog_error_retry))
                            }
                        }
                    }

                    cards.itemCount == 0 && cards.loadState.refresh is LoadState.NotLoading -> {
                        if (query.isBlank() && viewModel.setId == null) {
                            Text(
                                text = stringResource(R.string.catalog_search_prompt),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 32.dp)
                            )
                        } else {
                            NothingFound(
                                onClear = { viewModel.onQueryChange("") },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                count = cards.itemCount,
                                key = cards.itemKey { it.id }
                            ) { index ->
                                cards[index]?.let { card ->
                                    CardGridItem(
                                        card = card,
                                        onClick = { onCardClick(card) },
                                        owned = card.id in ownedIds
                                    )
                                }
                            }
                            if (cards.loadState.append is LoadState.Loading) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Empty search: a sad Pokéball, what happened, and the one way out. */
@Composable
private fun NothingFound(onClear: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        SadPokeball()
        Text(
            text = stringResource(R.string.catalog_empty),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            text = stringResource(R.string.catalog_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
        TextButton(onClick = onClear, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.catalog_clear_search), style = MaterialTheme.typography.labelLarge)
        }
    }
}
