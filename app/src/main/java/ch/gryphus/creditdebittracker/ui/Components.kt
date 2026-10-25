@file:OptIn(ExperimentalMaterial3Api::class)

package ch.gryphus.creditdebittracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.dp
import ch.gryphus.creditdebittracker.data.CurrencyManager
import ch.gryphus.creditdebittracker.data.ThemeMode

/** Convenience: a remembered boolean flag, default false. */
@Composable
fun rememberBool(initial: Boolean = false): MutableState<Boolean> =
    remember { mutableStateOf(initial) }

/**
 * A minimal single-field prompt dialog. [numeric] switches the keyboard to a
 * decimal pad. Confirm is disabled until the field is non-blank.
 *
 * Optionally shows a trailing [actionIcon] inside the field (e.g. "pick from
 * contacts"); tapping it invokes [onAction]. [prefill] lets the caller push a
 * value into the field after the dialog is open (e.g. the name returned by the
 * contact picker) - each new non-null value replaces the field contents.
 */
@Composable
fun TextPromptDialog(
    title: String,
    label: String,
    confirmLabel: String,
    numeric: Boolean = false,
    initial: String = "",
    prefill: String? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onAction: (() -> Unit)? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }

    // Push an externally-supplied value (e.g. a picked contact's name) into the
    // field whenever it changes.
    LaunchedEffect(prefill) {
        if (prefill != null) text = prefill
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(label) },
                keyboardOptions = if (numeric) {
                    KeyboardOptions(keyboardType = KeyboardType.Decimal)
                } else {
                    KeyboardOptions.Default
                },
                trailingIcon = if (actionIcon != null && onAction != null) {
                    {
                        IconButton(onClick = onAction) {
                            Icon(actionIcon, contentDescription = actionDescription)
                        }
                    }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { onConfirm(text) },
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Theme selection dialog: System / Light / Dark. */
@Composable
fun ThemeDialog(
    current: ThemeMode,
    onPick: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appearance") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = mode == current, onClick = { onPick(mode) })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == current, onClick = { onPick(mode) })
                        Text(mode.label, Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

/** Currency selection dialog listing all supported currencies. */
@Composable
fun CurrencyDialog(
    onPick: (CurrencyManager.Currency) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Currency") },
        text = {
            LazyColumn {
                items(CurrencyManager.supported, key = { it.code }) { c ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = false, onClick = { onPick(c) })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = false, onClick = { onPick(c) })
                        Column(Modifier.padding(start = 8.dp)) {
                            Text("${c.code} · ${c.symbol.trim()}")
                            Text(c.name, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
