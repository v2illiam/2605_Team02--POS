package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.liquor.ledger.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// PurchaseOrdersPage displays all purchase orders from Firestore
// Only visible to Manager employees
// Allows creating new POs with product line items
// Receiving a PO automatically updates inventory stock levels
class PurchaseOrdersPage(private val activity: Activity) {

    // Firestore database instance
    private val db: FirebaseFirestore = FirebaseManager.db

    // Reads saved settings from SettingsPage
    private val prefs = activity.getSharedPreferences("settings_prefs", Activity.MODE_PRIVATE)

    private val KEY_COLORBLIND_MODE = "colorblind_mode"
    private val KEY_DARK_MODE = "dark_mode"

    // Main layout containers
    private lateinit var pageLayout: LinearLayout
    private lateinit var detailPanel: LinearLayout
    private lateinit var poListContainer: LinearLayout

    // Current filter selection
    private var currentFilter = "All"

    // List of products loaded from Firestore for the dropdown
    // Key = product name, Value = document ID
    private var productMap: Map<String, String> = emptyMap()

    // Line items being added to a new PO
    // Each item is: productName, quantity, costPerUnit
    private val lineItems = mutableListOf<Triple<String, Int, Double>>()

    // Container for the line items in the new order form
    private lateinit var lineItemsContainer: LinearLayout

    // Total amount label in the new order form
    private lateinit var totalAmountLabel: TextView

    fun build(): LinearLayout {

        // ROOT — horizontal split between list and detail panel
        pageLayout = LinearLayout(activity)
        pageLayout.orientation = LinearLayout.HORIZONTAL
        pageLayout.setBackgroundColor(getPageBackgroundColor())

        // LEFT SIDE — PO list
        val leftSide = LinearLayout(activity)
        leftSide.orientation = LinearLayout.VERTICAL
        leftSide.setBackgroundColor(getPageBackgroundColor())

        val leftParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            3f
        )

        // TOP BAR — filter buttons and new order button
        val topBar = LinearLayout(activity)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(dp(16), dp(12), dp(16), dp(12))
        topBar.setBackgroundColor(getPageBackgroundColor())

        val topBarParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Filter buttons
        val filters = listOf("All", "pending review", "submitted", "received")

        fun refreshFilterBar() {
            topBar.removeAllViews()
            filters.forEach { f ->
                val btn = makeFilterButton(f)
                if (f == currentFilter) {
                    btn.setBackgroundColor(getPrimaryActionColor())
                    btn.setTextColor(Color.WHITE)
                }
                btn.setOnClickListener {
                    currentFilter = f
                    loadPurchaseOrders()
                    refreshFilterBar()
                }
                topBar.addView(btn)
            }
            topBar.addView(makeNewOrderButton())
        }

        refreshFilterBar()

        // TABLE HEADER ROW
        val headerRow = makeTableHeader()

        // SCROLLABLE LIST of PO rows
        val scrollView = ScrollView(activity)
        scrollView.setBackgroundColor(getPageBackgroundColor())

        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        poListContainer = LinearLayout(activity)
        poListContainer.orientation = LinearLayout.VERTICAL
        poListContainer.setBackgroundColor(getPageBackgroundColor())

        scrollView.addView(poListContainer)

        leftSide.addView(topBar, topBarParams)
        leftSide.addView(headerRow)
        leftSide.addView(scrollView, scrollParams)

        // RIGHT SIDE — detail panel
        detailPanel = LinearLayout(activity)
        detailPanel.orientation = LinearLayout.VERTICAL
        detailPanel.setBackgroundColor(getSectionBackgroundColor())
        detailPanel.setPadding(dp(20), dp(20), dp(20), dp(20))

