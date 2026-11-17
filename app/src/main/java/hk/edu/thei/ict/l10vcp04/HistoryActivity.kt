package hk.edu.thei.ict.l10vcp04

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import hk.edu.thei.ict.l10vcp04.database.DatabaseHelper

/**
 * Data class representing a saved Bingo round record including drawn numbers.
 */
data class BingoRecord(
    val id: Long,
    val username: String,
    val roundNumber: Int,
    val cardGrid: String,
    val drawnNumbers: String,
    val isBingo: Int,
    val bingoDetails: String,
    val time: String
)

/**
 * HistoryActivity displays saved SQLite records, allows clearing or deleting records,
 * viewing full details via dialog, and returning to the main game.
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var listViewHistory: ListView
    private lateinit var btnClearAll: Button
    private lateinit var btnBackToGame: Button
    private val recordList = mutableListOf<BingoRecord>()
    private lateinit var recordAdapter: RecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        databaseHelper = DatabaseHelper(this)
        listViewHistory = findViewById(R.id.listViewHistory)
        btnClearAll = findViewById(R.id.btnClearAll)
        btnBackToGame = findViewById(R.id.btnBackToGame)

        recordAdapter = RecordAdapter()
        listViewHistory.adapter = recordAdapter

        loadHistoryData()

        // Return to main game activity
        btnBackToGame.setOnClickListener {
            finish()
        }

        btnClearAll.setOnClickListener {
            databaseHelper.clearAllRecords()
            loadHistoryData()
            Toast.makeText(this, "All history cleared!", Toast.LENGTH_SHORT).show()
        }

        listViewHistory.setOnItemClickListener { _, _, position, _ ->
            val record = recordList[position]
            showRecordDetails(record)
        }
    }

    private fun loadHistoryData() {
        recordList.clear()
        val cursor: Cursor = databaseHelper.getAllRecords()

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME))
                val roundNum = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ROUND_NUMBER))
                val cardGrid = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BINGO_CARD))
                val drawnNumbers = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DRAWN_NUMBERS)) ?: "None"
                val isBingo = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_BINGO))
                val details = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BINGO_DETAILS))
                val time = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SYSTEM_TIME))

                recordList.add(BingoRecord(id, username, roundNum, cardGrid, drawnNumbers, isBingo, details, time))
            } while (cursor.moveToNext())
        }
        cursor.close()

        recordAdapter.notifyDataSetChanged()

        if (recordList.isEmpty()) {
            Toast.makeText(this, "No saved records found yet.", Toast.LENGTH_SHORT).show()
        }
    }

    inner class RecordAdapter : BaseAdapter() {
        override fun getCount(): Int = recordList.size
        override fun getItem(position: Int): Any = recordList[position]
        override fun getItemId(position: Int): Long = recordList[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@HistoryActivity)
                .inflate(R.layout.item_history, parent, false)

            val record = recordList[position]

            val tvUserRound = view.findViewById<TextView>(R.id.tvItemUserRound)
            val tvCard = view.findViewById<TextView>(R.id.tvItemCard)
            val tvTime = view.findViewById<TextView>(R.id.tvItemTime)
            val btnDeleteRow = view.findViewById<Button>(R.id.btnDeleteRow)

            tvUserRound.text = "Player: ${record.username} (Round ${record.roundNumber})"
            tvCard.text = if (record.isBingo == 1) "🔥 BINGO ACHIEVED!" else "No Bingo"
            tvTime.text = "Saved: ${record.time}"

            btnDeleteRow.setOnClickListener {
                databaseHelper.deleteRecord(record.id)
                loadHistoryData()
                Toast.makeText(this@HistoryActivity, "Record deleted", Toast.LENGTH_SHORT).show()
            }

            return view
        }
    }

    private fun showRecordDetails(record: BingoRecord) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_history_detail, null)
        val tvUserRound = dialogView.findViewById<TextView>(R.id.tvDetailUserRound)
        val tvBingoStatus = dialogView.findViewById<TextView>(R.id.tvDetailBingoStatus)
        val tvWinningLines = dialogView.findViewById<TextView>(R.id.tvDetailWinningLines)
        val tvDrawnNumbers = dialogView.findViewById<TextView>(R.id.tvDetailDrawnNumbers)
        val tvCardGrid = dialogView.findViewById<TextView>(R.id.tvDetailCardGrid)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseDialog)

        tvUserRound.text = "Player: ${record.username} | Round: ${record.roundNumber}\nSaved: ${record.time}"
        if (record.isBingo == 1) {
            tvBingoStatus.text = "Bingo Status: YES 🎉"
            tvBingoStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            tvBingoStatus.text = "Bingo Status: NO ❌"
            tvBingoStatus.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
        }
        tvWinningLines.text = "Winning Lines: ${record.bingoDetails}"
        tvDrawnNumbers.text = record.drawnNumbers
        tvCardGrid.text = record.cardGrid

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}