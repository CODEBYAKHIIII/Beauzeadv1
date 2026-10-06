package com.example.data

import java.text.NumberFormat
import java.time.LocalTime
import java.util.Currency
import java.util.Locale

object InvoiceDisplayFormat {
    val supportedCountries = listOf(
        "India", "USA", "UK", "UAE", "Australia", "Canada",
        "Germany", "Singapore", "Nigeria", "Kenya",
        "South Africa", "Brazil"
    )

    fun greeting(hour: Int = LocalTime.now().hour): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Hello"
    }

    fun currencyCode(country: String): String = Currency.getInstance(localeForCountry(country)).currencyCode

    fun currencyName(country: String): String {
        val locale = localeForCountry(country)
        val currency = Currency.getInstance(locale)
        return "${currency.currencyCode} (${currency.getSymbol(locale)})"
    }

    fun formatCurrency(amount: Double, country: String): String =
        NumberFormat.getCurrencyInstance(localeForCountry(country)).format(amount)

    private fun localeForCountry(country: String): Locale {
        val normalized = country.trim()
        val code = when (normalized.lowercase(Locale.ROOT)) {
            "uk", "united kingdom" -> "GB"
            "usa", "united states", "united states of america" -> "US"
            "uae", "united arab emirates" -> "AE"
            else -> Locale.getISOCountries().firstOrNull { candidate ->
                Locale("", candidate).getDisplayCountry(Locale.ENGLISH)
                    .equals(normalized, ignoreCase = true)
            } ?: error("Select a supported country to determine its currency.")
        }
        return Locale.Builder().setLanguage("en").setRegion(code).build()
    }
}
