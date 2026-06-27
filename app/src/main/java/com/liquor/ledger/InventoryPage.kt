package com.liquor.ledger

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore
import com.liquor.ledger.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// InventoryPage displays all products from Firestore
// Manager roles can add new products, adjust stock, and view full inventory details
class InventoryPage(private val activity: Activity) {

    // Firestore database instance
    private val db: FirebaseFirestore = FirebaseManager.db


    // Check if current employee is a manager
    private val isManager = SessionManager.currentEmployee?.position == "Manager"

    // Container for the product list rows
    private lateinit var productListContainer: LinearLayout

    // Summary stat views
    private lateinit var totalProductsText: TextView
    private lateinit var inventoryValueText: TextView
    private lateinit var lowStockText: TextView
    private lateinit var outOfStockText: TextView

    // Current search and filter values
    private var currentSearch = ""
    private var currentCategory = "All"

    fun build(): LinearLayout {

        // ROOT layout — vertical
        val page = LinearLayout(activity)
        page.orientation = LinearLayout.VERTICAL

        // Uses ThemeManager so the page background follows Light or Dark Mode - AF
        page.setBackgroundColor(ThemeManager.pageBackground(activity))

        // TOP BAR — search, filter, and buttons
        val topBar = LinearLayout(activity)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(dp(16), dp(12), dp(16), dp(12))

        // Uses ThemeManager so the top bar background follows the selected theme - AF
        topBar.setBackgroundColor(ThemeManager.pageBackground(activity))

        val topBarParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Search input
        val searchInput = EditText(activity)
        searchInput.hint = "Search products..."
        searchInput.textSize = 14f

        // Uses ThemeManager so the search input follows the selected theme - AF
        searchInput.setTextColor(ThemeManager.primaryText(activity))
        searchInput.setHintTextColor(ThemeManager.mutedText(activity))

        searchInput.setPadding(dp(12), dp(8), dp(12), dp(8))

        searchInput.setBackgroundColor(ThemeManager.inputBackground(activity))

        val searchParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            2f
        )
        searchParams.setMargins(0, 0, dp(8), 0)
        searchInput.layoutParams = searchParams

        // Listens for search input changes
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                currentSearch = s.toString().trim()
                loadProducts()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Category filter dropdown
        val categorySpinner = android.widget.Spinner(activity)
        val categories = arrayOf("All", "Alcohol", "Wine", "Beer", "Spirits", "Snacks", "Other")
        val spinnerAdapter = android.widget.ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_item,
            categories
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter

        val spinnerParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        spinnerParams.setMargins(0, 0, dp(8), 0)
        categorySpinner.layoutParams = spinnerParams

        categorySpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    currentCategory = categories[position]
                    loadProducts()
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

        topBar.addView(searchInput, searchParams)
        topBar.addView(categorySpinner, spinnerParams)

        // Buttons — only show for managers
        if (isManager) {
            val adjustStockBtn = makeTopButton("Adjust Stock", ThemeManager.primaryAction(activity))
            adjustStockBtn.setOnClickListener { showAdjustStockDialog() }

            val addProductBtn = makeTopButton("+ Add Product", ThemeManager.positive(activity))
            addProductBtn.setOnClickListener { showAddProductDialog() }

            topBar.addView(adjustStockBtn)
            topBar.addView(addProductBtn)
        }

        // SUMMARY STATS ROW
        val statsRow = LinearLayout(activity)
        statsRow.orientation = LinearLayout.HORIZONTAL
        statsRow.setPadding(dp(16), dp(8), dp(16), dp(8))

        // Uses ThemeManager so the stats row background follows the selected theme - AF
        statsRow.setBackgroundColor(ThemeManager.sectionBackground(activity))

        val statsParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        totalProductsText = makeStatView("Total Products", "0", ThemeManager.primaryText(activity))

        inventoryValueText = makeStatView("Inventory Value", "$0.00", ThemeManager.primaryAction(activity))

        lowStockText = makeStatView("Low Stock", "0", ThemeManager.warning(activity))

        outOfStockText = makeStatView("Out of Stock", "0", ThemeManager.negative(activity))

