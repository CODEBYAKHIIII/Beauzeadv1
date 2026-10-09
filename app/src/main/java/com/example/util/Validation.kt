package com.example.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Central input validation for the whole app.
 *
 * Every function returns `null` when the input is valid, or a human-readable
 * error message that can be fed straight into `WavesTextField(errorMessage = ...)`.
 *
 * Usage pattern (validate-on-save, show-inline):
 * ```
 * var hasSubmitted by remember { mutableStateOf(false) }
 * val emailError = if (hasSubmitted) WavesValidation.email(email) else null
 * // on save: if (listOfNotNull(allErrors).isNotEmpty()) { hasSubmitted = true; return }
 * ```
 */
object WavesValidation {

    // ---------- regexes ----------
    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*" +
            "@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$"
    )
    private val PHONE_REGEX = Regex("^\\+?[0-9][0-9 \\-()]{5,18}$")
    private val URL_REGEX = Regex(
        "^(https?://)?([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}(/[^\\s]*)?$"
    )
    private val POSTAL_REGEX = Regex("^[A-Za-z0-9][A-Za-z0-9 \\-]{2,9}$")
    private val NAME_REGEX = Regex("^[\\p{L}\\p{N}][\\p{L}\\p{N} .,'&()/-]{1,59}$")
    private val ALNUM_MIN_4 = Regex("^[A-Za-z0-9][A-Za-z0-9 \\-/.]{3,}$")

    // ---------- generic ----------

    /** Non-blank with a sane length. */
    fun required(value: String, label: String = "This field"): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> "$label is required"
            trimmed.length < 2 -> "$label is too short"
            trimmed.length > 120 -> "$label is too long"
            else -> null
        }
    }

    /** Person / business / client name: letters first, sane punctuation allowed. */
    fun name(value: String, label: String = "Name"): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> "$label is required"
            !NAME_REGEX.matches(trimmed) -> "Enter a valid $label (letters, spaces and . , ' & - only)"
            else -> null
        }
    }

    fun email(value: String, required: Boolean = true): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return if (required) "Email is required" else null
        return if (EMAIL_REGEX.matches(trimmed)) null else "Enter a valid email address"
    }

    fun phone(value: String, required: Boolean = true): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return if (required) "Phone number is required" else null
        return if (PHONE_REGEX.matches(trimmed)) null else "Enter a valid phone number (7-15 digits)"
    }

    fun website(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null // optional field
        return if (URL_REGEX.matches(trimmed) && trimmed.length <= 200) {
            null
        } else {
            "Enter a valid website (e.g. waves.app)"
        }
    }

    fun postalCode(value: String, required: Boolean = true): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return if (required) "Postal code is required" else null
        return if (POSTAL_REGEX.matches(trimmed)) null else "Enter a valid postal code"
    }

    fun address(value: String, label: String = "Address", required: Boolean = true): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return if (required) "$label is required" else null
        return if (trimmed.length < 4) "$label is too short" else null
    }

    /** Password rule used across the app: at least 6 characters. */
    fun password(value: String): String? = when {
        value.isEmpty() -> "Password is required"
        value.length < 6 -> "Password must be at least 6 characters"
        value.length > 128 -> "Password is too long"
        else -> null
    }

    fun passwordConfirm(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Confirm your password"
        password != confirm -> "Passwords do not match"
        else -> null
    }

    fun otp(value: String, digits: Int = 6): String? = when {
        value.isBlank() -> "Enter the $digits-digit code"
        !Regex("^\\d{$digits}$").matches(value.trim()) -> "Code must be exactly $digits digits"
        else -> null
    }

    /** Money amount: positive decimal, max 2 decimals. */
    fun amount(value: String, label: String = "Amount", allowZero: Boolean = false): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "$label is required"
        val normalized = trimmed.replace(",", "")
        val parsed = normalized.toDoubleOrNull()
            ?: return "Enter a valid number for $label"
        return when {
            parsed < 0.0 -> "$label cannot be negative"
            parsed == 0.0 && !allowZero -> "$label must be greater than zero"
            parsed > 999_999_999.0 -> "$label is too large"
            Regex("^\\d+(\\.\\d{3,})$").matches(normalized) -> "$label can have at most 2 decimal places"
            else -> null
        }
    }

    /** Item quantity: positive number, up to 2 decimals. */
    fun quantity(value: String, label: String = "Quantity"): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "$label is required"
        val parsed = trimmed.toDoubleOrNull() ?: return "Enter a valid $label"
        return when {
            parsed <= 0.0 -> "$label must be greater than zero"
            parsed > 1_000_000.0 -> "$label is too large"
            else -> null
        }
    }

    /** Percentage 0-100, optional decimals. */
    fun percent(value: String, label: String = "Percentage"): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "$label is required"
        val parsed = trimmed.toDoubleOrNull() ?: return "Enter a valid $label"
        return when {
            parsed < 0.0 -> "$label cannot be negative"
            parsed > 100.0 -> "$label must be 100 or less"
            else -> null
        }
    }

    /** Generic tax / registration number: at least 4 alphanumeric characters. */
    fun taxId(value: String, label: String = "Tax number"): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "$label is required"
        return if (ALNUM_MIN_4.matches(trimmed)) null else "Enter a valid $label"
    }

    /**
     * Free text date shown in the app. Accepts the display format `dd MMM yyyy`
     * plus common fallbacks. Returns the error message or null.
     */
    fun dateText(
        value: String,
        label: String = "Date",
        required: Boolean = true
    ): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return if (required) "$label is required" else null
        val patterns = listOf("dd MMM yyyy", "d MMM yyyy", "dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd")
        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                format.parse(trimmed) ?: continue
                return null
            } catch (_: ParseException) {
                // try next pattern
            }
        }
        return "Enter $label as e.g. 25 Dec 2025"
    }

    /**
     * Bank account number, optional: blank is valid because bank details are
     * optional everywhere (an empty section prints as "Nill" on the PDF).
     * Accepts 6-18 digits (most domestic accounts) or 6-34 letters/digits
     * (international IBAN-style numbers).
     */
    fun bankAccountNumber(value: String): String? {
        val trimmed = value.trim().replace(" ", "")
        if (trimmed.isEmpty()) return null
        return when {
            Regex("^\\d{6,18}$").matches(trimmed) -> null
            Regex("^[A-Za-z0-9]{6,34}$").matches(trimmed) -> null
            else -> "Enter a valid account number (6-18 digits, or 6-34 letters/digits for IBAN)"
        }
    }

    /** IFSC (India): e.g. HDFC0000123. */
    fun ifsc(value: String): String? {
        val trimmed = value.trim().uppercase()
        return when {
            trimmed.isEmpty() -> "IFSC code is required"
            !Regex("^[A-Z]{4}0[A-Z0-9]{6}$").matches(trimmed) -> "Enter a valid IFSC (e.g. HDFC0000123)"
            else -> null
        }
    }

    /** SWIFT / BIC: 8 or 11 characters. */
    fun swiftBic(value: String): String? {
        val trimmed = value.trim().uppercase()
        return when {
            trimmed.isEmpty() -> "SWIFT/BIC is required"
            !Regex("^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$").matches(trimmed) ->
                "Enter a valid SWIFT/BIC (8 or 11 characters)"
            else -> null
        }
    }

    /** UPI id: name@bank. */
    fun upiId(value: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> "UPI ID is required"
            !Regex("^[A-Za-z0-9.\\-]{2,256}@[A-Za-z]{2,64}$").matches(trimmed) ->
                "Enter a valid UPI ID (e.g. name@okbank)"
            else -> null
        }
    }

    /** Helper: run several validators, return the first non-null error. */
    fun firstError(vararg results: String?): String? =
        results.firstOrNull { it != null }
}

