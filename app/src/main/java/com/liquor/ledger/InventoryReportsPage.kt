package com.liquor.ledger


import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.liquor.ledger.firebase.FirebaseManager
import android.view.Gravity
import android.widget.ScrollView


class InventoryReportPage(private val activity: Activity) {


    private val db: FirebaseFirestore = FirebaseManager.db
    private var inventoryListener: ListenerRegistration? = null


    private lateinit var totalProductsCard: TextView
    private lateinit var lowStockCard: TextView
    private lateinit var outOfStockCard: TextView
    private lateinit var inventoryValueCard: TextView


    fun build(): LinearLayout {
        val root = LinearLayout(activity)
        root.orientation = LinearLayout.VERTICAL

        // Changes root background based on Settings - AF
        root.setBackgroundColor(ThemeManager.pageBackground(activity))

        val scrollView = ScrollView(activity)

        // Changes scroll area background based on Settings - AF
        scrollView.setBackgroundColor(ThemeManager.pageBackground(activity))

        val page = LinearLayout(activity)
        page.orientation = LinearLayout.VERTICAL

        // Changes page background based on Settings - AF
        page.setBackgroundColor(ThemeManager.pageBackground(activity))

        page.setPadding(40, 40, 40, 40)

        val title = TextView(activity)
        title.text = "Inventory Report"
        title.textSize = 28f
        title.setTypeface(null, Typeface.BOLD)

        // Changes title color based on Settings - AF
        title.setTextColor(ThemeManager.primaryText(activity))

        val subtitle = TextView(activity)
        subtitle.text = "Summary of product count, stock status, and total inventory value."
        subtitle.textSize = 16f

        // Changes subtitle color based on Settings - AF
        subtitle.setTextColor(ThemeManager.secondaryText(activity))

        subtitle.setPadding(0, 10, 0, 30)

        val summaryRow1 = LinearLayout(activity)
        summaryRow1.orientation = LinearLayout.HORIZONTAL

        val summaryRow2 = LinearLayout(activity)
        summaryRow2.orientation = LinearLayout.HORIZONTAL
        summaryRow2.setPadding(0, 16, 0, 24)

        // Changes summary card colors based on Settings - AF
        totalProductsCard = makeSummaryCard("Total Products", "0", ThemeManager.primaryAction(activity))
        lowStockCard = makeSummaryCard("Low Stock", "0", ThemeManager.warning(activity))
        outOfStockCard = makeSummaryCard("Out of Stock", "0", ThemeManager.negative(activity))
        inventoryValueCard = makeSummaryCard("Inventory Value", "$0.00", ThemeManager.positive(activity))

        summaryRow1.addView(totalProductsCard)
        summaryRow1.addView(lowStockCard)

        summaryRow2.addView(outOfStockCard)
        summaryRow2.addView(inventoryValueCard)

        val detailTitle = TextView(activity)
        detailTitle.text = "Inventory Overview"
        detailTitle.textSize = 20f
        detailTitle.setTypeface(null, Typeface.BOLD)

        // Changes detail title color based on Settings - AF
        detailTitle.setTextColor(ThemeManager.primaryText(activity))

        detailTitle.setPadding(0, 10, 0, 12)

        val detailText = TextView(activity)
        detailText.text = "Loading inventory report..."
        detailText.textSize = 16f

        // Changes detail text color based on Settings - AF
        detailText.setTextColor(ThemeManager.secondaryText(activity))

        // Changes detail box background based on Settings - AF
        detailText.setBackgroundColor(ThemeManager.sectionBackground(activity))

        detailText.setPadding(24, 24, 24, 24)

        page.addView(title)
        page.addView(subtitle)
        page.addView(summaryRow1)
        page.addView(summaryRow2)
        page.addView(detailTitle)
        page.addView(detailText)

        scrollView.addView(page)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        loadInventoryReport(detailText)

        return root
    }


    private fun loadInventoryReport(detailText: TextView) {
        inventoryListener?.remove()


        inventoryListener = db.collection("products")
            .addSnapshotListener { snapshot, error ->


                if (error != null) {
                    detailText.text = "Error loading inventory report: ${error.message}"
                    return@addSnapshotListener
                }


                if (snapshot == null || snapshot.isEmpty) {
                    totalProductsCard.text = "Total Products\n0"
                    lowStockCard.text = "Low Stock\n0"
                    outOfStockCard.text = "Out of Stock\n0"
                    inventoryValueCard.text = "Inventory Value\n$0.00"

                    detailText.text = "No products found."
                    return@addSnapshotListener
                }


                var totalProducts = 0
                var lowStockItems = 0
                var outOfStockItems = 0
                var inventoryValue = 0.0


                for (doc in snapshot.documents) {
                    val stock = doc.getLong("stock") ?: 0L
                    val reorderPoint = doc.getLong("reorderPoint") ?: 0L
                    val cost = doc.getDouble("cost") ?: 0.0


                    totalProducts++
                    inventoryValue += stock * cost


                    if (stock == 0L) {
                        outOfStockItems++
                    } else if (stock <= reorderPoint) {
                        lowStockItems++
                    }
                }


                totalProductsCard.text = "Total Products\n$totalProducts"
                lowStockCard.text = "Low Stock\n$lowStockItems"
                outOfStockCard.text = "Out of Stock\n$outOfStockItems"
                inventoryValueCard.text = "Inventory Value\n$${"%.2f".format(inventoryValue)}"

                detailText.text = """
                Total Products: $totalProducts

                Low Stock Items: $lowStockItems

                Out of Stock Items: $outOfStockItems

                Inventory Value:
                $${"%.2f".format(inventoryValue)}
               """.trimIndent()
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

        val params = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        params.setMargins(6, 0, 6, 0)
        card.layoutParams = params

        return card
    }

}
