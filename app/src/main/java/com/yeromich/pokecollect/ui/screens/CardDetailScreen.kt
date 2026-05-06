package com.yeromich.pokecollect.ui.screens

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yeromich.pokecollect.R
import com.yeromich.pokecollect.data.remote.TcgdexCardDto
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
            isInCollection = isInCollection,
            quantity = quantity,
            onAddOrIncrement = viewModel::addOrIncrement,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun CardDetailContent(
    card: Card,
    cardDetails: TcgdexCardDto?,
    isInCollection: Boolean,
    quantity: Int,
    onAddOrIncrement: () -> Unit,
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

        Text(text = card.name, style = MaterialTheme.typography.headlineSmall)

        Text(
            text = "${card.number} • $setName",
            style = MaterialTheme.typography.bodyMedium
        )

        if (rarity != null) {
            AssistChip(
                onClick = {},
                label = { Text(rarity) },
                leadingIcon = {
                    Icon(Icons.Filled.Star, contentDescription = null)
                }
            )
        }

        if (seriesName != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Серия: $seriesName",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Набор: $setName",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Button(
            onClick = onAddOrIncrement,
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
    }
}
