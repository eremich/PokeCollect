package com.yeromich.pokecollect.ui.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yeromich.pokecollect.R
import com.yeromich.pokecollect.data.remote.TcgdexCardDto
import com.yeromich.pokecollect.data.remote.TcgdexCardmarketDto
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.domain.model.formatEur
import com.yeromich.pokecollect.ui.components.FolderPickerSheet
import com.yeromich.pokecollect.ui.components.HoloCard
import com.yeromich.pokecollect.ui.components.isFoilVariant
import com.yeromich.pokecollect.ui.theme.Ink

private const val COLOR_FADE_MS = 400
private const val GLOW_ALPHA = 0.38f

// TCGdex returns German type names for the /de endpoint.
private val typeColors = mapOf(
    "Pflanze" to Color(0xFF7BC96F),
    "Feuer" to Color(0xFFFF8A5B),
    "Wasser" to Color(0xFF5DB2F0),
    "Elektro" to Color(0xFFF7D154),
    "Psycho" to Color(0xFFC18BFF),
    "Kampf" to Color(0xFFE0915C),
    "Finsternis" to Color(0xFF8E93C2),
    "Metall" to Color(0xFFAAB8C6),
    "Drache" to Color(0xFFD9B95C),
    "Fee" to Color(0xFFF59AD2),
    "Farblos" to Color(0xFFD6D2CA)
)


@Composable
fun CardDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: CardDetailViewModel = hiltViewModel()
) {
    val cardDetails by viewModel.cardDetails.collectAsStateWithLifecycle()
    val pricing by viewModel.pricing.collectAsStateWithLifecycle()
    val availableVariants by viewModel.availableVariants.collectAsStateWithLifecycle()
    val selectedVariant by viewModel.selectedVariant.collectAsStateWithLifecycle()
    val collectionCounts by viewModel.collectionCounts.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val memberFolderIds by viewModel.memberFolderIds.collectAsStateWithLifecycle()
    var folderSheet by remember { mutableStateOf<FolderSheetMode?>(null) }

    val typeColor = cardDetails?.types?.firstNotNullOfOrNull { typeColors[it] } ?: MaterialTheme.colorScheme.primary
    val accent by animateColorAsState(typeColor, tween(COLOR_FADE_MS), label = "typeAccent")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = GLOW_ALPHA), Color.Transparent)))
        )
        CardDetailContent(
            card = viewModel.card,
            cardDetails = cardDetails,
            pricing = pricing,
            accent = accent,
            availableVariants = availableVariants,
            selectedVariant = selectedVariant,
            collectionCounts = collectionCounts,
            onNavigateBack = onNavigateBack,
            onVariantSelected = viewModel::selectVariant,
            memberFolderNames = folders.filter { it.id in memberFolderIds }.map { it.name },
            showFolders = folders.isNotEmpty(),
            onEditFolders = { folderSheet = FolderSheetMode.Edit },
            // Without any collections there is nothing to choose, so add straight away.
            onAddOrIncrement = { if (folders.isEmpty()) viewModel.addOrIncrement() else folderSheet = FolderSheetMode.Add },
            onRemoveOne = viewModel::removeOne
        )
    }

    folderSheet?.let { mode ->
        FolderPickerSheet(
            title = stringResource(
                if (mode == FolderSheetMode.Add) R.string.detail_add_to_title else R.string.detail_folders_title,
                selectedVariant
            ),
            confirmLabel = stringResource(if (mode == FolderSheetMode.Add) R.string.detail_add_confirm else R.string.folder_save),
            folders = folders,
            initiallySelected = memberFolderIds,
            accent = accent,
            onCreateFolder = viewModel::createFolder,
            onConfirm = { ids ->
                if (mode == FolderSheetMode.Add) viewModel.addToCollection(ids) else viewModel.saveFolders(ids)
                folderSheet = null
            },
            onDismiss = { folderSheet = null }
        )
    }
}

private enum class FolderSheetMode { Add, Edit }

