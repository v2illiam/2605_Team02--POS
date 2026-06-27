package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.os.Environment
import android.view.Gravity
import android.widget.*
import com.google.firebase.Timestamp
import com.liquor.ledger.firebase.FirebaseManager
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import android.provider.Settings

class SalesReportPage(private val activity: Activity) {

    private val allSales = mutableListOf<SaleRow>()
    private lateinit var reportText: TextView
    private lateinit var filterSpinner: Spinner
    private lateinit var totalTransactionsCard: TextView
    private lateinit var totalSalesCard: TextView
    private lateinit var averageSaleCard: TextView
    private lateinit var filterPeriodCard: TextView

    data class SaleRow(
        val date: Date,
        val total: Double,
        val paymentType: String,
        val employee: String
    )

    fun build(): LinearLayout {
        val root = LinearLayout(activity)
        root.orientation = LinearLayout.VERTICAL

        // Changes root background based on Settings - AF
        root.setBackgroundColor(ThemeManager.pageBackground(activity))

        val title = TextView(activity)
        title.text = "Sales Report"
        title.textSize = 28f
        title.setTypeface(null, android.graphics.Typeface.BOLD)


        // Changes title color based on Settings - AF
        title.setTextColor(ThemeManager.primaryText(activity))

        val subtitle = TextView(activity)
        subtitle.text = "Summary of transactions, revenue, and averages."
        subtitle.textSize = 16f

        // Changes subtitle color based on Settings - AF
        subtitle.setTextColor(ThemeManager.secondaryText(activity))

        subtitle.setPadding(0, 10, 0, 30)

        filterSpinner = Spinner(activity)
        filterSpinner.adapter = ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("30d", "60d", "90d", "YTD", "All")
        )

        val exportReportBtn = TextView(activity)
        exportReportBtn.text = "Export This Report to CSV"
        exportReportBtn.textSize = 15f
        exportReportBtn.gravity = android.view.Gravity.CENTER
        exportReportBtn.setTextColor(Color.WHITE)

        // Changes export report button color based on Settings - AF
        exportReportBtn.setBackgroundColor(ThemeManager.primaryAction(activity))

        exportReportBtn.setPadding(24, 20, 24, 20)
        val exportReportParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        exportReportParams.setMargins(0, 0, 0, 12)
        exportReportBtn.layoutParams = exportReportParams

        val exportFullBtn = TextView(activity)
        exportFullBtn.text = "Full Data Export"
        exportFullBtn.textSize = 15f
        exportFullBtn.gravity = android.view.Gravity.CENTER

        // Changes full export button colors based on Settings - AF
        exportFullBtn.setTextColor(ThemeManager.primaryAction(activity))
        exportFullBtn.setBackgroundColor(ThemeManager.sectionBackground(activity))

        exportFullBtn.setPadding(24, 20, 24, 20)
        val exportFullParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        exportFullParams.setMargins(0, 0, 0, 24)
        exportFullBtn.layoutParams = exportFullParams

        // Changes summary card colors based on Settings - AF
        totalTransactionsCard = makeSummaryCard("Total Transactions", "0", ThemeManager.primaryAction(activity))
        totalSalesCard = makeSummaryCard("Total Sales", "$0.00", ThemeManager.positive(activity))
        averageSaleCard = makeSummaryCard("Average Sale", "$0.00", ThemeManager.warning(activity))
        filterPeriodCard = makeSummaryCard("Period", "All", ThemeManager.primaryAction(activity))

        val summaryRow1 = LinearLayout(activity)
        summaryRow1.orientation = LinearLayout.HORIZONTAL
        summaryRow1.addView(totalTransactionsCard)
        summaryRow1.addView(totalSalesCard)

        val summaryRow2 = LinearLayout(activity)
        summaryRow2.orientation = LinearLayout.HORIZONTAL
        summaryRow2.setPadding(0, 16, 0, 24)
        summaryRow2.addView(averageSaleCard)
        summaryRow2.addView(filterPeriodCard)

        reportText = TextView(activity)
        reportText.textSize = 16f
        reportText.setTextColor(Color.DKGRAY)

        // Changes report text colors based on Settings - AF
        reportText.setTextColor(ThemeManager.secondaryText(activity))
        reportText.setBackgroundColor(ThemeManager.sectionBackground(activity))

        reportText.gravity = Gravity.START

        val scrollView = ScrollView(activity)

        // Changes scroll background based on Settings - AF
        scrollView.setBackgroundColor(ThemeManager.pageBackground(activity))

