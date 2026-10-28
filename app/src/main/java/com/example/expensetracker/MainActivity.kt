package com.example.expensetracker

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private var income = 0.0
    private var expense = 0.0

    private lateinit var preferences: SharedPreferences

    private lateinit var tvBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var transactionContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = getSharedPreferences("expense_data", MODE_PRIVATE)

        income = preferences.getFloat("income", 0f).toDouble()
        expense = preferences.getFloat("expense", 0f).toDouble()

        setContentView(R.layout.activity_main)

        tvBalance = findViewById(R.id.tvBalance)
        tvIncome = findViewById(R.id.tvIncome)
        tvExpense = findViewById(R.id.tvExpense)
        transactionContainer = findViewById(R.id.transactionContainer)

        loadTransactions()

        val btnAddTransaction =
            findViewById<Button>(R.id.btnAddTransaction)

        btnAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }
    }

    private fun showAddTransactionDialog() {

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 10, 40, 10)

        val titleInput = EditText(this)
        titleInput.hint = "Transaction title"

        val amountInput = EditText(this)
        amountInput.hint = "Amount"
        amountInput.inputType = 2

        val typeSpinner = Spinner(this)

        val types = arrayOf("Expense", "Income")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            types
        )

        typeSpinner.adapter = adapter

        layout.addView(titleInput)
        layout.addView(amountInput)
        layout.addView(typeSpinner)

        AlertDialog.Builder(this)
            .setTitle("Add Transaction")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->

                val title = titleInput.text.toString()
                val amount = amountInput.text.toString().toDoubleOrNull()
                val type = typeSpinner.selectedItem.toString()

                if (title.isNotEmpty() && amount != null && amount > 0) {

                    if (type == "Income") {
                        income += amount
                    } else {
                        expense += amount
                    }

                    preferences.edit()
                        .putFloat("income", income.toFloat())
                        .putFloat("expense", expense.toFloat())
                        .apply()

                    addTransactionToList(title, amount, type)
                    saveTransaction(title, amount, type)
                    updateSummary()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveTransaction(
        title: String,
        amount: Double,
        type: String
    ) {
        val transactions = JSONArray(
            preferences.getString("transactions", "[]")
        )

        val transaction = JSONObject()
        transaction.put("title", title)
        transaction.put("amount", amount)
        transaction.put("type", type)

        transactions.put(transaction)

        preferences.edit()
            .putString("transactions", transactions.toString())
            .apply()
    }

    private fun loadTransactions() {
        val transactions = JSONArray(
            preferences.getString("transactions", "[]")
        )

        for (i in 0 until transactions.length()) {
            val transaction = transactions.getJSONObject(i)

            val title = transaction.getString("title")
            val amount = transaction.getDouble("amount")
            val type = transaction.getString("type")

            addTransactionToList(title, amount, type)
        }
    }

    private fun deleteTransaction(
        title: String,
        amount: Double,
        type: String
    ) {
        val oldTransactions = JSONArray(
            preferences.getString("transactions", "[]")
        )

        val newTransactions = JSONArray()

        for (i in 0 until oldTransactions.length()) {
            val transaction = oldTransactions.getJSONObject(i)

            val sameTitle = transaction.getString("title") == title
            val sameAmount = transaction.getDouble("amount") == amount
            val sameType = transaction.getString("type") == type

            if (!(sameTitle && sameAmount && sameType)) {
                newTransactions.put(transaction)
            }
        }

        preferences.edit()
            .putString("transactions", newTransactions.toString())
            .apply()
    }

    private fun addTransactionToList(
        title: String,
        amount: Double,
        type: String
    ) {

        val textView = TextView(this)

        val sign = if (type == "Income") "+" else "-"

        textView.text =
            "$title        $sign ₹${String.format("%.2f", amount)}"

        textView.textSize = 17f
        textView.setPadding(10, 15, 10, 15)

        // Long press karke transaction delete kar sakte hain
        textView.setOnLongClickListener {

            AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Delete \"$title\"?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->

                    // Amount ko total se minus karna
                    if (type == "Income") {
                        income -= amount
                    } else {
                        expense -= amount
                    }

                    preferences.edit()
                        .putFloat("income", income.toFloat())
                        .putFloat("expense", expense.toFloat())
                        .apply()

                    // Screen se transaction remove
                    transactionContainer.removeView(textView)

                    deleteTransaction(title, amount, type)

                    // Summary update
                    updateSummary()
                }
                .show()

            true
        }

        transactionContainer.addView(textView)
    }

    private fun updateSummary() {

        val balance = income - expense

        tvIncome.text =
            "₹ ${String.format("%.2f", income)}"

        tvExpense.text =
            "₹ ${String.format("%.2f", expense)}"

        tvBalance.text =
            "₹ ${String.format("%.2f", balance)}"
    }
}