package com.liquor.ledger


import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.liquor.ledger.firebase.FirebaseManager


class InventoryAlertPage(private val activity: Activity) {


    fun build(): LinearLayout {


        val root = LinearLayout(activity)
        root.orientation = LinearLayout.VERTICAL

        // Changes main page background based on Settings - AF
        root.setBackgroundColor(ThemeManager.pageBackground(activity))

        root.setPadding(40, 40, 40, 40)


        val title = TextView(activity)
        title.text = "Inventory Alert Report"
        title.textSize = 28f

        // Changes title color based on Settings - AF
        title.setTextColor(ThemeManager.primaryText(activity))


        val subtitle = TextView(activity)
        subtitle.text = "Products with 10 or fewer units in stock"
        subtitle.textSize = 18f

        // Changes subtitle color based on Settings - AF
        subtitle.setTextColor(ThemeManager.secondaryText(activity))

        subtitle.setPadding(0, 15, 0, 30)

        val summaryRow = LinearLayout(activity)
        summaryRow.orientation = LinearLayout.HORIZONTAL

        val lowStockCard = makeSummaryCard("Low Stock", "0")
        val outOfStockCard = makeSummaryCard("Out of Stock", "0")
        val totalCheckedCard = makeSummaryCard("Products Checked", "0")

        summaryRow.addView(lowStockCard)
        summaryRow.addView(outOfStockCard)
        summaryRow.addView(totalCheckedCard)

        val actionRow = LinearLayout(activity)
        actionRow.orientation = LinearLayout.HORIZONTAL
        actionRow.setPadding(0, 0, 0, 24)

        val exportAlertsButton = TextView(activity)
        exportAlertsButton.text = "Export Alerts"
        exportAlertsButton.textSize = 16f
        exportAlertsButton.gravity = Gravity.CENTER

        exportAlertsButton.setTextColor(Color.WHITE)

        // Changes export button color based on Settings - AF
        exportAlertsButton.setBackgroundColor(ThemeManager.positive(activity))

        exportAlertsButton.setPadding(0, 16, 0, 16)

        val createPoButton = TextView(activity)
        createPoButton.text = "Create Purchase Order"
        createPoButton.textSize = 16f
        createPoButton.gravity = Gravity.CENTER
        createPoButton.setTextColor(Color.WHITE)

        // Changes purchase order button color based on Settings - AF
        createPoButton.setBackgroundColor(ThemeManager.primaryAction(activity))

        createPoButton.setPadding(0, 16, 0, 16)

        val actionButtonParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        actionButtonParams.setMargins(6, 0, 6, 0)

        actionRow.addView(exportAlertsButton, actionButtonParams)
        actionRow.addView(createPoButton, actionButtonParams)

        createPoButton.setOnClickListener {
            createPurchaseOrderFromAlerts()
        }

        val scrollView = ScrollView(activity)

        // Changes scroll area background based on Settings - AF
        scrollView.setBackgroundColor(ThemeManager.pageBackground(activity))

        val content = LinearLayout(activity)
        content.orientation = LinearLayout.VERTICAL

        // Changes alert content background based on Settings - AF
        content.setBackgroundColor(ThemeManager.pageBackground(activity))

        scrollView.addView(content)


        root.addView(title)
        root.addView(subtitle)
        root.addView(summaryRow)
        root.addView(scrollView)
        root.addView(actionRow)


        FirebaseManager.db.collection("products")
            .addSnapshotListener { snapshot, error ->


                content.removeAllViews()


                if (error != null) {


                    val errorText = TextView(activity)
                    errorText.text = "Error loading inventory alerts: ${error.message}"
                    errorText.textSize = 18f

                    // Changes error text color based on Settings - AF
                    errorText.setTextColor(ThemeManager.negative(activity))

                    content.addView(errorText)
                    return@addSnapshotListener
                }


                if (snapshot == null || snapshot.isEmpty) {


                    val emptyText = TextView(activity)
                    emptyText.text = "No products found."
                    emptyText.textSize = 18f

                    // Changes empty text color based on Settings - AF
                    emptyText.setTextColor(ThemeManager.secondaryText(activity))

                    content.addView(emptyText)
                    return@addSnapshotListener
                }


                var alertCount = 0
                var outOfStockCount = 0
                var totalChecked = 0


                for (doc in snapshot.documents) {


                    val name = doc.getString("name") ?: "Unknown Product"
                    totalChecked++
                    val sku = doc.getString("sku") ?: "—"
                    val category = doc.getString("category") ?: "—"
                    val vendor = doc.getString("vendor") ?: "—"


                    val stock = when (val stockField = doc.get("stock")) {
                        is Long -> stockField.toInt()
                        is Double -> stockField.toInt()
                        is Int -> stockField
                        is String -> stockField.toIntOrNull() ?: 0
                        else -> 0
                    }

                    if (stock <= 0) {
                        outOfStockCount++
                    }

                    if (stock <= 10) {


                        alertCount++

                        val stockStatusText = when {
                            stock <= 0 -> "OUT OF STOCK"
                            stock <= 5 -> "CRITICAL STOCK"
                            else -> "LOW STOCK"
                        }

                        val stockStatusColor = when {
                            stock <= 0 -> ThemeManager.negative(activity)
                            stock <= 5 -> ThemeManager.warning(activity)
                            else -> ThemeManager.warning(activity)
                        }

                        val alertCard = LinearLayout(activity)
                        alertCard.orientation = LinearLayout.VERTICAL

                        // Changes alert card background based on Settings - AF
                        alertCard.setBackgroundColor(ThemeManager.sectionBackground(activity))

                        alertCard.setPadding(24, 24, 24, 24)

                        val cardParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )

                        cardParams.setMargins(0, 0, 0, 20)

                        val nameText = TextView(activity)
                        nameText.text = name
                        nameText.textSize = 20f

                        // Changes product name color based on Settings - AF
                        nameText.setTextColor(ThemeManager.primaryText(activity))

                        val statusText = TextView(activity)
                        statusText.text = stockStatusText
                        statusText.textSize = 14f
                        statusText.setTextColor(Color.WHITE)
                        statusText.setBackgroundColor(stockStatusColor)
                        statusText.gravity = Gravity.CENTER
                        statusText.setPadding(12, 8, 12, 8)

                        val detailsText = TextView(activity)
                        detailsText.text = """
                            SKU: $sku
                            Category: $category
                            Vendor: $vendor
                            Current Stock: $stock
                            """.trimIndent()

                        detailsText.textSize = 16f

                        // Changes product details color based on Settings - AF
                        detailsText.setTextColor(ThemeManager.secondaryText(activity))

                        alertCard.addView(statusText)
                        alertCard.addView(nameText)
                        alertCard.addView(detailsText)

                        content.addView(alertCard, cardParams)

                    }
                }

                lowStockCard.text = "Low Stock\n$alertCount"
                outOfStockCard.text = "Out of Stock\n$outOfStockCount"
                totalCheckedCard.text = "Products Checked\n$totalChecked"

                if (alertCount == 0) {


                    val noAlerts = TextView(activity)


                    noAlerts.text =
                        "No inventory alerts. All products have more than 10 units in stock."


                    noAlerts.textSize = 18f

                    noAlerts.setTextColor(ThemeManager.secondaryText(activity))


                    content.addView(noAlerts)
                }
            }


