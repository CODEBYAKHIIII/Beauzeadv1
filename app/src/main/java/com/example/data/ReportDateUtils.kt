package com.example.data

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ReportDateUtils {
    private val acceptedFormats = listOf("dd MMM yyyy", "d MMM yyyy", "yyyy-MM-dd", "dd/MM/yyyy")

    fun parse(value: String): Date? {
        for (pattern in acceptedFormats) {
            val parser = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
            val position = ParsePosition(0)
            val date = parser.parse(value.trim(), position)
            if (date != null && position.index == value.trim().length) return date
        }
        return null
    }

    fun monthKey(value: String): String? = parse(value)?.let(::monthKey)

    fun monthKey(date: Date): String {
        val calendar = Calendar.getInstance(Locale.US).apply { time = date }
        return "%04d-%02d".format(
            Locale.US,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
    }

    fun monthKey(year: Int, zeroBasedMonth: Int): String =
        "%04d-%02d".format(Locale.US, year, zeroBasedMonth + 1)

    fun monthKeys(from: Date, through: Date): List<String> {
        val month = Calendar.getInstance(Locale.US).apply {
            time = from
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val lastMonth = Calendar.getInstance(Locale.US).apply {
            time = through
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (month.after(lastMonth)) return emptyList()
        return buildList {
            while (!month.after(lastMonth)) {
                add(monthKey(month.time))
                month.add(Calendar.MONTH, 1)
            }
        }
    }

    fun displayMonth(year: Int, zeroBasedMonth: Int): String {
        val calendar = Calendar.getInstance(Locale.US).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, zeroBasedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMM yyyy", Locale.US).format(calendar.time)
    }

    fun displayMonth(monthKey: String): String? {
        val parser = SimpleDateFormat("yyyy-MM", Locale.US).apply { isLenient = false }
        val position = ParsePosition(0)
        val date = parser.parse(monthKey, position)
        return if (date != null && position.index == monthKey.length) {
            SimpleDateFormat("MMM yyyy", Locale.US).format(date)
        } else {
            null
        }
    }

    fun isBeforeToday(value: String, today: Date = currentDate()): Boolean {
        val date = parse(value) ?: return false
        val dueDate = Calendar.getInstance(Locale.US).apply { time = date }
        val currentDate = Calendar.getInstance(Locale.US).apply { time = today }
        return when {
            dueDate.get(Calendar.YEAR) != currentDate.get(Calendar.YEAR) ->
                dueDate.get(Calendar.YEAR) < currentDate.get(Calendar.YEAR)
            else -> dueDate.get(Calendar.DAY_OF_YEAR) < currentDate.get(Calendar.DAY_OF_YEAR)
        }
    }

    fun displayDate(date: Date): String = SimpleDateFormat("dd MMM yyyy", Locale.US).format(date)

    fun currentDate(): Date = Calendar.getInstance().time

    fun dateAfterDays(date: Date, days: Int): Date = Calendar.getInstance(Locale.US).apply {
        time = date
        add(Calendar.DAY_OF_YEAR, days)
    }.time
}