@Composable
private fun CardDetailContent(
    card: Card,
    cardDetails: TcgdexCardDto?,
    pricing: TcgdexCardmarketDto?,
    accent: Color,
    availableVariants: List<String>,
    selectedVariant: String,
    collectionCounts: Map<String, Int>,
    onNavigateBack: () -> Unit,
    onVariantSelected: (String) -> Unit,
    memberFolderNames: List<String>,
    showFolders: Boolean,
    onEditFolders: () -> Unit,
    onAddOrIncrement: () -> Unit,
    onRemoveOne: () -> Unit
) {
    val rarity = cardDetails?.rarity ?: card.rarity
    val setName = cardDetails?.set?.name ?: card.setName
    val selectedCount = collectionCounts[selectedVariant] ?: 0
    val foil = isFoilVariant(selectedVariant)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        FilledIconButton(
            onClick = onNavigateBack,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = stringResource(R.string.detail_back))
        }

        HoloCard(
            card = card,
            foil = foil,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(0.84f)
                .aspectRatio(0.72f)
                .align(Alignment.CenterHorizontally)
        )

        Text(
            text = card.name,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 28.dp)
        )
        Text(
            text = "$setName · ${card.number}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (rarity != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(painterResource(R.drawable.ic_star_fill), contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Text(rarity, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (showFolders && selectedCount > 0) {
            FoldersLine(names = memberFolderNames, accent = accent, onEdit = onEditFolders)
        }

        if (availableVariants.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 20.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                availableVariants.forEach { variant ->
                    VariantPill(
                        label = variant,
                        count = collectionCounts[variant] ?: 0,
                        selected = variant == selectedVariant,
                        accent = accent,
                        onClick = { onVariantSelected(variant) }
                    )
                }
            }
        }

        pricing?.let { PriceBlock(it, foil, Modifier.padding(top = 24.dp)) }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp)
        ) {
            if (selectedCount > 0) {
                OutlinedIconButton(
                    onClick = onRemoveOne,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_minus), contentDescription = stringResource(R.string.detail_remove_one))
                }
            }
            Button(
                onClick = onAddOrIncrement,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Ink),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text(
                    text = stringResource(if (selectedCount > 0) R.string.detail_add_more else R.string.detail_add_to_collection),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/** "In: EX und V, Favoriten · Edit" — where the selected variant is filed. */
@Composable
private fun FoldersLine(names: List<String>, accent: Color, onEdit: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .padding(top = 6.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onEdit)
            .padding(vertical = 4.dp)
    ) {
        Icon(painterResource(R.drawable.ic_cards), contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Text(
            text = if (names.isEmpty()) stringResource(R.string.detail_in_no_folder)
            else stringResource(R.string.detail_in_folders, names.joinToString(", ")),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(stringResource(R.string.detail_edit_folders), style = MaterialTheme.typography.labelMedium, color = accent)
    }
}

@Composable
private fun VariantPill(label: String, count: Int, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, if (selected) accent else Color.Transparent)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (count > 0) {
                Text("×$count", style = MaterialTheme.typography.labelLarge, color = accent)
            }
        }
    }
}

@Composable
private fun PriceBlock(cm: TcgdexCardmarketDto, foil: Boolean, modifier: Modifier = Modifier) {
    // Foil variants trade at their own price, so follow the selected variant when TCGdex has it.
    // TCGdex reports "no data" as 0, which must never show up as a real price.
    val useHolo = foil && cm.trendHolo.orNone() != null
    val trend = (if (useHolo) cm.trendHolo else cm.trend).orNone()
    val average = (if (useHolo) cm.avgHolo else cm.avg30).orNone()
    val low = (if (useHolo) cm.lowHolo else cm.low).orNone()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.detail_price_trend),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = trend?.let(::formatEur) ?: "—",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                average?.let { PriceStat(stringResource(R.string.detail_price_avg30), it) }
                low?.let { PriceStat(stringResource(R.string.detail_price_low), it) }
            }
            cm.updated?.let { updated ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.detail_updated, updated.take(10)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun Double?.orNone(): Double? = this?.takeIf { it > 0.0 }

@Composable
private fun PriceStat(label: String, price: Double) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatEur(price), style = MaterialTheme.typography.titleMedium)
    }
}