        statsRow.addView(totalProductsText)
        statsRow.addView(inventoryValueText)
        statsRow.addView(lowStockText)
        statsRow.addView(outOfStockText)

        // TABLE HEADER
        val tableHeader = makeTableHeader()

        // SCROLLABLE PRODUCT LIST
        val scrollView = ScrollView(activity)

        // Uses ThemeManager so the scroll area follows the selected theme - AF
        scrollView.setBackgroundColor(ThemeManager.pageBackground(activity))

        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        productListContainer = LinearLayout(activity)
        productListContainer.orientation = LinearLayout.VERTICAL

        // Uses ThemeManager so the product list background follows the selected theme - AF
        productListContainer.setBackgroundColor(ThemeManager.pageBackground(activity))

        scrollView.addView(productListContainer)

        // Add everything to page
        page.addView(topBar, topBarParams)
        page.addView(statsRow, statsParams)
        page.addView(tableHeader)
        page.addView(scrollView, scrollParams)

        // Load products from Firestore
        loadProducts()

        return page
    }

    // Loads products from Firestore and displays them
    private fun loadProducts() {
        productListContainer.removeAllViews()

        // Show loading
        val loadingText = TextView(activity)
        loadingText.text = "Loading inventory..."
        loadingText.textSize = 14f

        // Uses ThemeManager so loading text follows the selected theme - AF
        loadingText.setTextColor(ThemeManager.mutedText(activity))

        loadingText.setPadding(dp(16), dp(16), dp(16), dp(16))
        productListContainer.addView(loadingText)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("products").get().await()

                val allProducts = snapshot.documents.mapNotNull { doc ->
                    mapOf(
                        "docId" to doc.id,
                        "name" to (doc.getString("name") ?: ""),
                        "sku" to (doc.getString("sku") ?: "—"),
                        "category" to (doc.getString("category") ?: ""),
                        "vendor" to (doc.getString("vendor") ?: "—"),
                        "stock" to (doc.getLong("stock")?.toString() ?: "0"),
                        "reorderPoint" to (doc.getLong("reorderPoint")?.toString() ?: "0"),
                        "cost" to (doc.getDouble("cost")?.toString() ?: "0.0"),
                        "taxPercent" to (doc.getDouble("taxPercent")?.toString() ?: "0.0"),
                        "marginPercent" to (doc.getDouble("marginPercent")?.toString() ?: "0.0"),
                        "price" to (doc.getDouble("price")?.toString() ?: "0.0"),
                        "stockValue" to (
                                (doc.getLong("stock")?.toDouble() ?: 0.0) *
                                        (doc.getDouble("cost") ?: 0.0)
                                ).toString()
                    )
                }

                // Apply search filter
                val searched = if (currentSearch.isEmpty()) {
                    allProducts
                } else {
                    allProducts.filter { product ->
                        product["name"]?.contains(currentSearch, ignoreCase = true) == true ||
                                product["sku"]?.contains(currentSearch, ignoreCase = true) == true ||
                                product["vendor"]?.contains(currentSearch, ignoreCase = true) == true
                    }
                }

                // Apply category filter
                val filtered = if (currentCategory == "All") {
                    searched
                } else {
                    searched.filter { it["category"] == currentCategory }
                }

                // Calculate stats
                val totalProducts = allProducts.size

                val inventoryValue = allProducts.sumOf {
                    it["stockValue"]?.toDoubleOrNull() ?: 0.0
                }

                val lowStock = allProducts.count { product ->
                    val stock = product["stock"]?.toIntOrNull() ?: 0
                    val reorder = product["reorderPoint"]?.toIntOrNull() ?: 0
                    stock in 1..reorder
                }

                val outOfStock = allProducts.count { product ->
                    (product["stock"]?.toIntOrNull() ?: 0) == 0
                }

                withContext(Dispatchers.Main) {
                    // Update stats
                    totalProductsText.text = "Total Products\n$totalProducts"
                    inventoryValueText.text = "Inventory Value\n$${"%.2f".format(inventoryValue)}"
                    lowStockText.text = "Low Stock\n! $lowStock"
                    outOfStockText.text = "Out of Stock\nX $outOfStock"

                    productListContainer.removeAllViews()

                    if (filtered.isEmpty()) {
                        val emptyText = TextView(activity)
                        emptyText.text = "No products found"
                        emptyText.textSize = 14f

                        // Uses ThemeManager so empty text follows the selected theme - AF
                        emptyText.setTextColor(ThemeManager.mutedText(activity))

                        emptyText.setPadding(dp(16), dp(16), dp(16), dp(16))
                        productListContainer.addView(emptyText)
                    } else {
                        filtered.forEach { product ->
                            productListContainer.addView(makeProductRow(product))

                            // Add thin divider line between rows
                            val divider = android.view.View(activity)

                            // Uses ThemeManager so dividers follow Light or Dark Mode - AF
                            divider.setBackgroundColor(ThemeManager.divider(activity))

                            productListContainer.addView(
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
                    productListContainer.removeAllViews()

                    val errorText = TextView(activity)
                    errorText.text = "Error loading inventory: ${e.message}"
                    errorText.textSize = 14f

                    // Uses ThemeManager so error text supports Colorblind Mode - AF
                    errorText.setTextColor(ThemeManager.negative(activity))

                    errorText.setPadding(dp(16), dp(16), dp(16), dp(16))

                    productListContainer.addView(errorText)
                }
            }
        }
    }

    // Creates a single product row
    private fun makeProductRow(product: Map<String, String>): LinearLayout {

        val stock = product["stock"]?.toIntOrNull() ?: 0
        val reorderPoint = product["reorderPoint"]?.toIntOrNull() ?: 0

        // Determine stock status
        val status = when {
            stock == 0 -> "Out of Stock"
            stock <= reorderPoint -> "Low Stock"
            else -> ""
        }

        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(dp(16), dp(12), dp(16), dp(12))

        // Uses ThemeManager so each product row follows the selected theme - AF
        row.setBackgroundColor(ThemeManager.pageBackground(activity))

        row.gravity = Gravity.CENTER_VERTICAL

        // Product name — clickable link
        val nameCell = TextView(activity)
        nameCell.text = product["name"] ?: ""
        nameCell.textSize = 14f

        // Uses ThemeManager so product links support Colorblind Mode - AF
        nameCell.setTextColor(ThemeManager.primaryAction(activity))

        nameCell.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            2f
        )

        nameCell.setOnClickListener {
            if (isManager) showEditProductDialog(product)
        }

        row.addView(nameCell)
        row.addView(makeCell(product["sku"] ?: "—", 1f))
        row.addView(makeCell(product["category"] ?: "", 1f))
        row.addView(makeCell(product["vendor"] ?: "—", 1f))

        // Stock cell — colored based on status
        val stockCell = TextView(activity)

        val stockColor = when {
            stock == 0 -> ThemeManager.negative(activity)
            stock <= reorderPoint -> ThemeManager.warning(activity)
            else -> ThemeManager.secondaryText(activity)
        }

        val stockPrefix = when {
            stock == 0 -> "X "
            stock <= reorderPoint -> "! "
            else -> ""
        }

        stockCell.text = "$stockPrefix$stock"
        stockCell.textSize = 14f
        stockCell.setTextColor(stockColor)
        stockCell.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        row.addView(stockCell)

        // Reorder point — colored if stock is at or below it
        val reorderCell = TextView(activity)

        reorderCell.text = if (stock <= reorderPoint && stock > 0) {
            "! $reorderPoint"
        } else {
            reorderPoint.toString()
        }

        reorderCell.textSize = 14f

        reorderCell.setTextColor(
            if (stock <= reorderPoint && stock > 0) {
                ThemeManager.warning(activity)
            } else {
                ThemeManager.secondaryText(activity)
            }
        )

        reorderCell.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        row.addView(reorderCell)

        row.addView(makeCell("$${product["cost"]}", 1f))
        row.addView(makeCell("${product["taxPercent"]}%", 1f))
        row.addView(makeCell("${product["marginPercent"]}%", 1f))

        row.addView(
            makeCell(
                "$${"%.2f".format(product["stockValue"]?.toDoubleOrNull() ?: 0.0)}",
                1f
            )
        )

        // Status cell
        val statusCell = TextView(activity)
        statusCell.text = status
        statusCell.textSize = 12f

        // Uses ThemeManager so row status colors support Colorblind Mode - AF
        when (status) {
            "Out of Stock" -> ThemeManager.negative(activity)
            "Low Stock" -> ThemeManager.warning(activity)
            else -> ThemeManager.secondaryText(activity)
        }

        statusCell.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        row.addView(statusCell)

        return row
    }

    // Shows a dialog with full product details and edit options
    // Shows a dialog allowing managers to edit an existing product's details
    private fun showEditProductDialog(product: Map<String, String>) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Edit Product")

        val form = LinearLayout(activity)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(20), dp(10), dp(20), dp(10))

        val nameInput = makeDialogInput(form, "Product Name", product["name"] ?: "")
        val skuInput = makeDialogInput(form, "SKU", product["sku"]?.takeIf { it != "—" } ?: "")
        val categoryInput = makeDialogInput(form, "Category", product["category"] ?: "")
        val vendorInput = makeDialogInput(form, "Vendor", product["vendor"]?.takeIf { it != "—" } ?: "")
        val reorderInput = makeDialogInput(form, "Reorder Point",
            product["reorderPoint"] ?: "0", isNumber = true)
        val costInput = makeDialogInput(form, "Cost ($)",
            product["cost"] ?: "0.0", isDecimal = true)
        val priceInput = makeDialogInput(form, "Price ($)",
            product["price"] ?: "0.0", isDecimal = true)
        val taxInput = makeDialogInput(form, "Tax %",
            product["taxPercent"] ?: "0.0", isDecimal = true)
        val marginInput = makeDialogInput(form, "Margin %",
            product["marginPercent"] ?: "0.0", isDecimal = true)

        builder.setView(form)

        builder.setPositiveButton("Save Changes") { _, _ ->
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                android.widget.Toast.makeText(
                    activity, "Product name is required",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val updates = mapOf(
                        "name" to name,
                        "sku" to skuInput.text.toString().trim(),
                        "category" to categoryInput.text.toString().trim(),
                        "vendor" to vendorInput.text.toString().trim(),
                        "reorderPoint" to (reorderInput.text.toString().toLongOrNull() ?: 0L),
                        "cost" to (costInput.text.toString().toDoubleOrNull() ?: 0.0),
                        "price" to (priceInput.text.toString().toDoubleOrNull() ?: 0.0),
                        "taxPercent" to (taxInput.text.toString().toDoubleOrNull() ?: 0.0),
                        "marginPercent" to (marginInput.text.toString().toDoubleOrNull() ?: 0.0)
                    )

                    db.collection("products")
                        .document(product["docId"] ?: "")
                        .update(updates)
                        .await()

                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            activity, "$name updated successfully",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        loadProducts()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            activity, "Error: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // Shows dialog to add a new product
    private fun showAddProductDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Add New Product")

        val form = LinearLayout(activity)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(20), dp(10), dp(20), dp(10))

        // Form fields
        val nameInput = makeDialogInput(form, "Product Name *")
        val skuInput = makeDialogInput(form, "SKU")
        val categoryInput = makeDialogInput(form, "Category")
        val vendorInput = makeDialogInput(form, "Vendor")
        val stockInput = makeDialogInput(form, "Initial Stock", isNumber = true)
        val reorderInput = makeDialogInput(form, "Reorder Point", isNumber = true)
        val costInput = makeDialogInput(form, "Cost ($)", isDecimal = true)
        val priceInput = makeDialogInput(form, "Price ($)", isDecimal = true)
        val taxInput = makeDialogInput(form, "Tax %", isDecimal = true)
        val marginInput = makeDialogInput(form, "Margin %", isDecimal = true)

        builder.setView(form)

        builder.setPositiveButton("Add Product") { _, _ ->
            val name = nameInput.text.toString().trim()

            if (name.isEmpty()) {
                android.widget.Toast.makeText(
                    activity,
                    "Product name is required",
                    android.widget.Toast.LENGTH_SHORT
                ).show()

                return@setPositiveButton
            }

            // Write new product to Firestore
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val newProduct = hashMapOf(
                        "name" to name,
                        "sku" to skuInput.text.toString().trim(),
                        "category" to categoryInput.text.toString().trim(),
                        "vendor" to vendorInput.text.toString().trim(),
                        "stock" to (stockInput.text.toString().toLongOrNull() ?: 0L),
                        "reorderPoint" to (reorderInput.text.toString().toLongOrNull() ?: 0L),
                        "cost" to (costInput.text.toString().toDoubleOrNull() ?: 0.0),
                        "price" to (priceInput.text.toString().toDoubleOrNull() ?: 0.0),
                        "taxPercent" to (taxInput.text.toString().toDoubleOrNull() ?: 0.0),
                        "marginPercent" to (marginInput.text.toString().toDoubleOrNull() ?: 0.0)
                    )

                    db.collection("products").add(newProduct).await()

                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            activity,
                            "$name added to inventory",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()

                        loadProducts()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            activity,
                            "Error: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // Shows dialog to adjust stock for a product
    // Shows dialog to adjust stock for a product using a dropdown
    private fun showAdjustStockDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Adjust Stock")

        val form = LinearLayout(activity)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(20), dp(10), dp(20), dp(10))

        val productLabel = makeFormLabelForDialog("Select Product")
        val productSpinner = android.widget.Spinner(activity)

        val spinnerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        spinnerParams.setMargins(0, 0, 0, dp(8))
        productSpinner.layoutParams = spinnerParams

        val adjustLabel = makeFormLabelForDialog("Adjust Amount (+ or -)")
        val adjustAmountInput = EditText(activity)
        adjustAmountInput.hint = "e.g. 10 or -5"
        adjustAmountInput.textSize = 14f

        // Uses ThemeManager so amount inputs follow the selected theme - AF
        adjustAmountInput.setTextColor(ThemeManager.primaryText(activity))
        adjustAmountInput.setHintTextColor(ThemeManager.mutedText(activity))
        adjustAmountInput.setPadding(dp(8), dp(8), dp(8), dp(8))

        adjustAmountInput.setBackgroundColor(ThemeManager.inputBackground(activity))

        adjustAmountInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
            android.text.InputType.TYPE_NUMBER_FLAG_SIGNED

        val noteText = TextView(activity)
        noteText.text = "Enter a positive number to add stock or negative to remove."
        noteText.textSize = 12f

        // Uses ThemeManager so note text follows the selected theme - AF
        noteText.setTextColor(ThemeManager.mutedText(activity))

        noteText.setPadding(0, dp(4), 0, dp(8))

        form.addView(productLabel)
        form.addView(productSpinner, spinnerParams)
        form.addView(adjustLabel)
        form.addView(adjustAmountInput)
        form.addView(noteText)

        builder.setView(form)

        builder.setPositiveButton("Adjust", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()

        // Load products into the spinner
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("products").get().await()
                val products = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: ""
                    val currentStock = doc.getLong("stock") ?: 0L
                    if (name.isNotEmpty()) Pair(name, currentStock) else null
                }

                withContext(Dispatchers.Main) {
                    val productNames = products.map { "${it.first} (current: ${it.second})" }
                    val adapter = android.widget.ArrayAdapter(
                        activity,
                        android.R.layout.simple_spinner_item,
                        productNames
                    )
                    adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item)
                    productSpinner.adapter = adapter

                    dialog.show()

                    // Override positive button so dialog doesn't auto-close on error
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val selectedIndex = productSpinner.selectedItemPosition
                        if (selectedIndex < 0 || products.isEmpty()) {
                            android.widget.Toast.makeText(
                                activity, "Please select a product",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        val selectedProductName = products[selectedIndex].first
                        val adjustAmount = adjustAmountInput.text.toString()
                            .trim().toIntOrNull()

                        if (adjustAmount == null) {
                            android.widget.Toast.makeText(
                                activity, "Please enter a valid amount",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        adjustProductStock(selectedProductName, adjustAmount)
                        dialog.dismiss()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity, "Error loading products: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                }
            }
        }
    }

    // Performs the actual stock adjustment in Firestore
    private fun adjustProductStock(productName: String, adjustAmount: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("products")
                    .whereEqualTo("name", productName)
                    .get()
                    .await()

                if (snapshot.isEmpty) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            activity, "Product not found",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val doc = snapshot.documents[0]
                val currentStock = doc.getLong("stock") ?: 0L
                val newStock = maxOf(0L, currentStock + adjustAmount)

                db.collection("products")
                    .document(doc.id)
                    .update("stock", newStock)
                    .await()

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Stock updated: $currentStock -> $newStock",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    loadProducts()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity, "Error: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Creates a table header row
    private fun makeTableHeader(): LinearLayout {
        val header = LinearLayout(activity)
        header.orientation = LinearLayout.HORIZONTAL
        header.setPadding(dp(16), dp(10), dp(16), dp(10))

        // Uses ThemeManager so the table header background follows the selected theme - AF
        header.setBackgroundColor(ThemeManager.sectionBackground(activity))

        val columns = listOf(
            Pair("Product", 2f),
            Pair("SKU", 1f),
            Pair("Category", 1f),
            Pair("Vendor", 1f),
            Pair("Stock", 1f),
            Pair("Reorder Pt", 1f),
            Pair("Cost", 1f),
            Pair("Tax%", 1f),
            Pair("Margin%", 1f),
            Pair("Stock Value", 1f),
            Pair("Status", 1f)
        )

        columns.forEach { (text, weight) ->
            val cell = TextView(activity)
            cell.text = text
            cell.textSize = 12f

            // Uses ThemeManager so table header text follows the selected theme - AF
            cell.setTextColor(ThemeManager.mutedText(activity))

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

    // Creates a standard table cell
    private fun makeCell(text: String, weight: Float): TextView {
        val cell = TextView(activity)
        cell.text = text
        cell.textSize = 13f
        cell.setTextColor(ThemeManager.secondaryText(activity))
        cell.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            weight
        )

        return cell
    }

    // Creates a stat summary view
    private fun makeStatView(label: String, value: String, color: Int): TextView {
        val view = TextView(activity)
        view.text = "$label\n$value"
        view.textSize = 13f
        view.setTextColor(color)
        view.gravity = Gravity.CENTER
        view.setPadding(dp(8), dp(8), dp(8), dp(8))
        view.
        layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        return view
    }

    // Creates a top bar button
    private fun makeTopButton(text: String, color: Int): TextView {
        val btn = TextView(activity)
        btn.text = text
        btn.textSize = 13f
        btn.gravity = Gravity.CENTER
        btn.setTextColor(Color.WHITE)
        btn.setBackgroundColor(color)
        btn.setPadding(dp(12), dp(8), dp(12), dp(8))

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(dp(8), 0, 0, 0)
        btn.layoutParams = params

        return btn
    }

    // Creates an input field for dialogs
    private fun makeDialogInput(
        parent: LinearLayout,
        hint: String,
        defaultValue: String = "",
        isNumber: Boolean = false,
        isDecimal: Boolean = false
    ): EditText {
        val label = TextView(activity)
        label.text = hint
        label.textSize = 13f

        // Uses ThemeManager so dialog labels follow the selected theme - AF
        label.setTextColor(ThemeManager.mutedText(activity))

        label.setPadding(0, dp(8), 0, dp(2))
        parent.addView(label)

        val input = EditText(activity)
        input.hint = hint
        if (defaultValue.isNotEmpty()) input.setText(defaultValue)
        input.textSize = 14f

        // Uses ThemeManager so the dialog input follows the selected theme - AF
        input.setTextColor(ThemeManager.primaryText(activity))
        input.setHintTextColor(ThemeManager.mutedText(activity))

        input.setPadding(dp(8), dp(8), dp(8), dp(8))

        input.setBackgroundColor(ThemeManager.inputBackground(activity))

        input.inputType = when {
            isDecimal -> android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            isNumber -> android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            else -> android.text.InputType.TYPE_CLASS_TEXT
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, dp(4))
        input.layoutParams = params
        parent.addView(input)

        return input
    }

    private fun makeFormLabelForDialog(text: String): TextView {
        val label = TextView(activity)
        label.text = text
        label.textSize = 13f

        // Uses ThemeManager so form labels follow the selected theme - AF
        label.setTextColor(ThemeManager.mutedText(activity))

        label.setPadding(0, dp(8), 0, dp(2))
        return label
    }

    // Converts dp to pixels
    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}
