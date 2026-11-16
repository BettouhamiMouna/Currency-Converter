package com.example.currencyconverter

/**
 * Calcul pur de conversion, séparé de l'Activity pour pouvoir le tester
 * avec un simple test unitaire JVM (voir ConversionTest).
 *
 * L'euro sert de devise pivot : tous les taux sont exprimés « pour 1 EUR ».
 *   montant en EUR = montant / taux(source)
 *   résultat       = montant en EUR × taux(cible)
 *
 * @param montant  montant saisi dans la devise source
 * @param source   code de la devise de départ (ex. "USD")
 * @param cible    code de la devise d'arrivée (ex. "EUR")
 * @param tauxPourUnEuro table des taux, ex. "USD" to 1.17
 * @throws IllegalArgumentException si une devise est absente de la table
 */
fun convertirViaEuro(
    montant: Double,
    source: String,
    cible: String,
    tauxPourUnEuro: Map<String, Double>
): Double {
    // Même devise : le résultat est égal au montant (aucune erreur d'arrondi possible)
    if (source == cible) return montant

    val tauxSource = requireNotNull(tauxPourUnEuro[source]) { "Taux inconnu pour $source" }
    val tauxCible = requireNotNull(tauxPourUnEuro[cible]) { "Taux inconnu pour $cible" }

    val montantEnEuros = montant / tauxSource
    return montantEnEuros * tauxCible
}
