package com.yeromich.pokecollect.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yeromich.pokecollect.R
import com.yeromich.pokecollect.domain.model.Folder
import com.yeromich.pokecollect.ui.theme.Ink
import kotlinx.coroutines.launch

private val ROW_HEIGHT = 52.dp
private val LIST_MAX_HEIGHT = 360.dp

/**
 * Lets the user pick which of their collections a card variant goes into.
 * "All cards" is always ticked: every added card is part of the full collection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerSheet(
    title: String,
    confirmLabel: String,
    folders: List<Folder>,
    initiallySelected: Set<Long>,
    accent: Color,
    onCreateFolder: suspend (String) -> Long,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(initiallySelected) }
    var naming by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val checkboxColors = CheckboxDefaults.colors(checkedColor = accent, checkmarkColor = Ink)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            Column(
                modifier = Modifier
                    .heightIn(max = LIST_MAX_HEIGHT)
                    .verticalScroll(rememberScrollState())
            ) {
                PickerRow(
                    label = stringResource(R.string.collection_all_cards),
                    trailing = stringResource(R.string.folder_always),
                    checked = true,
                    enabled = false,
                    colors = checkboxColors,
                    onToggle = {}
                )
                folders.forEach { folder ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    PickerRow(
                        label = folder.name,
                        trailing = folder.entries.toString(),
                        checked = folder.id in selected,
                        enabled = true,
                        colors = checkboxColors,
                        onToggle = { checked -> selected = if (checked) selected + folder.id else selected - folder.id }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        .toggleable(value = false, role = Role.Button, onValueChange = { naming = true })
                ) {
                    Icon(painterResource(R.drawable.ic_plus), contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.folder_new_title), style = MaterialTheme.typography.bodyLarge, color = accent)
                }
            }
            Button(
                onClick = { onConfirm(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Ink),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(52.dp)
            ) {
                Text(confirmLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (naming) {
        FolderNameDialog(
            title = stringResource(R.string.folder_new_title),
            confirmLabel = stringResource(R.string.folder_create),
            initialName = "",
            onConfirm = { name ->
                naming = false
                // A collection created from here is meant for this card, so tick it straight away.
                scope.launch { selected = selected + onCreateFolder(name) }
            },
            onDismiss = { naming = false }
        )
    }
}

@Composable
private fun PickerRow(
    label: String,
    trailing: String,
    checked: Boolean,
    enabled: Boolean,
    colors: androidx.compose.material3.CheckboxColors,
    onToggle: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onToggle)
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled, colors = colors)
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
