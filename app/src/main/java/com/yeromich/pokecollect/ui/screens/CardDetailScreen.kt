package com.yeromich.pokecollect.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yeromich.pokecollect.R
import com.yeromich.pokecollect.data.remote.TcgdexCardDto
import com.yeromich.pokecollect.data.remote.TcgdexCardmarketDto
import com.yeromich.pokecollect.domain.model.Card

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: CardDetailViewModel = hiltViewModel()
) {
    val isInCollection by viewModel.isInCollection.collectAsStateWithLifecycle()
    val quantity by viewModel.quantity.collectAsStateWithLifecycle()
    val cardDetails by viewModel.cardDetails.collectAsStateWithLifecycle()
    val pricing by viewModel.pricing.collectAsStateWithLifecycle()
    val availableVariants by viewModel.availableVariants.collectAsStateWithLifecycle()
    val selectedVariant by viewModel.selectedVariant.collectAsStateWithLifecycle()

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.card.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        CardDetailContent(
            card = viewModel.card,
            cardDetails = cardDetails,
            pricing = pricing,
            isInCollection = isInCollection,
            quantity = quantity,
            onAddToCollection = { showSheet = true },
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text("Вариант", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    availableVariants.forEach { variant ->
                        FilterChip(
                            selected = variant == selectedVariant,
                            onClick = { viewModel.selectVariant(variant) },
                            label = { Text(variant) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.addOrIncrement()
                        showSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isInCollection) "Ещё раз добавить" else "Добавить в коллекцию")
                }
            }
        }
    }
}

@Composable
private fun CardDetailContent(
    card: Card,
    cardDetails: TcgdexCardDto?,
    pricing: TcgdexCardmarketDto?,
    isInCollection: Boolean,
    quantity: Int,
    onAddToCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rarity = cardDetails?.rarity ?: card.rarity
    val seriesName = cardDetails?.set?.serie?.name
    val setName = cardDetails?.set?.name ?: card.setName

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = card.imageLarge,
            contentDescription = card.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f),
            contentScale = ContentScale.Fit
        )

        Button(
            onClick = onAddToCollection,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isInCollection) {
                    stringResource(R.string.detail_in_collection, quantity)
                } else {
                    stringResource(R.string.detail_add_to_collection)
                }
            )
        }

        if (rarity != null) {
            AssistChip(
                onClick = {},
                label = { Text(rarity) },
                leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null) }
            )
        }

        if (seriesName != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Серия: $seriesName", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Набор: $setName", style = MaterialTheme.typography.bodyMedium)
            }
        }

        pricing?.let { cm ->
            Spacer(Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Cardmarket (EUR)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        cm.trend?.let { PriceColumn("Trend", it) }
                        cm.avg30?.let { PriceColumn("Ø 30d", it) }
                        cm.low?.let { PriceColumn("Low", it) }
                    }
                    if (cm.trendHolo != null || cm.avgHolo != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Holo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            cm.trendHolo?.let { PriceColumn("Trend", it) }
                            cm.avgHolo?.let { PriceColumn("Ø 30d", it) }
                            cm.lowHolo?.let { PriceColumn("Low", it) }
                        }
                    }
                    cm.updated?.let { updated ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Aktualisiert: ${updated.take(10)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceColumn(label: String, price: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "€%.2f".format(price),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
