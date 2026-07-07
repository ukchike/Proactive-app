package com.unifiedproductivity.app.util

import java.text.NumberFormat
import java.util.Locale

/** Formats whole-currency-unit amounts as Naira (₦12,345), matching everyday budgeting. */
object CurrencyFormatter {

    private val numberFormat = NumberFormat.getIntegerInstance(Locale.US)

    fun format(amount: Long): String = "₦${numberFormat.format(amount)}"
}
