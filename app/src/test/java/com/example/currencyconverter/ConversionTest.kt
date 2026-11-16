package com.example.currencyconverter

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * Cas de test de l'énoncé, vérifiés sur la fonction de conversion
 * (lancer avec : ./gradlew test).
 */
class ConversionTest {

    private val taux = mapOf(
        "EUR" to 1.0, "USD" to 1.17, "TND" to 3.40,
        "GBP" to 0.87, "JPY" to 172.50, "CAD" to 1.62
    )

    /** Même formatage que dans MainActivity. */
    private fun convertirEtFormater(montant: Double, source: String, cible: String) =
        String.format(Locale.US, "%.2f", convertirViaEuro(montant, source, cible, taux))

    @Test
    fun usdVersEur() = assertEquals("102.56", convertirEtFormater(120.0, "USD", "EUR"))

    @Test
    fun eurVersTnd() = assertEquals("340.00", convertirEtFormater(100.0, "EUR", "TND"))

    @Test
    fun gbpVersUsd() = assertEquals("67.24", convertirEtFormater(50.0, "GBP", "USD"))

    @Test
    fun memeDeviseRenvoieLeMontant() = assertEquals("10.00", convertirEtFormater(10.0, "EUR", "EUR"))

    @Test(expected = IllegalArgumentException::class)
    fun deviseInconnueRefusee() {
        convertirViaEuro(1.0, "XYZ", "EUR", taux)
    }
}
