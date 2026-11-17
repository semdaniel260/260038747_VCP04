package hk.edu.thei.ict.l10vcp04

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import hk.edu.thei.ict.l10vcp04.database.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * MainActivity handles 5x5 card generation, manual/auto ticking modes,
 * auto-roll on save, round 1 reset, and SQLite persistence.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var tvRoundNumber: TextView
    private lateinit var tvBingoAlert: TextView
    private lateinit var tvLastDrawn: TextView
    private lateinit var tvDrawnHistory: TextView
    private lateinit var toggleGameMode: ToggleButton
    private lateinit var btnDrawNumber: Button
    private lateinit var btnGenerateCard: Button
    private lateinit var btnSaveRound: Button
    private lateinit var btnResetGame: Button
    private lateinit var btnViewHistory: Button
    private lateinit var tvStatus: TextView

    private lateinit var databaseHelper: DatabaseHelper
    private var currentRound = 1

    private val cardCells = arrayOfNulls<TextView>(25)
    private val cardValues = Array(25) { "" }
    private val selectedStates = BooleanArray(25) { false }
    private val drawnNumbers = mutableSetOf<Int>()
    private val drawnHistoryList = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        databaseHelper = DatabaseHelper(this)

        etUsername = findViewById(R.id.etUsername)
        tvRoundNumber = findViewById(R.id.tvRoundNumber)
        tvBingoAlert = findViewById(R.id.tvBingoAlert)
        tvLastDrawn = findViewById(R.id.tvLastDrawn)
        tvDrawnHistory = findViewById(R.id.tvDrawnHistory)
        toggleGameMode = findViewById(R.id.toggleGameMode)
        btnDrawNumber = findViewById(R.id.btnDrawNumber)
        btnGenerateCard = findViewById(R.id.btnGenerateCard)
        btnSaveRound = findViewById(R.id.btnSaveRound)
        btnResetGame = findViewById(R.id.btnResetGame)
        btnViewHistory = findViewById(R.id.btnViewHistory)
        tvStatus = findViewById(R.id.tvStatus)

        // Bind all 25 cells and handle manual clicks
        for (i in 0 until 25) {
            val resId = resources.getIdentifier("cell_$i", "id", packageName)
            val cell = findViewById<TextView>(resId)
            cardCells[i] = cell

            val index = i
            cell?.setOnClickListener {
                if (toggleGameMode.isChecked) {
                    Toast.makeText(this, "Auto Mode is ON: System ticks automatically!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val cellNumberStr = cardValues[index]
                val cellNumber = cellNumberStr.toIntOrNull()

                if (cellNumber != null && drawnNumbers.contains(cellNumber)) {
                    selectedStates[index] = !selectedStates[index]
                    updateCellUI(index)
                    evaluateBingo()
                } else {
                    Toast.makeText(this, "Number $cellNumber has not been drawn yet!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        generateNewCard()

        btnDrawNumber.setOnClickListener {
            drawNextNumber()
        }

        btnGenerateCard.setOnClickListener {
            generateNewCard()
        }

        btnSaveRound.setOnClickListener {
            saveCurrentRound()
        }

        btnResetGame.setOnClickListener {
            currentRound = 1
            tvRoundNumber.text = "Current Round: $currentRound"
            generateNewCard()
            Toast.makeText(this, "Game reset to Round 1!", Toast.LENGTH_SHORT).show()
        }

        btnViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun drawNextNumber() {
        val availableNumbers = cardValues.mapNotNull { it.toIntOrNull() }.filter { !drawnNumbers.contains(it) }

        if (availableNumbers.isEmpty()) {
            Toast.makeText(this, "All numbers on your card have been drawn!", Toast.LENGTH_SHORT).show()
            return
        }

        val newNum = availableNumbers.random()
        drawnNumbers.add(newNum)
        drawnHistoryList.add(0, newNum)

        tvLastDrawn.text = "Drawn Number: $newNum"
        tvDrawnHistory.text = "History: ${drawnHistoryList.joinToString(", ")}"

        if (toggleGameMode.isChecked) {
            for (i in 0 until 25) {
                if (cardValues[i].toIntOrNull() == newNum) {
                    selectedStates[i] = true
                    updateCellUI(i)
                }
            }
            evaluateBingo()
            tvStatus.text = "Status: Auto-ticked number $newNum!"
        } else {
            tvStatus.text = "Status: Number $newNum drawn. Tap your card manually!"
        }
    }

    private fun generateNewCard() {
        drawnNumbers.clear()
        drawnHistoryList.clear()
        tvLastDrawn.text = "Drawn Number: --"
        tvDrawnHistory.text = "History: None"

        for (i in 0 until 25) {
            selectedStates[i] = false
        }

        for (col in 0 until 5) {
            val min = col * 15 + 1
            val max = min + 15
            val colNums = mutableSetOf<Int>()
            while (colNums.size < 5) {
                colNums.add(Random.nextInt(min, max))
            }
            val list = colNums.toList()

            for (row in 0 until 5) {
                val idx = row * 5 + col
                val num = list[row]
                cardValues[idx] = num.toString()
            }
        }

        for (i in 0 until 25) {
            updateCellUI(i)
        }
        evaluateBingo()
        tvStatus.text = "Status: New card rolled. Ready to play!"
    }

    private fun updateCellUI(index: Int) {
        val cell = cardCells[index] ?: return
        cell.text = cardValues[index]
        if (selectedStates[index]) {
            cell.setBackgroundColor(Color.parseColor("#FF4081"))
            cell.setTextColor(Color.WHITE)
        } else {
            cell.setBackgroundColor(Color.parseColor("#E0E0E0"))
            cell.setTextColor(Color.BLACK)
        }
    }

    private fun evaluateBingo(): Pair<Int, String> {
        var bingoCount = 0
        val winningLines = mutableListOf<String>()

        for (r in 0 until 5) {
            if ((0 until 5).all { c -> selectedStates[r * 5 + c] }) {
                bingoCount++
                winningLines.add("Row ${r + 1}")
            }
        }

        val colNames = arrayOf("B", "I", "N", "G", "O")
        for (c in 0 until 5) {
            if ((0 until 5).all { r -> selectedStates[r * 5 + c] }) {
                bingoCount++
                winningLines.add("Column ${colNames[c]}")
            }
        }

        if ((0 until 5).all { i -> selectedStates[i * 5 + i] }) {
            bingoCount++
            winningLines.add("Diagonal (TL-BR)")
        }
        if ((0 until 5).all { i -> selectedStates[i * 5 + (4 - i)] }) {
            bingoCount++
            winningLines.add("Diagonal (TR-BL)")
        }

        tvBingoAlert.text = "BINGO: $bingoCount"
        val details = if (winningLines.isEmpty()) "No Bingo yet" else winningLines.joinToString(", ")
        return Pair(bingoCount, details)
    }

    private fun saveCurrentRound() {
        val username = etUsername.text.toString().trim()
        if (username.isEmpty()) {
            etUsername.error = "Please enter username!"
            return
        }

        val cardString = cardValues.joinToString(",")
        val maskString = selectedStates.joinToString(",")
        val drawnString = if (drawnHistoryList.isEmpty()) "None" else drawnHistoryList.joinToString(",")
        val (bingoCount, bingoDetails) = evaluateBingo()
        val isBingoInt = if (bingoCount > 0) 1 else 0
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val success = databaseHelper.insertGameRound(
            username,
            currentRound,
            cardString,
            maskString,
            drawnString,
            isBingoInt,
            bingoDetails,
            currentTime
        )

        if (success) {
            Toast.makeText(this, "Round $currentRound saved & new card rolled!", Toast.LENGTH_SHORT).show()
            // Increment round number and automatically generate a new card for the next round
            currentRound++
            tvRoundNumber.text = "Current Round: $currentRound"
            generateNewCard()
        } else {
            Toast.makeText(this, "Failed to save record.", Toast.LENGTH_SHORT).show()
        }
    }
}