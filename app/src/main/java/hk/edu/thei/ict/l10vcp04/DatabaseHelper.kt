package hk.edu.thei.ict.l10vcp04.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * DatabaseHelper manages SQLite storage for Bingo rounds, storing username,
 * round number, 25 card numbers, user selection states, drawn numbers, bingo status, and timestamp.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "bingo_master.db"
        private const val DATABASE_VERSION = 5 // Incremented version for drawn numbers column

        const val TABLE_NAME = "bingo_rounds"
        const val COLUMN_ID = "_id"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_ROUND_NUMBER = "round_number"
        const val COLUMN_BINGO_CARD = "bingo_card"
        const val COLUMN_SELECTED_MASK = "selected_mask"
        const val COLUMN_DRAWN_NUMBERS = "drawn_numbers" // Stores comma-separated drawn numbers
        const val COLUMN_IS_BINGO = "is_bingo"
        const val COLUMN_BINGO_DETAILS = "bingo_details"
        const val COLUMN_SYSTEM_TIME = "system_time"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = ("CREATE TABLE $TABLE_NAME ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_USERNAME TEXT, "
                + "$COLUMN_ROUND_NUMBER INTEGER, "
                + "$COLUMN_BINGO_CARD TEXT, "
                + "$COLUMN_SELECTED_MASK TEXT, "
                + "$COLUMN_DRAWN_NUMBERS TEXT, "
                + "$COLUMN_IS_BINGO INTEGER, "
                + "$COLUMN_BINGO_DETAILS TEXT, "
                + "$COLUMN_SYSTEM_TIME TEXT)")
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    /**
     * Inserts a complete game round record into SQLite including drawn numbers.
     */
    fun insertGameRound(
        username: String,
        roundNumber: Int,
        cardNumbers: String,
        selectedMask: String,
        drawnNumbers: String,
        isBingo: Int,
        bingoDetails: String,
        systemTime: String
    ): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_ROUND_NUMBER, roundNumber)
            put(COLUMN_BINGO_CARD, cardNumbers)
            put(COLUMN_SELECTED_MASK, selectedMask)
            put(COLUMN_DRAWN_NUMBERS, drawnNumbers)
            put(COLUMN_IS_BINGO, isBingo)
            put(COLUMN_BINGO_DETAILS, bingoDetails)
            put(COLUMN_SYSTEM_TIME, systemTime)
        }
        val result = db.insert(TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    fun getAllRecords(): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC", null)
    }

    fun deleteRecord(id: Long): Int {
        val db = this.writableDatabase
        val result = db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
        return result
    }

    fun clearAllRecords() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM $TABLE_NAME")
        db.close()
    }
}