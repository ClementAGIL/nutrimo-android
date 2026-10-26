// French number and date formatting, and lenient decimal parsing ("72,5" or "72.5").
package com.clement.nutrimo.ui.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FR = Locale.FRANCE
private val DAY = DateTimeFormatter.ofPattern("EEE d MMM", FR)
private val FULL = DateTimeFormatter.ofPattern("d MMM yyyy", FR)
private val SHORT = DateTimeFormatter.ofPattern("dd/MM", FR)

fun fmt(value: Double, decimals: Int = 1): String {
    val text = "%.${decimals}f".format(FR, value)
    return if (decimals > 0) text.trimEnd('0').trimEnd(',') else text
}

fun kcal(value: Double) = "${fmt(value, 0)} kcal"
fun kg(value: Double) = "${fmt(value, 1)} kg"
fun signedKg(delta: Double) = "${if (delta > 0) "+" else ""}${fmt(delta)} kg"
fun grams(value: Double?) = value?.let { "${fmt(it, 1)} g" } ?: "–"
fun percent(ratio: Double) = "${fmt(ratio * 100, 0)} %"

fun LocalDate.day(): String = format(DAY).replaceFirstChar { it.uppercase() }
fun LocalDate.full(): String = format(FULL)
fun LocalDate.short(): String = format(SHORT)

fun String.toDecimal(): Double? = trim().replace(',', '.').toDoubleOrNull()

/** Text for an editable field: "72.5" → "72,5", whole numbers without decimals. */
fun Double.toInput(): String = fmt(this, 2)