/** One selectable tax identification type offered for a country. */
data class TaxIdType(
    val id: String,
    val label: String,
    val hint: String,
    val regex: String?
) {
    val isNoTax: Boolean get() = id == NO_TAX_ID

    /** Returns null when [value] is a valid tax number of this type. */
    fun validate(value: String): String? {
        if (isNoTax) return null
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "${label.trim()} is required"
        val pattern = regex
        return if (pattern == null) {
            WavesValidation.taxId(trimmed, label)
        } else {
            if (Regex(pattern).matches(trimmed.uppercase())) null
            else "Enter a valid $label (e.g. $hint)"
        }
    }

    companion object {
        const val NO_TAX_ID = "NO_TAX"
    }
}

data class CountryTaxTypes(val country: String, val types: List<TaxIdType>)

/**
 * Catalog of per-country tax identification types.
 * Used by Tax Settings (dropdown-driven) and referenced by Business Profile.
 */
object WavesTaxCatalog {

    private val india = listOf(
        TaxIdType("GSTIN", "GSTIN", "22AAAAA0000A1Z5",
            "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$"),
        TaxIdType("PAN", "PAN", "ABCDE1234F", "^[A-Z]{5}[0-9]{4}[A-Z]$"),
        noTax()
    )
    private val usa = listOf(
        TaxIdType("EIN", "EIN", "12-3456789", "^\\d{2}-\\d{7}$"),
        TaxIdType("SSN", "SSN", "123-45-6789", "^(\\d{3}-\\d{2}-\\d{4}|\\d{9})$"),
        noTax()
    )
    private val uk = listOf(
        TaxIdType("UK_VAT", "VAT Registration Number", "GB123456789", "^GB\\d{9}(\\d{3})?$"),
        TaxIdType("UTR", "UTR", "1234567890", "^\\d{10}$"),
        TaxIdType("NINO", "NINO", "QQ123456A", "^[A-Z]{2}\\d{6}[A-D]$"),
        noTax()
    )
    private val uae = listOf(
        TaxIdType("TRN", "TRN (VAT)", "100123456700003", "^\\d{15}$"),
        noTax()
    )
    private val australia = listOf(
        TaxIdType("ABN", "ABN", "12 345 678 901", "^\\d{11}$"),
        TaxIdType("TFN", "TFN", "123456789", "^\\d{8,9}$"),
        noTax()
    )
    private val canada = listOf(
        TaxIdType("BN", "Business Number (BN)", "123456789RT0001",
            "^\\d{9}([A-Z]{2}\\d{4})?$"),
        TaxIdType("SIN", "SIN", "123456789", "^\\d{9}$"),
        noTax()
    )
    private val germany = listOf(
        TaxIdType("UST", "USt-IdNr (VAT)", "DE123456789", "^DE\\d{9}$"),
        TaxIdType("STEU", "Steuernummer", "12345678901", "^\\d{10,11}$"),
        noTax()
    )
    private val singapore = listOf(
        TaxIdType("UEN", "UEN", "202012345A", "^[A-Z0-9]{8,10}$"),
        TaxIdType("SG_GST", "GST Registration No", "M90364669A", "^[A-Z]\\d{8}[A-Z]$"),
        noTax()
    )
    private val nigeria = listOf(
        TaxIdType("NG_TIN", "TIN", "12345678-0001", null),
        noTax()
    )
    private val kenya = listOf(
        TaxIdType("KRA_PIN", "KRA PIN", "P051234567X", "^[A-Z]\\d{9}[A-Z]$"),
        noTax()
    )
    private val southAfrica = listOf(
        TaxIdType("SA_TAX_REF", "SARS Tax Reference", "1234567890", "^\\d{10}$"),
        TaxIdType("SA_VAT", "VAT Registration Number", "4123456789", "^\\d{10}$"),
        noTax()
    )
    private val brazil = listOf(
        TaxIdType("CNPJ", "CNPJ", "12.345.678/0001-90",
            "^\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}$"),
        TaxIdType("CPF", "CPF", "123.456.789-09",
            "^\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}$"),
        noTax()
    )