        val rightParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            2f
        )

        val selectText = TextView(activity)
        selectText.text = "Select an order to view details"
        selectText.textSize = 16f
        selectText.setTextColor(getMutedTextColor())
        selectText.gravity = Gravity.CENTER

        detailPanel.addView(selectText)

        pageLayout.addView(leftSide, leftParams)
        pageLayout.addView(detailPanel, rightParams)

        // Load products for dropdown then load POs
        loadProductsForDropdown()

        return pageLayout
    }

    // Loads all products from Firestore for use in the new order dropdown
    private fun loadProductsForDropdown() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("products").get().await()
                val map = mutableMapOf<String, String>()

                snapshot.documents.forEach { doc ->
                    val name = doc.getString("name") ?: ""

                    if (name.isNotEmpty()) {
                        map[name] = doc.id
                    }
                }

                withContext(Dispatchers.Main) {
                    productMap = map
                    loadPurchaseOrders()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadPurchaseOrders()
                }
            }
        }
    }

    // Loads purchase orders from Firestore
    private fun loadPurchaseOrders() {
        poListContainer.removeAllViews()

        val loadingText = TextView(activity)
        loadingText.text = "Loading..."
        loadingText.textSize = 14f
        loadingText.setTextColor(getMutedTextColor())
        loadingText.setPadding(dp(16), dp(16), dp(16), dp(16))
        poListContainer.addView(loadingText)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("purchaseOrders")
                    .get()
                    .await()

                val orders = snapshot.documents.mapNotNull { doc ->
                    val status = doc.getString("status") ?: ""

                    if (currentFilter != "All" && status != currentFilter) {
                        null
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        val items = doc.get("items") as? List<Map<String, Any>> ?: emptyList()

                        val formattedDate = try {
                            val timestamp = doc.getTimestamp("date")

                            if (timestamp != null) {
                                java.text.SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    java.util.Locale.getDefault()
                                ).format(timestamp.toDate())
                            } else {
                                doc.getString("date") ?: ""
                            }
                        } catch (e: Exception) {
                            doc.getString("date") ?: ""
                        }

                        mapOf(
                            "docId" to doc.id,
                            "poNumber" to (doc.getString("poNumber") ?: ""),
                            "vendor" to (doc.getString("vendor") ?: ""),
                            "date" to formattedDate,
                            "total" to (doc.getDouble("total")?.toString() ?: "0.0"),
                            "status" to status,
                            "notes" to (doc.getString("notes") ?: ""),
                            "itemCount" to items.size.toString()
                        )
                    }
                }

                val sortedOrders = orders.sortedByDescending { it["poNumber"] }

                withContext(Dispatchers.Main) {
                    poListContainer.removeAllViews()

                    if (sortedOrders.isEmpty()) {
                        val emptyText = TextView(activity)
                        emptyText.text = "No purchase orders found"
                        emptyText.textSize = 14f
                        emptyText.setTextColor(getMutedTextColor())
                        emptyText.setPadding(dp(16), dp(16), dp(16), dp(16))

                        poListContainer.addView(emptyText)
                    } else {
                        sortedOrders.forEach { order ->
                            poListContainer.addView(makePORow(order))

                            val divider = android.view.View(activity)
                            divider.setBackgroundColor(getDividerColor())

                            poListContainer.addView(
                                divider,
                                LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    1
                                )
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    poListContainer.removeAllViews()

                    val errorText = TextView(activity)
                    errorText.text = "Error loading orders: ${e.message}"
                    errorText.textSize = 14f
                    errorText.setTextColor(getNegativeColor())
                    errorText.setPadding(dp(16), dp(16), dp(16), dp(16))

                    poListContainer.addView(errorText)
                }
            }
        }
    }

    // Creates a single PO row
    private fun makePORow(order: Map<String, String>): LinearLayout {
        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(dp(16), dp(14), dp(16), dp(14))
        row.setBackgroundColor(getPageBackgroundColor())

        val rowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        rowParams.setMargins(0, 0, 0, dp(1))
        row.layoutParams = rowParams

        row.addView(makeRowCell(order["poNumber"] ?: "", 1f))
        row.addView(makeRowCell(order["vendor"] ?: "", 2f))
        row.addView(makeRowCell(order["date"] ?: "", 1f))
        row.addView(makeRowCell("$${"%.2f".format(order["total"]?.toDoubleOrNull() ?: 0.0)}", 1f))
        row.addView(makeStatusCell(order["status"] ?: ""))

        row.setOnClickListener { showDetailPanel(order) }
        row.isClickable = true
        row.isFocusable = true

        return row
    }

    // Creates a text cell for a table row
    private fun makeRowCell(text: String, weight: Float): TextView {
        val cell = TextView(activity)
        cell.text = text
        cell.textSize = 14f
        cell.setTextColor(getSecondaryTextColor())
        cell.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            weight
        )

        return cell
    }

    // Creates a colored status cell
    private fun makeStatusCell(status: String): TextView {
        val cell = TextView(activity)
        cell.text = status
        cell.textSize = 12f
        cell.gravity = Gravity.CENTER
        cell.setPadding(dp(8), dp(4), dp(8), dp(4))
        cell.setTextColor(getStatusColor(status))
        cell.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        return cell
    }

    // Shows the detail panel for a selected PO
    private fun showDetailPanel(order: Map<String, String>) {
        detailPanel.removeAllViews()
        detailPanel.setBackgroundColor(getSectionBackgroundColor())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = db.collection("purchaseOrders")
                    .document(order["docId"] ?: "")
                    .get()
                    .await()

                @Suppress("UNCHECKED_CAST")
                val items = doc.get("items") as? List<Map<String, Any>> ?: emptyList()

                withContext(Dispatchers.Main) {
                    detailPanel.removeAllViews()

                    val poTitle = TextView(activity)
                    poTitle.text = order["poNumber"] ?: ""
                    poTitle.textSize = 22f
                    poTitle.setTextColor(getPrimaryTextColor())
                    poTitle.setTypeface(null, Typeface.BOLD)
                    poTitle.setPadding(0, 0, 0, dp(4))

                    val vendorText = TextView(activity)
                    vendorText.text = "Vendor: ${order["vendor"]}"
                    vendorText.textSize = 16f
                    vendorText.setTextColor(getSecondaryTextColor())
                    vendorText.setPadding(0, 0, 0, dp(4))

                    val dateText = TextView(activity)
                    dateText.text = "Date: ${order["date"]}"
                    dateText.textSize = 14f
                    dateText.setTextColor(getMutedTextColor())
                    dateText.setPadding(0, 0, 0, dp(4))

                    val totalText = TextView(activity)
                    totalText.text = "Total: $${"%.2f".format(order["total"]?.toDoubleOrNull() ?: 0.0)}"
                    totalText.textSize = 16f
                    totalText.setTextColor(getPrimaryTextColor())
                    totalText.setTypeface(null, Typeface.BOLD)
                    totalText.setPadding(0, 0, 0, dp(8))

                    val statusText = TextView(activity)
                    statusText.text = "Status: ${order["status"]}"
                    statusText.textSize = 14f
                    statusText.setTextColor(getStatusColor(order["status"] ?: ""))
                    statusText.setPadding(0, 0, 0, dp(16))

                    detailPanel.addView(poTitle)
                    detailPanel.addView(vendorText)
                    detailPanel.addView(dateText)
                    detailPanel.addView(totalText)
                    detailPanel.addView(statusText)

                    if (items.isNotEmpty()) {
                        val itemsTitle = TextView(activity)
                        itemsTitle.text = "Items:"
                        itemsTitle.textSize = 15f
                        itemsTitle.setTextColor(getPrimaryTextColor())
                        itemsTitle.setTypeface(null, Typeface.BOLD)
                        itemsTitle.setPadding(0, 0, 0, dp(8))

                        detailPanel.addView(itemsTitle)

                        val itemHeader = LinearLayout(activity)
                        itemHeader.orientation = LinearLayout.HORIZONTAL
                        itemHeader.setPadding(0, 0, 0, dp(4))

                        listOf(
                            Pair("Product", 3f),
                            Pair("Qty", 1f),
                            Pair("Cost", 1f),
                            Pair("Subtotal", 1f)
                        ).forEach { (text, weight) ->
                            val cell = TextView(activity)
                            cell.text = text
                            cell.textSize = 12f
                            cell.setTextColor(getMutedTextColor())
                            cell.setTypeface(null, Typeface.BOLD)
                            cell.layoutParams = LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                weight
                            )

                            itemHeader.addView(cell)
                        }

                        detailPanel.addView(itemHeader)

                        items.forEach { item ->
                            val productName = item["productName"] as? String ?: ""
                            val qty = (item["quantity"] as? Long)?.toInt() ?: 0
                            val cost = item["costPerUnit"] as? Double ?: 0.0
                            val subtotal = qty * cost

                            val itemRow = LinearLayout(activity)
                            itemRow.orientation = LinearLayout.HORIZONTAL
                            itemRow.setPadding(0, dp(4), 0, dp(4))

                            listOf(
                                Pair(productName, 3f),
                                Pair(qty.toString(), 1f),
                                Pair("$${"%.2f".format(cost)}", 1f),
                                Pair("$${"%.2f".format(subtotal)}", 1f)
                            ).forEach { (text, weight) ->
                                val cell = TextView(activity)
                                cell.text = text
                                cell.textSize = 13f
                                cell.setTextColor(getSecondaryTextColor())
                                cell.layoutParams = LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    weight
                                )

                                itemRow.addView(cell)
                            }

                            detailPanel.addView(itemRow)
                        }

                        val divider = android.view.View(activity)
                        divider.setBackgroundColor(getDividerColor())

                        detailPanel.addView(
                            divider,
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                dp(1)
                            ).also {
                                it.setMargins(0, dp(8), 0, dp(8))
                            }
                        )
                    }

                    if (!order["notes"].isNullOrEmpty()) {
                        val notesText = TextView(activity)
                        notesText.text = "Notes: ${order["notes"]}"
                        notesText.textSize = 13f
                        notesText.setTextColor(getMutedTextColor())
                        notesText.setPadding(0, 0, 0, dp(16))

                        detailPanel.addView(notesText)
                    }

                    when (order["status"]) {
                        "pending review" -> {
                            detailPanel.addView(
                                makeActionButton(
                                    "Submit Order",
                                    getPrimaryActionColor()
                                ) {
                                    updatePOStatus(order["docId"] ?: "", "submitted")
                                }
                            )
                        detailPanel.addView(
                            makeActionButton(
                                "Delete Order",
                                getNegativeColor()
                            ) {
                                confirmDeletePO(order["docId"] ?: "", order["poNumber"] ?: "")
                            }
                        )
                    }

                        "submitted" -> {
                            detailPanel.addView(
                                makeActionButton(
                                    "Review & Receive Order",
                                    getPositiveColor()
                                ) {
                                    showReceivingChecklist(order["docId"] ?: "", items)
                                }
                            )
                        }

                        "received" -> {
                            val receivedNote = TextView(activity)
                            receivedNote.text = "This order has been received"
                            receivedNote.textSize = 14f
                            receivedNote.setTextColor(getPositiveColor())

                            detailPanel.addView(receivedNote)
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorText = TextView(activity)
                    errorText.text = "Error loading PO details: ${e.message}"
                    errorText.textSize = 14f
                    errorText.setTextColor(getNegativeColor())

                    detailPanel.addView(errorText)
                }
            }
        }
    }

    // Shows a confirmation dialog before deleting a PO
    private fun confirmDeletePO(docId: String, poNumber: String) {
        AlertDialog.Builder(activity)
            .setTitle("Delete $poNumber?")
            .setMessage("This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        db.collection("purchaseOrders")
                            .document(docId)
                            .delete()
                            .await()

                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                activity, "$poNumber deleted",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            detailPanel.removeAllViews()
                            val selectText = TextView(activity)
                            selectText.text = "Select an order to view details"
                            selectText.textSize = 16f
                            selectText.setTextColor(Color.GRAY)

                            // Uses ThemeManager so select text follows Light or Dark Mode - AF
                            selectText.setTextColor(getMutedTextColor())

                            detailPanel.addView(selectText)
                            loadPurchaseOrders()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                activity, "Error deleting: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // Marks PO as received and updates inventory stock for each item
    // Shows a checklist to verify what was actually received
// Manager can mark each item as received in full, partially, or not at all
    private fun showReceivingChecklist(
        docId: String,
        items: List<Map<String, Any>>
    ) {
        detailPanel.removeAllViews()

        val scrollView = ScrollView(activity)
        val container = LinearLayout(activity)
        container.orientation = LinearLayout.VERTICAL

        val title = TextView(activity)
        title.text = "Receiving Checklist"
        title.textSize = 20f

        // Uses ThemeManager so checklist title follows Light or Dark Mode - AF
        title.setTextColor(getPrimaryTextColor())

        title.setTypeface(null, Typeface.BOLD)
        title.setPadding(0, 0, 0, dp(4))

        val subtitle = TextView(activity)
        subtitle.text = "Confirm quantity actually received for each item"
        subtitle.textSize = 13f

        // Uses ThemeManager so checklist subtitle follows Light or Dark Mode - AF
        subtitle.setTextColor(getMutedTextColor())

        subtitle.setPadding(0, 0, 0, dp(16))

        container.addView(title)
        container.addView(subtitle)

        // One input field per item, defaulted to the ordered quantity
        // receivedInputs maps index -> the EditText so we can read values later
        val receivedInputs = mutableListOf<android.widget.EditText>()

        items.forEachIndexed { index, item ->
            val productName = item["productName"] as? String ?: ""
            val orderedQty = (item["quantity"] as? Long)?.toInt() ?: 0

            val itemBlock = LinearLayout(activity)
            itemBlock.orientation = LinearLayout.VERTICAL

            // Uses ThemeManager so item blocks follow Light or Dark Mode - AF
            itemBlock.setBackgroundColor(getSectionBackgroundColor())

            itemBlock.setPadding(dp(12), dp(12), dp(12), dp(12))

            val itemBlockParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            itemBlockParams.setMargins(0, 0, 0, dp(8))
            itemBlock.layoutParams = itemBlockParams

            val nameText = TextView(activity)
            nameText.text = productName
            nameText.textSize = 14f

            // Uses ThemeManager so item names follow Light or Dark Mode - AF
            nameText.setTextColor(getPrimaryTextColor())

            nameText.setTypeface(null, Typeface.BOLD)

            val orderedText = TextView(activity)
            orderedText.text = "Ordered: $orderedQty"
            orderedText.textSize = 12f

            // Uses ThemeManager so ordered quantity text follows Light or Dark Mode - AF
            orderedText.setTextColor(getMutedTextColor())

            orderedText.setPadding(0, dp(2), 0, dp(6))

            val receivedLabel = TextView(activity)
            receivedLabel.text = "Quantity Received"
            receivedLabel.textSize = 12f


            // Uses ThemeManager so received labels follow Light or Dark Mode - AF
            receivedLabel.setTextColor(getSecondaryTextColor())

            val receivedInput = android.widget.EditText(activity)
            receivedInput.setText(orderedQty.toString())
            receivedInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            receivedInput.textSize = 14f

            // Uses ThemeManager so received input follows Light or Dark Mode - AF
            receivedInput.setTextColor(getPrimaryTextColor())

            receivedInput.setPadding(dp(8), dp(8), dp(8), dp(8))

            // Uses ThemeManager so received input background follows Light or Dark Mode - AF
            receivedInput.setBackgroundColor(getInputBackgroundColor())

            receivedInputs.add(receivedInput)

            itemBlock.addView(nameText)
            itemBlock.addView(orderedText)
            itemBlock.addView(receivedLabel)
            itemBlock.addView(receivedInput)

            container.addView(itemBlock)
        }

        // Not Received button — marks everything as zero received, PO stays submitted
        val notReceivedBtn = makeActionButton(
            "Mark Order as Not Received",
            getNegativeColor()
        ) {
            AlertDialog.Builder(activity)
                .setTitle("Mark as Not Received?")
                .setMessage("This order will remain in 'submitted' status and no inventory will be updated.")
                .setPositiveButton("Confirm") { _, _ ->
                    android.widget.Toast.makeText(
                        activity,
                        "Order remains submitted. No inventory changes made.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    showDetailPanel(
                        mapOf(
                            "docId" to docId,
                            "poNumber" to "",
                            "vendor" to "",
                            "date" to "",
                            "total" to "0.0",
                            "status" to "submitted",
                            "notes" to ""
                        )
                    )
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        // Confirm Receipt button — updates inventory based on what was actually entered
        val confirmBtn = makeActionButton(
            "Confirm Receipt & Update Inventory",
            getPositiveColor()
        ) {
            val receivedItems = items.mapIndexed { index, item ->
                val productName = item["productName"] as? String ?: ""
                val orderedQty = (item["quantity"] as? Long)?.toInt() ?: 0
                val receivedQty = receivedInputs[index].text.toString()
                    .toIntOrNull() ?: 0
                Triple(productName, orderedQty, receivedQty.coerceIn(0, Int.MAX_VALUE))
            }

            // Check if this is a partial receipt
            val isPartial = receivedItems.any { (_, ordered, received) ->
                received != ordered
            }

            if (isPartial) {
                AlertDialog.Builder(activity)
                    .setTitle("Partial Receipt Detected")
                    .setMessage("Some quantities don't match what was ordered. Inventory will be updated based on what you entered, and the order will be marked received.")
                    .setPositiveButton("Confirm") { _, _ ->
                        finalizeReceiving(docId, receivedItems)
                    }
                    .setNegativeButton("Go Back") { dialog, _ -> dialog.dismiss() }
                    .show()
            } else {
                finalizeReceiving(docId, receivedItems)
            }
        }

        container.addView(confirmBtn)
        container.addView(notReceivedBtn)

        val cancelBtn = makeActionButton(
            "Cancel",
            getMutedButtonColor()
        ) {
            // Just reload the detail panel without changes
            CoroutineScope(Dispatchers.IO).launch {
                loadPurchaseOrders()
            }
        }
        container.addView(cancelBtn)

        scrollView.addView(container)
        detailPanel.addView(scrollView)
    }

    // Applies the received quantities to inventory and marks the PO as received
    private fun finalizeReceiving(
        docId: String,
        receivedItems: List<Triple<String, Int, Int>>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Update stock for each item based on quantity actually received
                receivedItems.forEach { (productName, _, receivedQty) ->
                    if (productName.isNotEmpty() && receivedQty > 0) {
                        val productSnapshot = db.collection("products")
                            .whereEqualTo("name", productName)
                            .get()
                            .await()

                        if (!productSnapshot.isEmpty) {
                            val productDoc = productSnapshot.documents[0]
                            val currentStock = productDoc.getLong("stock") ?: 0L
                            val newStock = currentStock + receivedQty

                            db.collection("products")
                                .document(productDoc.id)
                                .update("stock", newStock)
                                .await()
                        }
                    }
                }

                // Build a summary of what was received vs ordered to store on the PO
                val receivedSummary = receivedItems.map { (name, ordered, received) ->
                    hashMapOf(
                        "productName" to name,
                        "orderedQuantity" to ordered.toLong(),
                        "receivedQuantity" to received.toLong()
                    )
                }

                db.collection("purchaseOrders")
                    .document(docId)
                    .update(
                        mapOf(
                            "status" to "received",
                            "receivedItems" to receivedSummary
                        )
                    )
                    .await()

                withContext(Dispatchers.Main) {
                    loadPurchaseOrders()
                    detailPanel.removeAllViews()
                    val successText = TextView(activity)
                    successText.text = "Order received and inventory updated"
                    successText.textSize = 16f

                    // Uses ThemeManager so success text supports Colorblind Mode - AF
                    successText.setTextColor(getPositiveColor())

                    successText.setPadding(0, dp(20), 0, 0)
                    detailPanel.addView(successText)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorText = TextView(activity)
                    errorText.text = "Error receiving order: ${e.message}"
                    errorText.textSize = 14f

                    // Uses ThemeManager so error text supports Colorblind Mode - AF
                    errorText.setTextColor(getNegativeColor())

                    detailPanel.addView(errorText)
                }
            }
        }
    }

    // Updates PO status without updating inventory
    private fun updatePOStatus(docId: String, newStatus: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("purchaseOrders")
                    .document(docId)
                    .update("status", newStatus)
                    .await()

                withContext(Dispatchers.Main) {
                    loadPurchaseOrders()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorText = TextView(activity)
                    errorText.text = "Error updating status: ${e.message}"
                    errorText.textSize = 14f
                    errorText.setTextColor(getNegativeColor())

                    detailPanel.addView(errorText)
                }
            }
        }
    }

    // Shows the new order form in the detail panel
    private fun showNewOrderForm() {
        detailPanel.removeAllViews()
        detailPanel.setBackgroundColor(getSectionBackgroundColor())
        lineItems.clear()

        val scrollView = ScrollView(activity)
        scrollView.setBackgroundColor(getSectionBackgroundColor())

        val formContainer = LinearLayout(activity)
        formContainer.orientation = LinearLayout.VERTICAL
        formContainer.setBackgroundColor(getSectionBackgroundColor())

        val formTitle = TextView(activity)
        formTitle.text = "New Purchase Order"
        formTitle.textSize = 20f
        formTitle.setTextColor(getPrimaryTextColor())
        formTitle.setTypeface(null, Typeface.BOLD)
        formTitle.setPadding(0, 0, 0, dp(16))

        val vendorLabel = makeFormLabel("Vendor *")
        val vendorInput = makeFormInput("Enter vendor name")

        val notesLabel = makeFormLabel("Notes (optional)")
        val notesInput = makeFormInput("Add any notes...")

        val itemsTitle = TextView(activity)
        itemsTitle.text = "Add Products"
        itemsTitle.textSize = 16f
        itemsTitle.setTextColor(getPrimaryTextColor())
        itemsTitle.setTypeface(null, Typeface.BOLD)
        itemsTitle.setPadding(0, dp(16), 0, dp(8))

        val productLabel = makeFormLabel("Product")
        val productSpinner = Spinner(activity)

        val productNames = listOf("Select a product") + productMap.keys.toList()

        val spinnerAdapter = ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_item,
            productNames
        )

        spinnerAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        productSpinner.adapter = spinnerAdapter

        val spinnerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        spinnerParams.setMargins(0, 0, 0, dp(8))
        productSpinner.layoutParams = spinnerParams

        val qtyLabel = makeFormLabel("Quantity")
        val qtyInput = makeFormInput("0")
        qtyInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        val costLabel = makeFormLabel("Cost Per Unit ($)")
        val costInput = makeFormInput("0.00")
        costInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val addItemBtn = makeActionButton(
            "+ Add Item to Order",
            getMutedButtonColor()
        ) {
            val selectedProduct = productSpinner.selectedItem.toString()
            val qty = qtyInput.text.toString().toIntOrNull() ?: 0
            val cost = costInput.text.toString().toDoubleOrNull() ?: 0.0

            if (selectedProduct == "Select a product") {
                android.widget.Toast.makeText(
                    activity,
                    "Please select a product",
                    android.widget.Toast.LENGTH_SHORT
                ).show()

                return@makeActionButton
            }

            if (qty <= 0) {
                android.widget.Toast.makeText(
                    activity,
                    "Please enter a valid quantity",
                    android.widget.Toast.LENGTH_SHORT
                ).show()

                return@makeActionButton
            }

            lineItems.add(Triple(selectedProduct, qty, cost))
            updateLineItemsDisplay()
            updateTotalLabel()

            productSpinner.setSelection(0)
            qtyInput.setText("")
            costInput.setText("")
        }

        val lineItemsTitle = TextView(activity)
        lineItemsTitle.text = "Order Items:"
        lineItemsTitle.textSize = 14f
        lineItemsTitle.setTextColor(getPrimaryTextColor())
        lineItemsTitle.setTypeface(null, Typeface.BOLD)
        lineItemsTitle.setPadding(0, dp(12), 0, dp(4))

        lineItemsContainer = LinearLayout(activity)
        lineItemsContainer.orientation = LinearLayout.VERTICAL
        lineItemsContainer.setBackgroundColor(getSectionBackgroundColor())

        totalAmountLabel = TextView(activity)
        totalAmountLabel.text = "Total: $0.00"
        totalAmountLabel.textSize = 16f
        totalAmountLabel.setTextColor(getPrimaryTextColor())
        totalAmountLabel.setTypeface(null, Typeface.BOLD)
        totalAmountLabel.setPadding(0, dp(8), 0, dp(8))

        val createBtn = makeActionButton(
            "Create Purchase Order",
            getPositiveColor()
        ) {
            val vendor = vendorInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()

            if (vendor.isEmpty()) {
                vendorLabel.setTextColor(getNegativeColor())
                vendorLabel.text = "Vendor * (required)"
                return@makeActionButton
            }

            if (lineItems.isEmpty()) {
                android.widget.Toast.makeText(
                    activity,
                    "Please add at least one product",
                    android.widget.Toast.LENGTH_SHORT
                ).show()

                return@makeActionButton
            }

            createNewPO(vendor, notes)
        }

        formContainer.addView(formTitle)
        formContainer.addView(vendorLabel)
        formContainer.addView(vendorInput)
        formContainer.addView(notesLabel)
        formContainer.addView(notesInput)
        formContainer.addView(itemsTitle)
        formContainer.addView(productLabel)
        formContainer.addView(productSpinner, spinnerParams)
        formContainer.addView(qtyLabel)
        formContainer.addView(qtyInput)
        formContainer.addView(costLabel)
        formContainer.addView(costInput)
        formContainer.addView(addItemBtn)
        formContainer.addView(lineItemsTitle)
        formContainer.addView(lineItemsContainer)
        formContainer.addView(totalAmountLabel)
        formContainer.addView(createBtn)

        scrollView.addView(formContainer)
        detailPanel.addView(scrollView)
    }

    // Updates the line items display in the form
    private fun updateLineItemsDisplay() {
        lineItemsContainer.removeAllViews()

        if (lineItems.isEmpty()) {
            val emptyText = TextView(activity)
            emptyText.text = "No items added yet"
            emptyText.textSize = 13f
            emptyText.setTextColor(getMutedTextColor())

            lineItemsContainer.addView(emptyText)
            return
        }

        lineItems.forEachIndexed { index, (product, qty, cost) ->
            val itemRow = LinearLayout(activity)
            itemRow.orientation = LinearLayout.HORIZONTAL
            itemRow.gravity = Gravity.CENTER_VERTICAL
            itemRow.setPadding(0, dp(4), 0, dp(4))

            val itemText = TextView(activity)
            itemText.text = "$product x$qty @ $${"%.2f".format(cost)}" +
                    " = $${"%.2f".format(qty * cost)}"
            itemText.textSize = 13f
            itemText.setTextColor(getSecondaryTextColor())
            itemText.layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            val removeBtn = TextView(activity)
            removeBtn.text = "X"
            removeBtn.textSize = 13f
            removeBtn.setTextColor(getNegativeColor())
            removeBtn.setPadding(dp(8), dp(4), dp(8), dp(4))

            removeBtn.setOnClickListener {
                lineItems.removeAt(index)
                updateLineItemsDisplay()
                updateTotalLabel()
            }

            itemRow.addView(itemText)
            itemRow.addView(removeBtn)
            lineItemsContainer.addView(itemRow)
        }
    }

    // Recalculates and updates the total label
    private fun updateTotalLabel() {
        val total = lineItems.sumOf { (_, qty, cost) -> qty * cost }
        totalAmountLabel.text = "Total: $${"%.2f".format(total)}"
    }

    // Creates a new PO in Firestore with line items
    private fun createNewPO(vendor: String, notes: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("purchaseOrders").get().await()
                val nextNumber = snapshot.size() + 1
                val poNumber = "PO-" + nextNumber.toString().padStart(4, '0')

                val timestamp = com.google.firebase.Timestamp.now()

                val total = lineItems.sumOf { (_, qty, cost) -> qty * cost }

                val itemsList = lineItems.map { (product, qty, cost) ->
                    hashMapOf(
                        "productName" to product,
                        "quantity" to qty.toLong(),
                        "costPerUnit" to cost
                    )
                }

                val newPO = hashMapOf(
                    "poNumber" to poNumber,
                    "vendor" to vendor,
                    "date" to timestamp,
                    "total" to total,
                    "status" to "pending review",
                    "notes" to notes,
                    "items" to itemsList
                )

                db.collection("purchaseOrders").add(newPO).await()

                withContext(Dispatchers.Main) {
                    lineItems.clear()
                    loadPurchaseOrders()
                    detailPanel.removeAllViews()

                    val successText = TextView(activity)
                    successText.text = "$poNumber created successfully"
                    successText.textSize = 16f
                    successText.setTextColor(getPositiveColor())
                    successText.setPadding(0, dp(20), 0, 0)

                    detailPanel.addView(successText)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorText = TextView(activity)
                    errorText.text = "Error creating PO: ${e.message}"
                    errorText.textSize = 14f
                    errorText.setTextColor(getNegativeColor())

                    detailPanel.addView(errorText)
                }
            }
        }
    }

    // Creates the table header row
    private fun makeTableHeader(): LinearLayout {
        val header = LinearLayout(activity)
        header.orientation = LinearLayout.HORIZONTAL
        header.setPadding(dp(16), dp(10), dp(16), dp(10))
        header.setBackgroundColor(getSectionBackgroundColor())

        listOf(
            Pair("PO #", 1f),
            Pair("Vendor", 2f),
            Pair("Date", 1f),
            Pair("Total", 1f),
            Pair("Status", 1f)
        ).forEach { (text, weight) ->
            val cell = TextView(activity)
            cell.text = text
            cell.textSize = 13f
            cell.setTextColor(getMutedTextColor())
            cell.setTypeface(null, Typeface.BOLD)
            cell.layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                weight
            )

            header.addView(cell)
        }

        return header
    }

    // Creates a filter button
    private fun makeFilterButton(label: String): TextView {
        val btn = TextView(activity)
        btn.text = label
        btn.textSize = 13f
        btn.gravity = Gravity.CENTER
        btn.setPadding(dp(12), dp(6), dp(12), dp(6))
        btn.setTextColor(getSecondaryTextColor())
        btn.setBackgroundColor(getInputBackgroundColor())

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 0, dp(8), 0)
        btn.layoutParams = params

        return btn
    }

    // Creates the New Order button
    private fun makeNewOrderButton(): TextView {
        val btn = TextView(activity)
        btn.text = "+ New Order"
        btn.textSize = 14f
        btn.gravity = Gravity.CENTER
        btn.setPadding(dp(16), dp(8), dp(16), dp(8))
        btn.setTextColor(Color.WHITE)
        btn.setBackgroundColor(getPositiveColor())

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.gravity = Gravity.END
        params.weight = 1f
        params.setMargins(dp(8), 0, 0, 0)
        btn.layoutParams = params

        btn.setOnClickListener { showNewOrderForm() }

        return btn
    }

    // Creates a form label
    private fun makeFormLabel(text: String): TextView {
        val label = TextView(activity)
        label.text = text
        label.textSize = 14f
        label.setTextColor(getSecondaryTextColor())

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, dp(12), 0, dp(4))
        label.layoutParams = params

        return label
    }

    // Creates a form input field
    private fun makeFormInput(hint: String): android.widget.EditText {
        val input = android.widget.EditText(activity)
        input.hint = hint
        input.textSize = 15f
        input.setTextColor(getPrimaryTextColor())
        input.setHintTextColor(getMutedTextColor())
        input.setPadding(dp(12), dp(12), dp(12), dp(12))
        input.setBackgroundColor(getInputBackgroundColor())

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 0, 0, dp(8))
        input.layoutParams = params

        return input
    }

    // Creates an action button
    private fun makeActionButton(
        text: String,
        color: Int,
        onClick: () -> Unit
    ): TextView {
        val btn = TextView(activity)
        btn.text = text
        btn.textSize = 15f
        btn.gravity = Gravity.CENTER
        btn.setTextColor(Color.WHITE)
        btn.setBackgroundColor(color)
        btn.setPadding(dp(16), dp(14), dp(16), dp(14))

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, dp(8), 0, dp(8))
        btn.layoutParams = params

        btn.setOnClickListener { onClick() }

        return btn
    }

    // Returns color based on PO status
    private fun getStatusColor(status: String): Int {
        return when (status) {
            "received" -> getPositiveColor()
            "submitted" -> getPrimaryActionColor()
            "pending review" -> getWarningColor()
            else -> getMutedTextColor()
        }
    }

    private fun isDarkModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    private fun isColorblindModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_COLORBLIND_MODE, false)
    }

    private fun getPageBackgroundColor(): Int {
        // Uses ThemeManager so page backgrounds follow Light or Dark Mode - AF
        return ThemeManager.pageBackground(activity)
    }

    private fun getSectionBackgroundColor(): Int {
        // Uses ThemeManager so section backgrounds follow Light or Dark Mode - AF
        return ThemeManager.sectionBackground(activity)
    }

    private fun getInputBackgroundColor(): Int {
        // Uses ThemeManager so input backgrounds follow Light or Dark Mode - AF
        return ThemeManager.inputBackground(activity)
    }

    private fun getPrimaryTextColor(): Int {
        // Uses ThemeManager so primary text follows Light or Dark Mode - AF
        return ThemeManager.primaryText(activity)
    }

    private fun getSecondaryTextColor(): Int {
        // Uses ThemeManager so secondary text follows Light or Dark Mode - AF
        return ThemeManager.secondaryText(activity)
    }

    private fun getMutedTextColor(): Int {
        // Uses ThemeManager so muted text follows Light or Dark Mode - AF
        return ThemeManager.mutedText(activity)
    }

    private fun getDividerColor(): Int {
        // Uses ThemeManager so dividers follow Light or Dark Mode - AF
        return ThemeManager.divider(activity)
    }

    private fun getPrimaryActionColor(): Int {
        // Uses ThemeManager so action colors support Colorblind Mode - AF
        return ThemeManager.primaryAction(activity)
    }

    private fun getPositiveColor(): Int {
        // Uses ThemeManager so positive colors support Colorblind Mode - AF
        return ThemeManager.positive(activity)
    }

    private fun getWarningColor(): Int {
        // Uses ThemeManager so warning colors are centralized - AF
        return ThemeManager.warning(activity)
    }

    private fun getNegativeColor(): Int {
        // Uses ThemeManager so negative colors support Colorblind Mode - AF
        return ThemeManager.negative(activity)
    }

    private fun getMutedButtonColor(): Int {
        // Uses ThemeManager so muted button colors follow Light or Dark Mode - AF
        return ThemeManager.mutedButton(activity)
    }

    // Converts dp to pixels
    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}
