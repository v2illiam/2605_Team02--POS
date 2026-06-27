package com.liquor.ledger

    import android.app.Activity
    import android.graphics.Color
    import android.text.Editable
    import android.text.TextWatcher
    import android.widget.Button
    import android.widget.EditText
    import android.widget.LinearLayout
    import android.widget.TextView
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale
    import android.widget.Button
    import android.widget.EditText


    import com.liquor.ledger.firebase.FirebaseManager

    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale

    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.tasks.await
    import kotlinx.coroutines.withContext


            val headerBox = LinearLayout(activity)
            headerBox.orientation = LinearLayout.HORIZONTAL
            headerBox.setPadding(30, 25, 30, 20)
            headerBox.setBackgroundColor(Color.WHITE)

            val headerParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val leftHeader = LinearLayout(activity)
            leftHeader.orientation = LinearLayout.VERTICAL

            val leftHeaderParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            val headerTitle = TextView(activity)
            headerTitle.text = "Point of Sale / Register"
            headerTitle.textSize = 24f
            headerTitle.setTextColor(Color.BLACK)

            val headerDate = TextView(activity)
            headerDate.textSize = 14f
            headerDate.setTextColor(Color.GRAY)

            val dateFormat = SimpleDateFormat(
                "EEEE, MMMM d, yyyy • h:mm a",
                Locale.getDefault()
            )

            headerDate.text = dateFormat.format(Date())

            leftHeader.addView(headerTitle)
            leftHeader.addView(headerDate)

            val rightHeader = LinearLayout(activity)
            rightHeader.orientation = LinearLayout.VERTICAL

            val cashDrawerText = TextView(activity)
            cashDrawerText.text = "Cash Drawer: \$0.00"
            cashDrawerText.textSize = 16f
            cashDrawerText.setTextColor(Color.BLACK)

            val drawerStatusText = TextView(activity)
            drawerStatusText.text = "Drawer Closed"
            drawerStatusText.textSize = 13f
            drawerStatusText.setTextColor(Color.GRAY)

            rightHeader.addView(cashDrawerText)
            rightHeader.addView(drawerStatusText)

            headerBox.addView(leftHeader, leftHeaderParams)
            headerBox.addView(rightHeader)

            leftSide.addView(headerBox, headerParams)

            val entryRow = LinearLayout(activity)
            entryRow.orientation = LinearLayout.HORIZONTAL
            entryRow.setPadding(30, 20, 30, 20)
            entryRow.setBackgroundColor(Color.WHITE)

            val entryRowParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            entryRowParams.setMargins(0, 10, 0, 10)

            val searchBox = EditText(activity)
            searchBox.hint = "Scan or search product"
            searchBox.textSize = 16f
            searchBox.setSingleLine(true)

            val searchParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                2f
            )

            val qtyBox = EditText(activity)
            qtyBox.hint = "Qty"
            qtyBox.textSize = 16f
            qtyBox.setSingleLine(true)

            val smallInputParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            smallInputParams.setMargins(10, 0, 0, 0)

            val priceBox = EditText(activity)
            priceBox.hint = "Price"
            priceBox.textSize = 16f
            priceBox.setSingleLine(true)

            val discountBox = EditText(activity)
            discountBox.hint = "Discount %"
            discountBox.textSize = 16f
            discountBox.setSingleLine(true)

            val taxBox = EditText(activity)
            taxBox.hint = "Tax %"
            taxBox.textSize = 16f
            taxBox.setSingleLine(true)

            val addButton = Button(activity)
            addButton.text = "Add"

            val clearButton = Button(activity)
            clearButton.text = "Clear"

            entryRow.addView(searchBox, searchParams)
            entryRow.addView(qtyBox, smallInputParams)
            entryRow.addView(priceBox, smallInputParams)
            entryRow.addView(discountBox, smallInputParams)
            entryRow.addView(taxBox, smallInputParams)
            entryRow.addView(addButton)
            entryRow.addView(clearButton)

            leftSide.addView(entryRow, entryRowParams)


            val rightPanel = LinearLayout(activity)
            rightPanel.orientation = LinearLayout.VERTICAL
            rightPanel.setBackgroundColor(Color.rgb(235, 239, 245))

class POSPage(private val activity: Activity) {

    private val db = FirebaseManager.db

    private var selectedProductName = ""
    private var selectedProductSku = ""
    private var selectedProductPrice = 0.0
    private var selectedProductTax = 0.0
    private var selectedProductStock = 0
    private var cartSubtotal = 0.0
    private var cartTax = 0.0
    private var cartTotal = 0.0
    private val cartItems = mutableListOf<MutableMap<String, Any>>()
    private var amountPaid = 0.0
    private var remainingBalance = 0.0
    private var changeDue = 0.0
    private lateinit var remainingBalanceAmount: TextView
    private lateinit var paymentAmountBox: EditText

    fun build(): LinearLayout {

        val page = LinearLayout(activity)
        page.orientation = LinearLayout.HORIZONTAL

        // Changes main POS page background based on Settings - AF
        page.setBackgroundColor(ThemeManager.pageBackground(activity))

        val leftSide = LinearLayout(activity)
        leftSide.orientation = LinearLayout.VERTICAL

        // Changes left POS area background based on Settings - AF
        leftSide.setBackgroundColor(ThemeManager.sectionBackground(activity))

        leftSide.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        val leftParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            3f
        )

        val headerBox = LinearLayout(activity)
        headerBox.orientation = LinearLayout.HORIZONTAL
        headerBox.setPadding(30, 25, 30, 20)

        // Changes header background based on Settings - AF
        headerBox.setBackgroundColor(ThemeManager.pageBackground(activity))

        val headerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val leftHeader = LinearLayout(activity)
        leftHeader.orientation = LinearLayout.VERTICAL

        val leftHeaderParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val headerTitle = TextView(activity)
        headerTitle.text = "Point of Sale / Register"
        headerTitle.textSize = 24f

        // Changes header title color based on Settings - AF
        headerTitle.setTextColor(ThemeManager.primaryText(activity))

        val headerDate = TextView(activity)
        headerDate.textSize = 14f

