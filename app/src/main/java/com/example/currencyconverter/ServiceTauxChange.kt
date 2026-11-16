package com.example.currencyconverter

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

/**
 * Bonus : récupération des taux réels.
 *
 * API gratuite et sans clé « ExchangeRate-API (open access) » :
 *   GET https://open.er-api.com/v6/latest/EUR
 * Réponse (extrait) : { "result": "success", "base_code": "EUR", "rates": { "USD": 1.16, ... } }
 * Elle couvre les 6 devises du TP, y compris le dinar tunisien (TND).
 */
interface ServiceTauxChange {

    /** Taux de toutes les devises pour 1 EUR. Fonction suspendue : appelée depuis une coroutine. */
    @GET("v6/latest/EUR")
    suspend fun tauxPourUnEuro(): ReponseTaux
}

/** Modèle de la réponse JSON (seuls les champs utiles sont déclarés). */
data class ReponseTaux(
    @SerializedName("result") val resultat: String?,
    @SerializedName("rates") val taux: Map<String, Double>?
)

/** Point d'accès unique au service Retrofit (créé à la première utilisation). */
object ClientApi {
    private const val URL_DE_BASE = "https://open.er-api.com/"

    val service: ServiceTauxChange by lazy {
        Retrofit.Builder()
            .baseUrl(URL_DE_BASE)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ServiceTauxChange::class.java)
    }
}
