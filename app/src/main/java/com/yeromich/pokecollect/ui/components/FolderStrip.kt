package com.yeromich.pokecollect.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yeromich.pokecollect.R
import com.yeromich.pokecollect.domain.model.Folder
import com.yeromich.pokecollect.domain.model.formatEur

private val TILE_WIDTH = 108.dp
private val STACK_HEIGHT = 46.dp
private val STACK_CARD_WIDTH = 30.dp
private val STACK_STEP = 18.dp
private val STACK_TILT = listOf(-8f, 0f, 8f)
private val STACK_DROP = listOf(3.dp, 0.dp, 3.dp)

/**
 * Horizontal row of collections: "All cards" first, then the user's folders, then "+ New".
 * Long-pressing a folder offers rename and delete.
 */
@Composable
fun FolderStrip(
    folders: List<Folder>,
    selectedFolderId: Long?,
    allCount: Int,
    allTotalEur: Double,
    allPreviewImages: List<String>,
    onSelect: (Long?) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Folder?>(null) }
    var deleting by remember { mutableStateOf<Folder?>(null) }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = modifier
    ) {
        item(key = "all") {
            FolderTile(
                name = stringResource(R.string.collection_all_cards),
                entries = allCount,
                totalEur = allTotalEur,
                previewImages = allPreviewImages,
                selected = selectedFolderId == null,
                onClick = { onSelect(null) }
            )
        }
        items(folders, key = { it.id }) { folder ->
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                FolderTile(
                    name = folder.name,
                    entries = folder.entries,
                    totalEur = folder.totalEur,
                    previewImages = folder.previewImages,
                    selected = selectedFolderId == folder.id,
                    onClick = { onSelect(folder.id) },
                    onLongClick = { menuOpen = true }
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.folder_rename)) },
                        onClick = { menuOpen = false; renaming = folder }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.folder_delete), color = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; deleting = folder }
                    )
                }
            }
        }
        item(key = "new") { NewFolderTile(onClick = { creating = true }) }
    }

    if (creating) {
        FolderNameDialog(
            title = stringResource(R.string.folder_new_title),
            confirmLabel = stringResource(R.string.folder_create),
            initialName = "",
            onConfirm = { onCreate(it); creating = false },
            onDismiss = { creating = false }
        )
    }
    renaming?.let { folder ->
        FolderNameDialog(
            title = stringResource(R.string.folder_rename_title),
            confirmLabel = stringResource(R.string.folder_save),
            initialName = folder.name,
            onConfirm = { onRename(folder.id, it); renaming = null },
            onDismiss = { renaming = null }
        )
    }
    deleting?.let { folder ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.folder_delete_title, folder.name)) },
            text = { Text(stringResource(R.string.folder_delete_body)) },
            confirmButton = {
                TextButton(onClick = { onDelete(folder.id); deleting = null }) {
                    Text(stringResource(R.string.folder_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.folder_cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderTile(
    name: String,
    entries: Int,
    totalEur: Double,
    previewImages: List<String>,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.5.dp, if (selected) colors.primary else Color.Transparent),
        modifier = Modifier
            .width(TILE_WIDTH)
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(role = Role.Tab, onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(Modifier.padding(10.dp)) {
            CardStack(previewImages)
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = stringResource(R.string.folder_meta, entries, formatEur(totalEur)),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** Up to three card images fanned out like a small hand, or an empty card outline. */
@Composable
private fun CardStack(images: List<String>) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(STACK_HEIGHT)
    ) {
        if (images.isEmpty()) {
            Box(
                modifier = Modifier
                    .width(STACK_CARD_WIDTH)
                    .fillMaxHeight()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            )
        }
        images.forEachIndexed { index, url ->
            val slot = if (images.size == 1) 1 else index
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = STACK_STEP * index, y = STACK_DROP[slot])
                    .rotate(STACK_TILT[slot])
                    .width(STACK_CARD_WIDTH)
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun NewFolderTile(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        modifier = Modifier
            .width(72.dp)
            .height(104.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, colors.outline, MaterialTheme.shapes.medium)
            .background(Color.Transparent)
            .combinedClickable(role = Role.Button, onClick = onClick)
    ) {
        Icon(painterResource(R.drawable.ic_plus), contentDescription = null, tint = colors.primary)
        Text(stringResource(R.string.folder_new), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    }
}

@Composable
fun FolderNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.folder_name_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.folder_cancel)) } }
    )
}
