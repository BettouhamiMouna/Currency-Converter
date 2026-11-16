package com.example.currencyconverter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

/**
 * Bonus : ViewModel qui conserve l'état de l'écran lors d'un changement de configuration
 * (rotation, changement de langue, mode sombre…). L'Activity est recréée, pas le ViewModel.
 */
class ConvertisseurViewModel : ViewModel() {

    /** Montant tapé au clavier, sous forme de texte (ex. "120", "0.5"). */
    var montant: String = MONTANT_INITIAL

    /** Dernier résultat affiché (texte déjà formaté), vide si aucun. */
    var resultat: String = ""

    /** Positions sélectionnées dans les deux Spinner (-1 = pas encore initialisé). */
    var positionSource: Int = -1
    var positionCible: Int = -1

    /** Vrai si l'utilisateur a activé la bascule « Live » (taux réels). */
    var tauxReelsDemandes: Boolean = false

    /** Taux réels téléchargés, ou null quand on utilise les taux fixes de l'exercice. */
    private val _tauxReels = MutableLiveData<Map<String, Double>?>(null)
    val tauxReels: LiveData<Map<String, Double>?> = _tauxReels

    /** Événement ponctuel à afficher une seule fois (Toast), remis à null une fois traité. */
    private val _evenement = MutableLiveData<EvenementTaux?>(null)
    val evenement: LiveData<EvenementTaux?> = _evenement

    /**
     * Télécharge les taux réels dans une coroutine (viewModelScope : annulée
     * automatiquement si le ViewModel est détruit). En cas d'échec (pas de réseau,
     * délai dépassé, réponse incomplète), on revient aux taux fixes.
     *
     * @param devisesAttendues devises qui doivent toutes être présentes dans la réponse
     */
    fun chargerTauxReels(devisesAttendues: List<String>) {
        tauxReelsDemandes = true
        viewModelScope.launch {
            val reponse = try {
                withTimeoutOrNull(DELAI_RESEAU_MS) { ClientApi.service.tauxPourUnEuro() }
            } catch (annulation: CancellationException) {
                throw annulation // on ne bloque pas l'annulation de la coroutine
            } catch (erreur: Exception) {
                null // IOException (pas de réseau), HttpException, JSON invalide…
            }

            val tauxUtiles = reponse
                ?.takeIf { it.resultat == "success" }
                ?.taux
                ?.filterKeys { it in devisesAttendues }

            if (tauxUtiles != null && tauxUtiles.keys.containsAll(devisesAttendues)) {
                _tauxReels.value = tauxUtiles
                _evenement.value = EvenementTaux.TAUX_REELS_CHARGES
            } else {
                utiliserTauxFixes()
                _evenement.value = EvenementTaux.RESEAU_INDISPONIBLE
            }
        }
    }

    /** Repli sur les taux fixes de l'énoncé. */
    fun utiliserTauxFixes() {
        tauxReelsDemandes = false
        _tauxReels.value = null
    }

    /** À appeler par l'Activity après avoir affiché l'événement. */
    fun evenementTraite() {
        _evenement.value = null
    }

    companion object {
        const val MONTANT_INITIAL = "0"
        private const val DELAI_RESEAU_MS = 5_000L
    }
}

/** Résultats possibles du chargement des taux réels. */
enum class EvenementTaux { TAUX_REELS_CHARGES, RESEAU_INDISPONIBLE }
