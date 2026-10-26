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
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.gryphus.creditdebittracker.LedgerViewModel
import ch.gryphus.creditdebittracker.data.ContactName
import ch.gryphus.creditdebittracker.data.Money
import ch.gryphus.creditdebittracker.data.PersonWithBalance

@Composable
fun HomeScreen(
    vm: LedgerViewModel,
    onOpenPerson: (Long) -> Unit,
) {
    val persons by vm.persons.collectAsStateWithLifecycle()
    val credit by vm.totalCredit.collectAsStateWithLifecycle()
    val debit by vm.totalDebit.collectAsStateWithLifecycle()
    val symbol by vm.currencySymbol.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()

    var showAddPerson by rememberBool()
    var showCurrency by rememberBool()
    var showTheme by rememberBool()
    var showBackup by rememberBool()

    // Name pushed into the Add-person field when a contact is picked.
    val context = LocalContext.current
    var pickedName by remember { mutableStateOf<String?>(null) }

    // One-shot backup/restore result messages shown via snackbar.
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    // Local JSON export: user picks where to create the file, we write into it.
    val exportFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ch.gryphus.creditdebittracker.data.backup.BackupSerializer.MIME_TYPE),
    ) { uri ->
        if (uri != null) {
            vm.exportToStream { json ->
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    ?: error("Couldn't open the chosen file")
            }
        }
    }
    // Local JSON import: user picks a backup file, we read + restore it.
    val importFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
            if (text != null) vm.importFromText(text)
        }
    }

    // Google Sign-In for Drive; pendingDriveAction is run once we have an account.
    var pendingDriveBackup by remember { mutableStateOf(false) }
    val driveSignIn = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val account = vm.drive.accountFromResult(result.data)
        if (account != null) {
            if (pendingDriveBackup) vm.backupToDrive(account) else vm.restoreFromDrive(account)
        }
    }
    fun driveAction(backup: Boolean) {
        pendingDriveBackup = backup
        val existing = vm.drive.lastAccount()
        if (existing != null) {
            if (backup) vm.backupToDrive(existing) else vm.restoreFromDrive(existing)
        } else {
            driveSignIn.launch(vm.drive.signInIntent())
        }
    }

    // System contact picker: returns a contact URI; we read its full name.
    val pickContact = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact(),
    ) { uri ->
        if (uri != null) pickedName = ContactName.resolve(context, uri)
    }
    // READ_CONTACTS is needed to read the picked contact's name; request on tap.
    val requestContactsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) pickContact.launch(null) }

    fun pickContactName() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            pickContact.launch(null)
        } else {
            requestContactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Credit · Debit", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showBackup = true }) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Backup & restore")
                    }
                    IconButton(onClick = { showTheme = true }) {
                        Icon(
                            Icons.Default.BrightnessMedium,
                            contentDescription = "Appearance",
                        )
                    }
                    TextButton(onClick = { showCurrency = true }) { Text(symbol.trim()) }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddPerson = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Person") },
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
            SummaryRow(creditCents = credit, debitCents = debit, symbol = symbol)

            if (persons.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(persons, key = { it.person.id }) { pwb ->
                        PersonRow(pwb, symbol, onClick = { onOpenPerson(pwb.person.id) })
                    }
                }
            }
        }
    }

    if (showAddPerson) {
        TextPromptDialog(
            title = "Add person",
            label = "Name",
            confirmLabel = "Add",
            prefill = pickedName,
            actionIcon = Icons.Default.Contacts,
            actionDescription = "Pick from contacts",
            onAction = { pickContactName() },
            onConfirm = { name ->
                vm.addPerson(name)
                showAddPerson = false
                pickedName = null
            },
            onDismiss = {
                showAddPerson = false
                pickedName = null
            },
        )
    }
    if (showCurrency) {
        CurrencyDialog(
            onPick = { vm.setCurrency(it); showCurrency = false },
            onDismiss = { showCurrency = false },
        )
    }
    if (showTheme) {
        ThemeDialog(
            current = themeMode,
            onPick = { vm.setThemeMode(it); showTheme = false },
            onDismiss = { showTheme = false },
        )
    }
    if (showBackup) {
        BackupDialog(
            onExport = { showBackup = false; exportFile.launch(vm.suggestedFileName()) },
            onImport = {
                showBackup = false
                importFile.launch(arrayOf(ch.gryphus.creditdebittracker.data.backup.BackupSerializer.MIME_TYPE, "text/*"))
            },
            onDriveBackup = { showBackup = false; driveAction(backup = true) },
            onDriveRestore = { showBackup = false; driveAction(backup = false) },
            onDismiss = { showBackup = false },
        )
    }
}

@Composable
private fun SummaryRow(creditCents: Long, debitCents: Long, symbol: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryCard(
            label = "Owed to you",
            amount = Money.format(creditCents, symbol),
            container = MaterialTheme.colorScheme.primary,
            onColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            label = "You owe",
            amount = Money.format(debitCents, symbol),
            container = MaterialTheme.colorScheme.secondary,
            onColor = Color.White,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: String,
    container: Color,
    onColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = onColor, style = MaterialTheme.typography.labelLarge)
            Text(
                amount,
                color = onColor,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PersonRow(pwb: PersonWithBalance, symbol: String, onClick: () -> Unit) {
    val balance = pwb.balanceCents
    val (text, color) = when {
        balance > 0 -> Money.format(balance, symbol) to MaterialTheme.colorScheme.primary
        balance < 0 -> Money.format(balance, symbol) to MaterialTheme.colorScheme.secondary
        else -> "settled" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(pwb.person.name, fontWeight = FontWeight.Medium) },
            trailingContent = { Text(text, color = color, fontWeight = FontWeight.Bold) },
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Payments,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "No people yet. Tap “Person” to add someone you lend to or borrow from.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
        )
    }
}
