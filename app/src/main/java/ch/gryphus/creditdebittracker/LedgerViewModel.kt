package ch.gryphus.creditdebittracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.gryphus.creditdebittracker.data.CurrencyManager
import ch.gryphus.creditdebittracker.data.Entry
import ch.gryphus.creditdebittracker.data.LedgerRepository
import ch.gryphus.creditdebittracker.data.Person
import ch.gryphus.creditdebittracker.data.PersonWithBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LedgerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = LedgerRepository(app)

    private val _currencySymbol = MutableStateFlow(CurrencyManager.symbol(app))
    val currencySymbol: StateFlow<String> = _currencySymbol

    val persons: StateFlow<List<PersonWithBalance>> =
        repo.personsWithBalance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Total owed to you (sum of positive balances), in cents. */
    val totalCredit: StateFlow<Long> =
        persons.map { list -> list.filter { it.balanceCents > 0 }.sumOf { it.balanceCents } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** Total you owe (magnitude of negative balances), in cents. */
    val totalDebit: StateFlow<Long> =
        persons.map { list -> list.filter { it.balanceCents < 0 }.sumOf { -it.balanceCents } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun person(id: Long): Flow<Person?> = repo.person(id)
    fun entriesFor(id: Long): Flow<List<Entry>> = repo.entriesFor(id)

    fun setCurrency(currency: CurrencyManager.Currency) {
        CurrencyManager.setCurrency(getApplication(), currency)
        _currencySymbol.value = currency.symbol
    }

    fun addPerson(name: String) = viewModelScope.launch { repo.addPerson(name) }
    fun deletePerson(person: Person) = viewModelScope.launch { repo.deletePerson(person) }

    fun addEntry(personId: Long, note: String, amountCents: Long, isCredit: Boolean) =
        viewModelScope.launch { repo.addEntry(personId, note, amountCents, isCredit) }

    fun toggleSettled(entry: Entry) = viewModelScope.launch { repo.toggleSettled(entry) }
    fun deleteEntry(entry: Entry) = viewModelScope.launch { repo.deleteEntry(entry) }
}