        // Changes header date color based on Settings - AF
        headerDate.setTextColor(ThemeManager.mutedText(activity))

        val dateFormat = SimpleDateFormat(
            "EEEE, MMMM d, yyyy • h:mm a",
            Locale.getDefault()
        )

        headerDate.text = dateFormat.format(Date())

        leftHeader.addView(headerTitle)
        leftHeader.addView(headerDate)

        val rightHeader = LinearLayout(activity)
        rightHeader.orientation = LinearLayout.VERTICAL

        val cashDrawerText = TextView(activity)
        cashDrawerText.text = "Cash Drawer: \$0.00"
        cashDrawerText.textSize = 16f

        // Changes cash drawer text color based on Settings - AF
        cashDrawerText.setTextColor(ThemeManager.primaryText(activity))

        val drawerStatusText = TextView(activity)
        drawerStatusText.text = "Drawer Closed"
        drawerStatusText.textSize = 13f

        // Changes drawer status text color based on Settings - AF
        drawerStatusText.setTextColor(ThemeManager.mutedText(activity))

        rightHeader.addView(cashDrawerText)
        rightHeader.addView(drawerStatusText)

        headerBox.addView(leftHeader, leftHeaderParams)
        headerBox.addView(rightHeader)

        leftSide.addView(headerBox, headerParams)

        val entryRow = LinearLayout(activity)
        entryRow.orientation = LinearLayout.HORIZONTAL
        entryRow.setPadding(30, 20, 30, 20)

        // Changes entry row background based on Settings - AF
        entryRow.setBackgroundColor(ThemeManager.pageBackground(activity))

        val entryRowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        entryRowParams.setMargins(0, 10, 0, 10)

        val searchBox = EditText(activity)

        searchBox.setTextColor(Color.BLACK)
        searchBox.setHintTextColor(Color.DKGRAY)
        searchBox.setBackgroundColor(Color.rgb(245, 245, 245))

        // Changes search box colors based on Settings - AF
        searchBox.setTextColor(ThemeManager.primaryText(activity))
        searchBox.setHintTextColor(ThemeManager.secondaryText(activity))
        searchBox.setBackgroundColor(ThemeManager.inputBackground(activity))