        val page = LinearLayout(activity)
        page.orientation = LinearLayout.VERTICAL
        page.setPadding(40, 40, 40, 40)

        // Changes page background based on Settings - AF
        page.setBackgroundColor(ThemeManager.pageBackground(activity))

        page.addView(title)
        page.addView(subtitle)
        page.addView(filterSpinner)
        page.addView(exportReportBtn)
        page.addView(exportFullBtn)
        page.addView(summaryRow1)
        page.addView(summaryRow2)
        page.addView(reportText)

        scrollView.addView(page)
        root.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ))

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updateReport()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        exportReportBtn.setOnClickListener {
            exportSales(getFilteredSales(), "sales_report_${filterSpinner.selectedItem}.csv")
        }

        exportFullBtn.setOnClickListener {
            exportSales(allSales, "sales_full_data_export.csv")
        }

        loadSales()

        return root
    }

    private fun loadSales() {
        FirebaseManager.db.collection("sales")
            .get()
            .addOnSuccessListener { result ->
                allSales.clear()

                for (doc in result) {
                    val dateValue = doc.get("timestamp") ?: doc.get("date")
                    val date = when (dateValue) {
                        is Timestamp -> dateValue.toDate()
                        is Date -> dateValue
                        else -> Date()
                    }

                    allSales.add(
                        SaleRow(
                            date = date,
                            total = doc.getDouble("total") ?: 0.0,
                            paymentType = doc.getString("paymentType") ?: "Unknown",
                            employee = doc.getString("employee") ?: "Unknown"
                        )
                    )
                }

                updateReport()
            }
            .addOnFailureListener {
                reportText.text = "Failed to load sales report."
            }
    }

    private fun updateReport() {
        val filtered = getFilteredSales()
        val totalSales = filtered.sumOf { it.total }
        val totalTransactions = filtered.size
        val averageSale = if (totalTransactions > 0) totalSales / totalTransactions else 0.0

        totalTransactionsCard.text = "Total Transactions\n$totalTransactions"
        totalSalesCard.text = "Total Sales\n$${"%.2f".format(totalSales)}"
        averageSaleCard.text = "Average Sale\n$${"%.2f".format(averageSale)}"
        filterPeriodCard.text = "Period\n${filterSpinner.selectedItem}"

        reportText.text = """
            Filter: ${filterSpinner.selectedItem}

            Total Transactions: $totalTransactions
            Total Sales: $${"%.2f".format(totalSales)}
            Average Sale: $${"%.2f".format(averageSale)}
        """.trimIndent()
    }

    private fun getFilteredSales(): List<SaleRow> {
        val selected = filterSpinner.selectedItem?.toString() ?: "All"
        val calendar = Calendar.getInstance()

        return when (selected) {
            "30d" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                allSales.filter { it.date.after(calendar.time) }
            }
            "60d" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -60)
                allSales.filter { it.date.after(calendar.time) }
            }
            "90d" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -90)
                allSales.filter { it.date.after(calendar.time) }
            }
            "YTD" -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                allSales.filter { it.date.after(calendar.time) }
            }
            else -> allSales
        }
    }

    private fun makeSummaryCard(label: String, value: String, color: Int): TextView {
        val card = TextView(activity)
        card.text = "$label\n$value"
        card.textSize = 18f
        card.setTextColor(color)
        card.gravity = Gravity.CENTER

        // Changes summary card background based on Settings - AF
        card.setBackgroundColor(ThemeManager.sectionBackground(activity))

        card.setPadding(20, 24, 20, 24)
        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        params.setMargins(6, 0, 6, 0)
        card.layoutParams = params
        return card
    }

    private fun exportSales(sales: List<SaleRow>, fileName: String) {
        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )

            val exportsFolder = File(downloadsFolder, "exports")

            if (!exportsFolder.exists()) {
                exportsFolder.mkdirs()
            }

            val file = File(exportsFolder, fileName)
            val writer = FileWriter(file)
            val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US)

            writer.append("Date,Total,Payment Type,Employee\n")

            for (sale in sales) {
                writer.append("${dateFormat.format(sale.date)},")
                writer.append("${"%.2f".format(sale.total)},")
                writer.append("${sale.paymentType},")
                writer.append("${sale.employee}\n")
            }

            writer.flush()
            writer.close()

            Toast.makeText(
                activity,
                "Exported to Downloads/exports/$fileName",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                activity,
                "Export failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}