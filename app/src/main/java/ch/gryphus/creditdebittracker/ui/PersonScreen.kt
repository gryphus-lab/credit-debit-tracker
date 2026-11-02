@file:OptIn(ExperimentalMaterial3Api::class)

package ch.gryphus.creditdebittracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.gryphus.creditdebittracker.LedgerViewModel
import ch.gryphus.creditdebittracker.data.Entry
import ch.gryphus.creditdebittracker.data.Money
import java.text.DateFormat
import java.util.Date

@Composable
fun PersonScreen(
    vm: LedgerViewModel,
    personId: Long,
    onBack: () -> Unit,
) {
    val person by vm.person(personId).collectAsStateWithLifecycle(initialValue = null)
    val entries by vm.entriesFor(personId).collectAsStateWithLifecycle(initialValue = emptyList())
    val symbol by vm.currencySymbol.collectAsStateWithLifecycle()

    // Outstanding balance for this person (unsettled entries only).
    val balance = entries.sumOf { it.signedOutstanding }

    var showAdd by rememberBool()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person?.name ?: "…", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Entry") },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BalanceHeader(balanceCents = balance, symbol = symbol)

            if (entries.isEmpty()) {
                Text(
                    "No entries yet. Tap “Entry” to record a credit or debit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(entries, key = { it.id }) { entry ->
                        EntryRow(
                            entry = entry,
                            symbol = symbol,
                            onToggle = { vm.toggleSettled(entry) },
                            onDelete = { vm.deleteEntry(entry) },
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddEntryDialog(
            onConfirm = { note, cents, isCredit, entryDate ->
                vm.addEntry(personId, note, cents, isCredit, entryDate)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun BalanceHeader(balanceCents: Long, symbol: String) {
    val (label, color) = when {
        balanceCents > 0 -> "They owe you" to MaterialTheme.colorScheme.primary
        balanceCents < 0 -> "You owe them" to MaterialTheme.colorScheme.secondary
        else -> "All settled" to MaterialTheme.colorScheme.onSurface
    }
    Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = color)
            Text(
                Money.format(balanceCents, symbol),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
private fun EntryRow(
    entry: Entry,
    symbol: String,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val amountColor = when {
        entry.settled -> MaterialTheme.colorScheme.onSurfaceVariant
        entry.isCredit -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }
    val signed = if (entry.isCredit) entry.amountCents else -entry.amountCents
    var confirmDelete by remember { mutableStateOf(false) }

    Card(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    entry.note.ifBlank { if (entry.isCredit) "Credit" else "Debit" },
                    textDecoration = if (entry.settled) TextDecoration.LineThrough else null,
                    fontWeight = FontWeight.Medium,
                )
            },
            supportingContent = {
                val status = if (entry.settled) "Settled · tap to reopen" else "Tap to mark settled"
                Text("${formatEntryDate(entry.entryDate)} · $status")
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Money.format(signed, symbol),
                        color = amountColor,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (entry.settled) TextDecoration.LineThrough else null,
                    )
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete entry",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
        )
    }

    if (confirmDelete) {
        val label = entry.note.ifBlank { if (entry.isCredit) "Credit" else "Debit" }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete entry?") },
            text = { Text("“$label” (${Money.format(signed, symbol)}) will be removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AddEntryDialog(
    onConfirm: (note: String, cents: Long, isCredit: Boolean, entryDate: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var isCredit by rememberSaveable { mutableStateOf(true) }
    // Date is mandatory: no default is pre-selected, so the user must pick one
    // before "Add" is enabled. Null until chosen.
    var entryDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberBool()

    val cents = Money.parseToCents(amount)
    val valid = cents != null && cents > 0 && entryDate != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isCredit,
                        onClick = { isCredit = true },
                        label = { Text("They owe you") },
                    )
                    FilterChip(
                        selected = !isCredit,
                        onClick = { isCredit = false },
                        label = { Text("You owe them") },
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    singleLine = true,
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amount.isNotBlank() && cents == null,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    singleLine = true,
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                // Mandatory date field: a button that opens the calendar picker
                // and shows the chosen date (or a prompt until one is selected).
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Text(
                        text = entryDate?.let { "Date: ${formatEntryDate(it)}" } ?: "Pick a date *",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onConfirm(note, cents ?: 0L, isCredit, entryDate ?: 0L) },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = entryDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedDateMillis != null,
                    onClick = {
                        entryDate = state.selectedDateMillis
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

/** Format an epoch-millis date for display, using the device's locale. */
private fun formatEntryDate(epochMillis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMillis))