        return root
    }
    private fun makeSummaryCard(label: String, value: String): TextView {

        val card = TextView(activity)
        card.text = "$label\n$value"
        card.textSize = 18f

        // Changes summary card text color based on Settings - AF
        card.setTextColor(ThemeManager.primaryText(activity))

        card.gravity = Gravity.CENTER

        // Changes summary card background based on Settings - AF
        card.setBackgroundColor(ThemeManager.sectionBackground(activity))

        card.setPadding(20, 20, 20, 20)

        val params = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        params.setMargins(6, 0, 6, 24)
        card.layoutParams = params

        return card
    }

    private fun createPurchaseOrderFromAlerts() {

        FirebaseManager.db.collection("products")
            .get()
            .addOnSuccessListener { snapshot ->

                val itemsByVendor = mutableMapOf<String, MutableList<HashMap<String, Any>>>()

                for (doc in snapshot.documents) {

                    val name = doc.getString("name") ?: "Unknown Product"
                    val vendor = doc.getString("vendor") ?: "Unknown Vendor"

                    val stock = when (val stockField = doc.get("stock")) {
                        is Long -> stockField.toInt()
                        is Double -> stockField.toInt()
                        is Int -> stockField
                        is String -> stockField.toIntOrNull() ?: 0
                        else -> 0
                    }

                    val reorderPoint = when (val reorderField = doc.get("reorderPoint")) {
                        is Long -> reorderField.toInt()
                        is Double -> reorderField.toInt()
                        is Int -> reorderField
                        is String -> reorderField.toIntOrNull() ?: 10
                        else -> 10
                    }

                    val cost = doc.getDouble("cost") ?: 0.0

                    if (stock <= reorderPoint) {

                        val neededQuantity = reorderPoint - stock

                        if (neededQuantity > 0) {

                            val item = hashMapOf<String, Any>(
                                "productName" to name,
                                "quantity" to neededQuantity.toLong(),
                                "costPerUnit" to cost
                            )

                            if (!itemsByVendor.containsKey(vendor)) {
                                itemsByVendor[vendor] = mutableListOf()
                            }

                            itemsByVendor[vendor]?.add(item)
                        }
                    }
                }

                if (itemsByVendor.isEmpty()) {
                    Toast.makeText(
                        activity,
                        "No low stock items need ordering",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                FirebaseManager.db.collection("purchaseOrders")
                    .get()
                    .addOnSuccessListener { poSnapshot ->

                        var nextNumber = poSnapshot.size() + 1

                        itemsByVendor.forEach { vendorEntry ->

                            val vendor = vendorEntry.key
                            val items = vendorEntry.value

                            val total = items.sumOf { item ->
                                val quantity = item["quantity"] as Long
                                val costPerUnit = item["costPerUnit"] as Double
                                quantity * costPerUnit
                            }

                            val poNumber = "PO-" + nextNumber.toString().padStart(4, '0')
                            nextNumber++

                            val newPO = hashMapOf(
                                "poNumber" to poNumber,
                                "vendor" to vendor,
                                "date" to com.google.firebase.Timestamp.now(),
                                "total" to total,
                                "status" to "pending review",
                                "notes" to "Auto-created from Inventory Alerts",
                                "items" to items
                            )

                            FirebaseManager.db.collection("purchaseOrders")
                                .add(newPO)
                        }

                        Toast.makeText(
                            activity,
                            "Purchase order draft created",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    activity,
                    "Error creating purchase order: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