    private val catalog = listOf(
        CountryTaxTypes("India", india),
        CountryTaxTypes("USA", usa),
        CountryTaxTypes("UK", uk),
        CountryTaxTypes("UAE", uae),
        CountryTaxTypes("Australia", australia),
        CountryTaxTypes("Canada", canada),
        CountryTaxTypes("Germany", germany),
        CountryTaxTypes("Singapore", singapore),
        CountryTaxTypes("Nigeria", nigeria),
        CountryTaxTypes("Kenya", kenya),
        CountryTaxTypes("South Africa", southAfrica),
        CountryTaxTypes("Brazil", brazil)
    )

    /** Ordered country list shared by Business Profile and Tax Settings. */
    val countries: List<String> = catalog.map { it.country }

    fun configFor(country: String): CountryTaxTypes? =
        catalog.find { it.country == country }
            ?: catalog.find { it.country.equals(country.trim(), ignoreCase = true) }

    /** Types offered for [country]; falls back to India for unknown/blank countries. */
    fun typesFor(country: String): List<TaxIdType> =
        configFor(country)?.types ?: india

    fun noTax(): TaxIdType = TaxIdType(
        id = TaxIdType.NO_TAX_ID,
        label = "No Tax",
        hint = "",
        regex = null
    )

    /** Resolve the saved [taxLabel] to a type id for the dropdown. */
    fun typeIdFromLabel(country: String, taxLabel: String): String {
        val types = typesFor(country)
        return types.find { it.label.equals(taxLabel.trim(), ignoreCase = true) }?.id
            ?: types.find { it.id.equals(taxLabel.trim(), ignoreCase = true) }?.id
            ?: types.first().id
    }
}
