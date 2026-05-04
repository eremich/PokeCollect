package com.yeromich.pokecollect.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yeromich.pokecollect.R
import com.yeromich.pokecollect.domain.model.Card

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: CardDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isInCollection by viewModel.isInCollection.collectAsStateWithLifecycle()
    val quantity by viewModel.quantity.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState is CardDetailUiState.Success) {
                        Text(
                            text = (uiState as CardDetailUiState.Success).card.name,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is CardDetailUiState.Loading -> CircularProgressIndicator()
                is CardDetailUiState.Error -> Text(state.message)
                is CardDetailUiState.Success -> CardDetailContent(
                    card = state.card,
                    isInCollection = isInCollection,
                    quantity = quantity,
                    onAddOrIncrement = viewModel::addOrIncrement
                )
            }
        }
    }
}

@Composable
private fun CardDetailContent(
    card: Card,
    isInCollection: Boolean,
    quantity: Int,
    onAddOrIncrement: () -> Unit
) {
    Column(
        modifier = Modifier
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
            text = "${card.number} \u2022 ${card.setName}",
            style = MaterialTheme.typography.bodyMedium
        )
        if (card.rarity != null) {
            Text(text = card.rarity, style = MaterialTheme.typography.bodySmall)
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