        val searchParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            3f
        )

        val qtyBox = EditText(activity)

        // Changes quantity box colors based on Settings - AF
        qtyBox.setTextColor(ThemeManager.primaryText(activity))
        qtyBox.setHintTextColor(ThemeManager.secondaryText(activity))
        qtyBox.setBackgroundColor(ThemeManager.inputBackground(activity))

        qtyBox.hint = "Qty"
        qtyBox.textSize = 16f
        qtyBox.setSingleLine(true)

        val smallInputParams = LinearLayout.LayoutParams(
            140,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        smallInputParams.setMargins(10, 0, 0, 0)

        val priceBox = EditText(activity)

        // Changes price box colors based on Settings - AF
        priceBox.setTextColor(ThemeManager.primaryText(activity))
        priceBox.setHintTextColor(ThemeManager.secondaryText(activity))
        priceBox.setBackgroundColor(ThemeManager.inputBackground(activity))

        priceBox.hint = "Price"
        priceBox.textSize = 16f
        priceBox.setSingleLine(true)

        val discountBox = EditText(activity)

        // Changes discount box colors based on Settings - AF
        discountBox.setTextColor(ThemeManager.primaryText(activity))
        discountBox.setHintTextColor(ThemeManager.secondaryText(activity))
        discountBox.setBackgroundColor(ThemeManager.inputBackground(activity))

        discountBox.hint = "Discount %"
        discountBox.textSize = 16f
        discountBox.setSingleLine(true)

        val taxBox = EditText(activity)

        // Changes tax box colors based on Settings - AF
        taxBox.setTextColor(ThemeManager.primaryText(activity))
        taxBox.setHintTextColor(ThemeManager.secondaryText(activity))
        taxBox.setBackgroundColor(ThemeManager.inputBackground(activity))

        taxBox.hint = "Tax %"
        taxBox.textSize = 16f
        taxBox.setSingleLine(true)

        val addButton = Button(activity)
        addButton.text = "Add"

        val clearButton = Button(activity)
        clearButton.text = "Clear"

        entryRow.addView(searchBox, searchParams)
        entryRow.addView(qtyBox, smallInputParams)
        entryRow.addView(priceBox, smallInputParams)
        entryRow.addView(discountBox, smallInputParams)
        entryRow.addView(taxBox, smallInputParams)
        entryRow.addView(addButton)
        entryRow.addView(clearButton)

        leftSide.addView(entryRow, entryRowParams)

        val suggestionList = LinearLayout(activity)
        suggestionList.orientation = LinearLayout.VERTICAL

        // Changes suggestion list background based on Settings - AF
        suggestionList.setBackgroundColor(ThemeManager.pageBackground(activity))

        val suggestionListParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        suggestionListParams.setMargins(30, 0, 30, 10)

        leftSide.addView(suggestionList, suggestionListParams)

        var isSelectingProduct = false


        val cartBox = LinearLayout(activity)
        cartBox.orientation = LinearLayout.VERTICAL
        cartBox.setPadding(30, 20, 30, 20)

        // Changes cart box background based on Settings - AF
        cartBox.setBackgroundColor(ThemeManager.pageBackground(activity))

        val cartBoxParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        cartBoxParams.setMargins(0, 10, 0, 10)

        val cartHeader = LinearLayout(activity)
        cartHeader.orientation = LinearLayout.HORIZONTAL
        cartHeader.setPadding(10, 10, 10, 10)

        val productHeader = TextView(activity)
        productHeader.text = "Product"
        productHeader.textSize = 15f

        // Changes product header text color based on Settings - AF
        productHeader.setTextColor(ThemeManager.primaryText(activity))

        val qtyHeader = TextView(activity)
        qtyHeader.text = "Qty"
        qtyHeader.textSize = 15f

        // Changes quantity header text color based on Settings - AF
        qtyHeader.setTextColor(ThemeManager.primaryText(activity))

        val priceHeader = TextView(activity)
        priceHeader.text = "Price"
        priceHeader.textSize = 15f

        // Changes price header text color based on Settings - AF
        priceHeader.setTextColor(ThemeManager.primaryText(activity))

        val discountHeader = TextView(activity)
        discountHeader.text = "Disc %"
        discountHeader.textSize = 15f

        // Changes discount header text color based on Settings - AF
        discountHeader.setTextColor(ThemeManager.primaryText(activity))

        val taxHeader = TextView(activity)
        taxHeader.text = "Tax %"
        taxHeader.textSize = 15f

        // Changes tax header text color based on Settings - AF
        taxHeader.setTextColor(ThemeManager.primaryText(activity))

        val totalHeader = TextView(activity)
        totalHeader.text = "Line Total"
        totalHeader.textSize = 15f

        // Changes total header text color based on Settings - AF
        totalHeader.setTextColor(ThemeManager.primaryText(activity))

        val wideColumn = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            2f
        )

        val smallColumn = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        cartHeader.addView(productHeader, wideColumn)
        cartHeader.addView(qtyHeader, smallColumn)
        cartHeader.addView(priceHeader, smallColumn)
        cartHeader.addView(discountHeader, smallColumn)
        cartHeader.addView(taxHeader, smallColumn)
        cartHeader.addView(totalHeader, smallColumn)

        val removeHeader = TextView(activity)
        removeHeader.text = "Remove"
        removeHeader.textSize = 15f

        // Changes remove header text color based on Settings - AF
        removeHeader.setTextColor(ThemeManager.primaryText(activity))

        cartHeader.addView(removeHeader, smallColumn)

        val emptyCartText = TextView(activity)
        emptyCartText.text = "No items in cart"
        emptyCartText.textSize = 16f

        // Changes empty cart text color based on Settings - AF
        emptyCartText.setTextColor(ThemeManager.mutedText(activity))

        emptyCartText.setPadding(10, 40, 10, 10)

        cartBox.addView(cartHeader)
        cartBox.addView(emptyCartText)

        val cartScrollView = ScrollView(activity)
        cartScrollView.isVerticalScrollBarEnabled = true
        cartScrollView.isScrollbarFadingEnabled = false

        val cartItemsContainer = LinearLayout(activity)
        cartItemsContainer.orientation = LinearLayout.VERTICAL

        cartScrollView.addView(cartItemsContainer)

        val cartScrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        cartBox.addView(cartScrollView, cartScrollParams)


        val totalsRow = LinearLayout(activity)
        totalsRow.orientation = LinearLayout.HORIZONTAL
        totalsRow.setPadding(30, 8, 30, 8)

        // Changes totals row background based on Settings - AF
        totalsRow.setBackgroundColor(ThemeManager.pageBackground(activity))

        totalsRow.elevation = 8f

        leftSide.addView(cartBox, cartBoxParams)

        val totalsRowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        totalsRowParams.setMargins(0, 10, 0, 0)

        val subtotalText = TextView(activity)
        subtotalText.text = "Subtotal: $0.00"
        subtotalText.textSize = 16f

        // Changes subtotals text color based on Settings - AF
        subtotalText.setTextColor(ThemeManager.primaryText(activity))

        val taxText = TextView(activity)
        taxText.text = "Tax: $0.00"
        taxText.textSize = 16f

        // Changes tax text color based on Settings - AF
        taxText.setTextColor(ThemeManager.primaryText(activity))

        val totalText = TextView(activity)
        totalText.text = "Total: $0.00"
        totalText.textSize = 18f

        // Changes totals text color based on Settings - AF
        totalText.setTextColor(ThemeManager.primaryText(activity))

        val totalsTextParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val newTransactionButton = Button(activity)
        newTransactionButton.text = "New Transaction"

        val completeTransactionButton = Button(activity)
        completeTransactionButton.text = "Complete Transaction"

        totalsRow.addView(subtotalText, totalsTextParams)
        totalsRow.addView(taxText, totalsTextParams)
        totalsRow.addView(totalText, totalsTextParams)
        totalsRow.addView(newTransactionButton)
        totalsRow.addView(completeTransactionButton)

        fun addSelectedProductToCart() {

            if (selectedProductName.isEmpty()) {
                Toast.makeText(activity, "Select a product first", Toast.LENGTH_SHORT).show()
                return
            }

            val quantity = qtyBox.text.toString().toIntOrNull() ?: 1

            if (quantity <= 0) {
                Toast.makeText(activity, "Enter a valid quantity", Toast.LENGTH_SHORT).show()
                return
            }

            if (selectedProductStock > 0 && quantity > selectedProductStock) {
                Toast.makeText(activity, "Not enough stock available", Toast.LENGTH_SHORT).show()
                return
            }

            emptyCartText.visibility = android.view.View.GONE

            val lineSubtotal = selectedProductPrice * quantity
            val lineTax = lineSubtotal * (selectedProductTax / 100)
            val lineTotal = lineSubtotal + lineTax
            val cartItem = mutableMapOf<String, Any>(
                "name" to selectedProductName,
                "sku" to selectedProductSku,
                "quantity" to quantity,
                "price" to selectedProductPrice,
                "taxPercent" to selectedProductTax,
                "lineSubtotal" to lineSubtotal,
                "lineTax" to lineTax,
                "lineTotal" to lineTotal
            )

            cartItems.add(cartItem)

            cartSubtotal += lineSubtotal
            cartTax += lineTax
            cartTotal += lineTotal
            remainingBalance = cartTotal - amountPaid

            val cartRow = LinearLayout(activity)
            cartRow.orientation = LinearLayout.HORIZONTAL
            cartRow.setPadding(10, 10, 10, 10)

            val productCell = TextView(activity)
            productCell.text = selectedProductName
            productCell.textSize = 14f

            // Changes cart row product text color based on Settings - AF
            productCell.setTextColor(ThemeManager.primaryText(activity))

            val qtyCell = TextView(activity)
            qtyCell.text = quantity.toString()
            qtyCell.textSize = 14f

            // Changes cart row quantity text color based on Settings - AF
            qtyCell.setTextColor(ThemeManager.primaryText(activity))

            val priceCell = TextView(activity)
            priceCell.text = "$${"%.2f".format(selectedProductPrice)}"
            priceCell.textSize = 14f

            // Changes cart row price text color based on Settings - AF
            priceCell.setTextColor(ThemeManager.primaryText(activity))

            val discountCell = TextView(activity)
            discountCell.text = "0%"
            discountCell.textSize = 14f

            // Changes cart row discount text color based on Settings - AF
            discountCell.setTextColor(ThemeManager.primaryText(activity))

            val taxCell = TextView(activity)
            taxCell.text = "${"%.2f".format(selectedProductTax)}%"
            taxCell.textSize = 14f

            // Changes cart row tax text color based on Settings - AF
            taxCell.setTextColor(ThemeManager.primaryText(activity))

            val totalCell = TextView(activity)
            totalCell.text = "$${"%.2f".format(lineTotal)}"
            totalCell.textSize = 14f

            // Changes cart row total text color based on Settings - AF
            totalCell.setTextColor(ThemeManager.primaryText(activity))

            leftSide.addView(leftText)
            rightPanel.addView(rightText)
            page.addView(leftSide, leftParams)
            page.addView(rightPanel, rightParams)

            cartRow.addView(productCell, wideColumn)
            cartRow.addView(qtyCell, smallColumn)
            cartRow.addView(priceCell, smallColumn)
            cartRow.addView(discountCell, smallColumn)
            cartRow.addView(taxCell, smallColumn)
            cartRow.addView(totalCell, smallColumn)
            cartRow.addView(removeButton, smallColumn)

            cartItemsContainer.addView(cartRow)

            removeButton.setOnClickListener {
                cartSubtotal -= lineSubtotal
                cartTax -= lineTax
                cartTotal -= lineTotal
                remainingBalance = cartTotal - amountPaid

                if (remainingBalance < 0.0) {
                    remainingBalance = 0.0
                }

                if (cartSubtotal < 0.0) cartSubtotal = 0.0
                if (cartTax < 0.0) cartTax = 0.0
                if (cartTotal < 0.0) cartTotal = 0.0

                subtotalText.text = "Subtotal: $${"%.2f".format(cartSubtotal)}"
                taxText.text = "Tax: $${"%.2f".format(cartTax)}"
                totalText.text = "Total: $${"%.2f".format(cartTotal)}"
                remainingBalanceAmount.text = "$${"%.2f".format(remainingBalance)}"

                cartItemsContainer.removeView(cartRow)
                cartItems.remove(cartItem)

                if (cartItemsContainer.childCount == 0) {
                    emptyCartText.visibility = android.view.View.VISIBLE
                }

                if (cartItems.isEmpty()) {
                    amountPaid = 0.0
                    remainingBalance = 0.0
                    changeDue = 0.0
                    paymentAmountBox.setText("")
                }

            }

            subtotalText.text = "Subtotal: $${"%.2f".format(cartSubtotal)}"
            taxText.text = "Tax: $${"%.2f".format(cartTax)}"
            totalText.text = "Total: $${"%.2f".format(cartTotal)}"
            remainingBalanceAmount.text = "$${"%.2f".format(remainingBalance)}"

            searchBox.setText("")
            qtyBox.setText("")
            priceBox.setText("")
            discountBox.setText("")
            taxBox.setText("")

            selectedProductName = ""
            selectedProductSku = ""
            selectedProductPrice = 0.0
            selectedProductTax = 0.0
            selectedProductStock = 0
        }

        searchBox.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {

                if (isSelectingProduct) {
                    return
                }

                val query = s.toString().trim()

                suggestionList.removeAllViews()

                if (query.isEmpty()) {
                    return
                }

                CoroutineScope(Dispatchers.IO).launch {

                    try {

                        val exactSkuSnapshot = db.collection("products")
                            .whereEqualTo("sku", query)
                            .limit(1)
                            .get()
                            .await()

                        if (!exactSkuSnapshot.isEmpty) {

                            val product = exactSkuSnapshot.documents[0]

                            val name = product.getString("name") ?: query
                            val sku = product.getString("sku") ?: ""
                            val price = product.getDouble("price") ?: 0.0
                            val taxPercent = product.getDouble("taxPercent") ?: 0.0
                            val stock = product.getLong("stock")?.toInt() ?: 0

                            withContext(Dispatchers.Main) {

                                isSelectingProduct = true

                                searchBox.setText(name)
                                searchBox.setSelection(searchBox.text.length)
                                priceBox.setText(price.toString())
                                taxBox.setText(taxPercent.toString())

                                selectedProductName = name
                                selectedProductSku = sku
                                selectedProductPrice = price
                                selectedProductTax = taxPercent
                                selectedProductStock = stock

                                suggestionList.removeAllViews()

                                addSelectedProductToCart()

                                isSelectingProduct = false
                            }

                            return@launch
                        }

                        if (query.length < 2) {
                            return@launch
                        }

                        val snapshot = db.collection("products")
                            .get()
                            .await()

                        val matches = snapshot.documents.mapNotNull { doc ->

                            val name = doc.getString("name") ?: return@mapNotNull null
                            val sku = doc.getString("sku") ?: ""
                            val price = doc.getDouble("price") ?: 0.0
                            val taxPercent = doc.getDouble("taxPercent") ?: 0.0
                            val stock = doc.getLong("stock")?.toInt() ?: 0

                            mapOf(
                                "name" to name,
                                "sku" to sku,
                                "price" to price.toString(),
                                "taxPercent" to taxPercent.toString(),
                                "stock" to stock.toString()
                            )

                        }.filter { product ->

                            product["name"]?.contains(query, ignoreCase = true) == true ||
                                    product["sku"]?.contains(query, ignoreCase = true) == true

                        }.take(5)

                        withContext(Dispatchers.Main) {

                            suggestionList.removeAllViews()

                            matches.forEach { product ->

                                val suggestionItem = TextView(activity)
                                suggestionItem.text = "${product["name"]}    $${product["price"]}"
                                suggestionItem.textSize = 15f

                                // Changes suggestion item colors based on Settings - AF
                                suggestionItem.setTextColor(ThemeManager.primaryText(activity))

                                suggestionItem.setPadding(16, 12, 16, 12)

                                suggestionItem.setBackgroundColor(ThemeManager.sectionBackground(activity))



                                suggestionItem.setOnClickListener {

                                    isSelectingProduct = true

                                    searchBox.setText(product["name"])
                                    searchBox.setSelection(searchBox.text.length)
                                    priceBox.setText(product["price"])
                                    taxBox.setText(product["taxPercent"])

                                    selectedProductName = product["name"] ?: ""
                                    selectedProductSku = product["sku"] ?: ""
                                    selectedProductPrice = product["price"]?.toDoubleOrNull() ?: 0.0
                                    selectedProductTax =
                                        product["taxPercent"]?.toDoubleOrNull() ?: 0.0
                                    selectedProductStock = product["stock"]?.toIntOrNull() ?: 0

                                    suggestionList.removeAllViews()

                                    addSelectedProductToCart()

                                    isSelectingProduct = false
                                }

                                suggestionList.addView(suggestionItem)
                            }
                        }

                    } catch (e: Exception) {

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                activity,
                                "Product search error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }
        })

        leftSide.addView(totalsRow, totalsRowParams)

        val rightPanel = LinearLayout(activity)
        rightPanel.orientation = LinearLayout.VERTICAL

        // Changes payment panel background based on Settings - AF
        rightPanel.setBackgroundColor(ThemeManager.sectionBackground(activity))

        val rightParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )

        val leftText = TextView(activity)
        leftText.text = "Left POS Area"
        leftText.textSize = 22f
        leftText.setTextColor(Color.BLACK)
        leftText.setPadding(30, 30, 30, 30)

        val rightText = TextView(activity)
        rightText.text = "Payment Panel"
        rightText.textSize = 22f

        // Changes payment panel title color based on Settings - AF
        rightText.setTextColor(ThemeManager.primaryText(activity))

        rightText.setPadding(30, 30, 30, 30)

        rightPanel.setPadding(18, 8, 18, 8)


        val remainingBalanceLabel = TextView(activity)
        remainingBalanceLabel.text = "Remaining Balance"
        remainingBalanceLabel.textSize = 14f

        // Changes remaining balance label color based on Settings - AF
        remainingBalanceLabel.setTextColor(ThemeManager.mutedText(activity))

        remainingBalanceAmount = TextView(activity)
        remainingBalanceAmount.text = "$${"%.2f".format(remainingBalance)}"
        remainingBalanceAmount.textSize = 34f

        // Changes remaining balance amount color based on Settings - AF
        remainingBalanceAmount.setTextColor(ThemeManager.primaryText(activity))

        remainingBalanceAmount.setPadding(0, 0, 0, 12)

        val paymentAmountLabel = TextView(activity)
        paymentAmountLabel.text = "Payment Amount"
        paymentAmountLabel.textSize = 14f

        // Changes payment amount label color based on Settings - AF
        paymentAmountLabel.setTextColor(ThemeManager.mutedText(activity))

        paymentAmountBox = EditText(activity)
        paymentAmountBox.hint = "Enter amount"
        paymentAmountBox.textSize = 18f
        paymentAmountBox.setSingleLine(true)

        // Changes payment amount box colors based on Settings - AF
        paymentAmountBox.setTextColor(ThemeManager.primaryText(activity))
        paymentAmountBox.setHintTextColor(ThemeManager.secondaryText(activity))
        paymentAmountBox.setBackgroundColor(ThemeManager.inputBackground(activity))

        rightPanel.addView(remainingBalanceLabel)
        rightPanel.addView(remainingBalanceAmount)
        rightPanel.addView(paymentAmountLabel)
        rightPanel.addView(paymentAmountBox)

        val cashButtonsGrid = LinearLayout(activity)
        cashButtonsGrid.orientation = LinearLayout.VERTICAL

        val cashGridParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        cashGridParams.setMargins(0, 8, 0, 7)

        val cashRow1 = LinearLayout(activity)
        cashRow1.orientation = LinearLayout.HORIZONTAL

        val fiveButton = TextView(activity)
        fiveButton.text = "$5"
        fiveButton.textSize = 13f
        fiveButton.gravity = android.view.Gravity.CENTER

        fiveButton.setTextColor(ThemeManager.positive(activity))

        fiveButton.setPadding(0, 14, 0, 14)

        val tenButton = TextView(activity)
        tenButton.text = "$10"
        tenButton.textSize = 13f
        tenButton.gravity = android.view.Gravity.CENTER

        tenButton.setTextColor(ThemeManager.positive(activity))

        tenButton.setPadding(0, 14, 0, 14)

        val twentyButton = TextView(activity)
        twentyButton.text = "$20"
        twentyButton.textSize = 13f
        twentyButton.gravity = android.view.Gravity.CENTER

        twentyButton.setTextColor(ThemeManager.positive(activity))

        twentyButton.setPadding(0, 14, 0, 14)

        val cashRow2 = LinearLayout(activity)
        cashRow2.orientation = LinearLayout.HORIZONTAL

        val fiftyButton = TextView(activity)
        fiftyButton.text = "$50"
        fiftyButton.textSize = 13f
        fiftyButton.gravity = android.view.Gravity.CENTER

        fiftyButton.setTextColor(ThemeManager.positive(activity))

        fiftyButton.setPadding(0, 14, 0, 14)

        val hundredButton = TextView(activity)
        hundredButton.text = "$100"
        hundredButton.textSize = 13f
        hundredButton.gravity = android.view.Gravity.CENTER

        hundredButton.setTextColor(ThemeManager.positive(activity))

        hundredButton.setPadding(0, 14, 0, 14)

        val exactButton = TextView(activity)
        exactButton.text = "FULL"
        exactButton.textSize = 13f
        exactButton.gravity = android.view.Gravity.CENTER

        exactButton.setTextColor(ThemeManager.positive(activity))

        exactButton.setPadding(0, 14, 0, 14)

        val cashButtonParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        cashButtonParams.setMargins(6, 1, 6, 1)

        cashRow1.addView(fiveButton, cashButtonParams)
        cashRow1.addView(tenButton, cashButtonParams)
        cashRow1.addView(twentyButton, cashButtonParams)

        cashRow2.addView(fiftyButton, cashButtonParams)
        cashRow2.addView(hundredButton, cashButtonParams)
        cashRow2.addView(exactButton, cashButtonParams)

        // Changes quick cash button backgrounds based on Settings - AF
        fiveButton.setBackgroundColor(ThemeManager.inputBackground(activity))
        tenButton.setBackgroundColor(ThemeManager.inputBackground(activity))
        twentyButton.setBackgroundColor(ThemeManager.inputBackground(activity))
        fiftyButton.setBackgroundColor(ThemeManager.inputBackground(activity))
        hundredButton.setBackgroundColor(ThemeManager.inputBackground(activity))
        exactButton.setBackgroundColor(ThemeManager.inputBackground(activity))

        cashButtonsGrid.addView(cashRow1)
        cashButtonsGrid.addView(cashRow2)

        rightPanel.addView(cashButtonsGrid, cashGridParams)

        val keypadGrid = LinearLayout(activity)
        keypadGrid.orientation = LinearLayout.VERTICAL

        val keypadParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        keypadParams.setMargins(0, 0, 0, 5)

        val keypadButtonParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        keypadButtonParams.setMargins(4, 1, 4, 1)

        val keypadRow1 = LinearLayout(activity)
        keypadRow1.orientation = LinearLayout.HORIZONTAL

        val button1 = TextView(activity)
        button1.text = "1"
        button1.textSize = 16f
        button1.gravity = android.view.Gravity.CENTER

        button1.setTextColor(ThemeManager.primaryText(activity))
        button1.setBackgroundColor(ThemeManager.inputBackground(activity))

        button1.setPadding(0, 18, 0, 18)

        val button2 = TextView(activity)
        button2.text = "2"
        button2.textSize = 16f
        button2.gravity = android.view.Gravity.CENTER

        button2.setTextColor(ThemeManager.primaryText(activity))
        button2.setBackgroundColor(ThemeManager.inputBackground(activity))

        button2.setPadding(0, 18, 0, 18)

        val button3 = TextView(activity)
        button3.text = "3"
        button3.textSize = 16f
        button3.gravity = android.view.Gravity.CENTER

        button3.setTextColor(ThemeManager.primaryText(activity))
        button3.setBackgroundColor(ThemeManager.inputBackground(activity))

        button3.setPadding(0, 18, 0, 18)

        keypadRow1.addView(button1, keypadButtonParams)
        keypadRow1.addView(button2, keypadButtonParams)
        keypadRow1.addView(button3, keypadButtonParams)

        val keypadRow2 = LinearLayout(activity)
        keypadRow2.orientation = LinearLayout.HORIZONTAL

        val button4 = TextView(activity)
        button4.text = "4"
        button4.textSize = 16f
        button4.gravity = android.view.Gravity.CENTER

        button4.setTextColor(ThemeManager.primaryText(activity))
        button4.setBackgroundColor(ThemeManager.inputBackground(activity))

        button4.setPadding(0, 18, 0, 18)

        val button5 = TextView(activity)
        button5.text = "5"
        button5.textSize = 16f
        button5.gravity = android.view.Gravity.CENTER

        button5.setTextColor(ThemeManager.primaryText(activity))
        button5.setBackgroundColor(ThemeManager.inputBackground(activity))

        button5.setPadding(0, 18, 0, 18)

        val button6 = TextView(activity)
        button6.text = "6"
        button6.textSize = 16f
        button6.gravity = android.view.Gravity.CENTER

        button6.setTextColor(ThemeManager.primaryText(activity))
        button6.setBackgroundColor(ThemeManager.inputBackground(activity))

        button6.setPadding(0, 18, 0, 18)

        keypadRow2.addView(button4, keypadButtonParams)
        keypadRow2.addView(button5, keypadButtonParams)
        keypadRow2.addView(button6, keypadButtonParams)

        val keypadRow3 = LinearLayout(activity)
        keypadRow3.orientation = LinearLayout.HORIZONTAL

        val button7 = TextView(activity)
        button7.text = "7"
        button7.textSize = 16f
        button7.gravity = android.view.Gravity.CENTER

        button7.setTextColor(ThemeManager.primaryText(activity))
        button7.setBackgroundColor(ThemeManager.inputBackground(activity))

        button7.setPadding(0, 18, 0, 18)

        val button8 = TextView(activity)
        button8.text = "8"
        button8.textSize = 16f
        button8.gravity = android.view.Gravity.CENTER

        button8.setTextColor(ThemeManager.primaryText(activity))
        button8.setBackgroundColor(ThemeManager.inputBackground(activity))

        button8.setPadding(0, 18, 0, 18)

        val button9 = TextView(activity)
        button9.text = "9"
        button9.textSize = 16f
        button9.gravity = android.view.Gravity.CENTER

        button9.setTextColor(ThemeManager.primaryText(activity))

        button9.setBackgroundColor(ThemeManager.inputBackground(activity))
        button9.setPadding(0, 18, 0, 18)

        keypadRow3.addView(button7, keypadButtonParams)
        keypadRow3.addView(button8, keypadButtonParams)
        keypadRow3.addView(button9, keypadButtonParams)

        val keypadRow4 = LinearLayout(activity)
        keypadRow4.orientation = LinearLayout.HORIZONTAL

        val decimalButton = TextView(activity)
        decimalButton.text = "."
        decimalButton.textSize = 16f
        decimalButton.gravity = android.view.Gravity.CENTER

        decimalButton.setTextColor(ThemeManager.primaryText(activity))
        decimalButton.setBackgroundColor(ThemeManager.inputBackground(activity))

        decimalButton.setPadding(0, 18, 0, 18)

        val button0 = TextView(activity)
        button0.text = "0"
        button0.textSize = 16f
        button0.gravity = android.view.Gravity.CENTER

        button0.setTextColor(ThemeManager.primaryText(activity))
        button0.setBackgroundColor(ThemeManager.inputBackground(activity))

        button0.setPadding(0, 18, 0, 18)

        val backspaceButton = TextView(activity)
        backspaceButton.text = "⌫"
        backspaceButton.textSize = 16f
        backspaceButton.gravity = android.view.Gravity.CENTER

        backspaceButton.setTextColor(ThemeManager.negative(activity))
        backspaceButton.setBackgroundColor(ThemeManager.inputBackground(activity))

        backspaceButton.setPadding(0, 18, 0, 18)

        keypadRow4.addView(decimalButton, keypadButtonParams)
        keypadRow4.addView(button0, keypadButtonParams)
        keypadRow4.addView(backspaceButton, keypadButtonParams)

        keypadGrid.addView(keypadRow1)
        keypadGrid.addView(keypadRow2)
        keypadGrid.addView(keypadRow3)
        keypadGrid.addView(keypadRow4)

        rightPanel.addView(keypadGrid, keypadParams)

        val clearAmountButton = TextView(activity)
        clearAmountButton.text = "Clear Amount"
        clearAmountButton.textSize = 13f
        clearAmountButton.gravity = android.view.Gravity.CENTER
        clearAmountButton.setTextColor(Color.WHITE)

        // Changes clear amount button color based on Settings - AF
        clearAmountButton.setBackgroundColor(ThemeManager.warning(activity))

        clearAmountButton.setPadding(0, 16, 0, 16)

        val applyPaymentButton = TextView(activity)
        applyPaymentButton.text = "Apply Payment"
        applyPaymentButton.textSize = 13f
        applyPaymentButton.gravity = android.view.Gravity.CENTER
        applyPaymentButton.setTextColor(Color.WHITE)

        // Changes apply payment button color based on Settings - AF
        applyPaymentButton.setBackgroundColor(ThemeManager.positive(activity))

        applyPaymentButton.setPadding(0, 16, 0, 16)

        val completeSaleButton = TextView(activity)
        completeSaleButton.text = "Complete Sale"
        completeSaleButton.textSize = 13f
        completeSaleButton.gravity = android.view.Gravity.CENTER
        completeSaleButton.setTextColor(Color.WHITE)

        // Changes complete sale button color based on Settings - AF
        completeSaleButton.setBackgroundColor(ThemeManager.negative(activity))

        completeSaleButton.setPadding(0, 16, 0, 16)

        val cancelTransactionButton = TextView(activity)
        cancelTransactionButton.text = "Cancel"
        cancelTransactionButton.textSize = 13f
        cancelTransactionButton.gravity = android.view.Gravity.CENTER

        // Changes cancel button colors based on Settings - AF
        cancelTransactionButton.setTextColor(ThemeManager.secondaryText(activity))
        cancelTransactionButton.setBackgroundColor(ThemeManager.inputBackground(activity))

        cancelTransactionButton.setPadding(0, 16, 0, 16)

        val actionCellParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        actionCellParams.setMargins(3, 3, 3, 3)

        val actionRow1 = LinearLayout(activity)
        actionRow1.orientation = LinearLayout.HORIZONTAL
        actionRow1.addView(clearAmountButton, actionCellParams)
        actionRow1.addView(applyPaymentButton, actionCellParams)

        val actionRow2 = LinearLayout(activity)
        actionRow2.orientation = LinearLayout.HORIZONTAL
        actionRow2.addView(completeSaleButton, actionCellParams)
        actionRow2.addView(cancelTransactionButton, actionCellParams)

        val actionGridParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        actionGridParams.setMargins(0, 4, 0, 4)

        rightPanel.addView(actionRow1, actionGridParams)
        rightPanel.addView(actionRow2, actionGridParams)

        clearAmountButton.setOnClickListener {
            paymentAmountBox.setText("")
        }

        fiveButton.setOnClickListener {
            paymentAmountBox.setText("5.00")
        }

        tenButton.setOnClickListener {
            paymentAmountBox.setText("10.00")
        }

        twentyButton.setOnClickListener {
            paymentAmountBox.setText("20.00")
        }

        fiftyButton.setOnClickListener {
            paymentAmountBox.setText("50.00")
        }

        hundredButton.setOnClickListener {
            paymentAmountBox.setText("100.00")
        }

        exactButton.setOnClickListener {
            paymentAmountBox.setText("%.2f".format(remainingBalance))
        }

        fun appendPaymentText(value: String) {
            paymentAmountBox.append(value)
        }

        button1.setOnClickListener { appendPaymentText("1") }
        button2.setOnClickListener { appendPaymentText("2") }
        button3.setOnClickListener { appendPaymentText("3") }

        button4.setOnClickListener { appendPaymentText("4") }
        button5.setOnClickListener { appendPaymentText("5") }
        button6.setOnClickListener { appendPaymentText("6") }

        button7.setOnClickListener { appendPaymentText("7") }
        button8.setOnClickListener { appendPaymentText("8") }
        button9.setOnClickListener { appendPaymentText("9") }

        button0.setOnClickListener { appendPaymentText("0") }

        decimalButton.setOnClickListener {

            val currentText = paymentAmountBox.text.toString()

            if (!currentText.contains(".")) {
                appendPaymentText(".")
            }
        }

        backspaceButton.setOnClickListener {

            val currentText = paymentAmountBox.text.toString()

            if (currentText.isNotEmpty()) {
                paymentAmountBox.setText(
                    currentText.substring(0, currentText.length - 1)
                )
                paymentAmountBox.setSelection(
                    paymentAmountBox.text.length
                )
            }
        }

        fun completePaidTransaction() {

            Toast.makeText(activity, "Completing sale...", Toast.LENGTH_SHORT).show()

            if (cartItems.isEmpty()) {
                Toast.makeText(activity, "Cart is empty", Toast.LENGTH_SHORT).show()
                return
            }

            if (amountPaid < cartTotal) {
                Toast.makeText(activity, "Payment is not complete", Toast.LENGTH_SHORT).show()
                return
            }

            CoroutineScope(Dispatchers.IO).launch {

                try {

                    val saleData = hashMapOf(
                        "subtotal" to cartSubtotal,
                        "tax" to cartTax,
                        "total" to cartTotal,
                        "paidAmount" to amountPaid,
                        "changeDue" to maxOf(0.0, amountPaid - cartTotal),
                        "timestamp" to Date(),
                        "items" to cartItems.map { HashMap(it) }
                    )

                    db.collection("transactions").add(saleData).await()

                    cartItems.forEach { item ->

                        try {

                            val sku = item["sku"].toString()
                            val quantitySold = item["quantity"].toString().toIntOrNull() ?: 0

                            if (sku.isNotEmpty() && quantitySold > 0) {

                                val productSnapshot = db.collection("products")
                                    .whereEqualTo("sku", sku)
                                    .limit(1)
                                    .get()
                                    .await()

                                if (!productSnapshot.isEmpty) {

                                    val productDoc = productSnapshot.documents[0]
                                    val currentStock = productDoc.getLong("stock") ?: 0L
                                    val newStock = maxOf(0L, currentStock - quantitySold)

                                    db.collection("products")
                                        .document(productDoc.id)
                                        .update("stock", newStock)
                                        .await()
                                }
                            }

                        } catch (stockError: Exception) {
                        }
                    }

                    withContext(Dispatchers.Main) {

                        cartItems.clear()
                        cartItemsContainer.removeAllViews()
                        emptyCartText.visibility = android.view.View.VISIBLE

                        cartSubtotal = 0.0
                        cartTax = 0.0
                        cartTotal = 0.0
                        amountPaid = 0.0
                        remainingBalance = 0.0
                        changeDue = 0.0

                        subtotalText.text = "Subtotal: $0.00"
                        taxText.text = "Tax: $0.00"
                        totalText.text = "Total: $0.00"
                        remainingBalanceAmount.text = "$0.00"

                        searchBox.setText("")
                        qtyBox.setText("")
                        priceBox.setText("")
                        discountBox.setText("")
                        taxBox.setText("")
                        paymentAmountBox.setText("")

                        Toast.makeText(activity, "Sale completed", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            activity,
                            "Sale error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        /* Temporary Comment in case of implementation error.

        applyPaymentButton.setOnClickListener {

            val paymentAmount = paymentAmountBox.text.toString().toDoubleOrNull()

            if (paymentAmount == null || paymentAmount <= 0.0) {
                Toast.makeText(activity, "Enter a valid payment amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (cartTotal <= 0.0) {
                Toast.makeText(activity, "Cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            amountPaid += paymentAmount
            remainingBalance = cartTotal - amountPaid

            if (remainingBalance < 0.0) {
                changeDue = amountPaid - cartTotal
                remainingBalance = 0.0

                AlertDialog.Builder(activity)
                    .setTitle("Change Due")
                    .setMessage("$${"%.2f".format(changeDue)}")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            } else {
                changeDue = 0.0
            }

            remainingBalanceAmount.text = "$${"%.2f".format(remainingBalance)}"
            paymentAmountBox.setText("")

            if (remainingBalance <= 0.1) {
                completePaidTransaction()
            }
        }

        completeSaleButton.setOnClickListener {
            completePaidTransaction()
        }


        addButton.setOnClickListener {
            addSelectedProductToCart()
        }

        */

        applyPaymentButton.setOnClickListener {

            if (cartTotal <= 0.0) {
                Toast.makeText(activity, "Cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val paymentText = paymentAmountBox.text.toString().trim()

            // If no cash amount is entered, offer card payment
            if (paymentText.isEmpty()) {

                AlertDialog.Builder(activity)
                    .setTitle("Card Payment")
                    .setMessage("No cash amount was entered. Process this payment as a card payment?")
                    .setPositiveButton("Yes") { dialog, _ ->

                        dialog.dismiss()

                        // Stripe payment processing will go here later

                        Toast.makeText(
                            activity,
                            "Card payment approved",
                            Toast.LENGTH_SHORT
                        ).show()

                        amountPaid = cartTotal
                        remainingBalance = 0.0
                        changeDue = 0.0

                        remainingBalanceAmount.text = "$0.00"
                        paymentAmountBox.setText("")

                        completePaidTransaction()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()

                return@setOnClickListener
            }

            val paymentAmount = paymentText.toDoubleOrNull()

            if (paymentAmount == null || paymentAmount <= 0.0) {
                Toast.makeText(activity, "Enter a valid payment amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            amountPaid += paymentAmount
            remainingBalance = cartTotal - amountPaid

            // Customer paid more than the total, calculate change
            if (remainingBalance < 0.0) {

                changeDue = amountPaid - cartTotal
                remainingBalance = 0.0

                AlertDialog.Builder(activity)
                    .setTitle("Change Due")
                    .setMessage("$${"%.2f".format(changeDue)}")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()

            } else {

                changeDue = 0.0
            }

            remainingBalanceAmount.text = "$${"%.2f".format(remainingBalance)}"
            paymentAmountBox.setText("")

            if (remainingBalance <= 0.1) {
                completePaidTransaction()
            }
        }

        newTransactionButton.setOnClickListener {

            AlertDialog.Builder(activity)
                .setTitle("Cancel Current Transaction?")
                .setMessage("This will clear the current cart and payment information.")
                .setPositiveButton("Yes") { dialog, _ ->

                    cartItems.clear()
                    cartItemsContainer.removeAllViews()
                    emptyCartText.visibility = android.view.View.VISIBLE

                    cartSubtotal = 0.0
                    cartTax = 0.0
                    cartTotal = 0.0
                    amountPaid = 0.0
                    remainingBalance = 0.0
                    changeDue = 0.0

                    subtotalText.text = "Subtotal: $0.00"
                    taxText.text = "Tax: $0.00"
                    totalText.text = "Total: $0.00"
                    remainingBalanceAmount.text = "$0.00"

                    searchBox.setText("")
                    qtyBox.setText("")
                    priceBox.setText("")
                    discountBox.setText("")
                    taxBox.setText("")
                    paymentAmountBox.setText("")

                    selectedProductName = ""
                    selectedProductSku = ""
                    selectedProductPrice = 0.0
                    selectedProductTax = 0.0
                    selectedProductStock = 0

                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        val rightScrollView = ScrollView(activity)
        rightScrollView.isVerticalScrollBarEnabled = true
        rightScrollView.isScrollbarFadingEnabled = false
        rightScrollView.setBackgroundColor(Color.rgb(235, 239, 245))

        rightScrollView.addView(rightPanel)

        page.addView(leftSide, leftParams)
        page.addView(rightScrollView, rightParams)

        return page
    }

}