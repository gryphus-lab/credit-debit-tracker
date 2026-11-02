package ch.gryphus.creditdebittracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Person::class, Entry::class], version = 2, exportSchema = false)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        @Volatile
        private var instance: LedgerDatabase? = null

        /**
         * v1 -> v2: add the user-chosen [Entry.entryDate]. Existing rows have no
         * such value, so seed it from the audit timestamp (createdAt) - the best
         * available approximation of when the entry happened.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE entry ADD COLUMN entryDate INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL("UPDATE entry SET entryDate = createdAt")
            }
        }

        fun get(context: Context): LedgerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LedgerDatabase::class.java,
                    "ledger.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
