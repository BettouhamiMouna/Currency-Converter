package com.example.currencyconverter

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.currencyconverter.databinding.ActivityMainBinding
import java.util.Locale

/**
 * Écran unique de l'application « Currency Converter ».
 *
 * - Le montant est saisi uniquement via le clavier numérique de l'application.
 * - La conversion passe par l'euro (devise pivot) avec des taux fixes,
 *   ou des taux réels téléchargés (bonus, bascule « Live »).
 * - L'état de l'écran est conservé dans un ViewModel (bonus).
 */
class MainActivity : AppCompatActivity() {

    /** Accès typé aux vues de activity_main.xml (View Binding). */
    private lateinit var binding: ActivityMainBinding

    /** État de l'écran, conservé lors d'une recréation de l'Activity. */
    private val etat: ConvertisseurViewModel by viewModels()

    /** Taux fixes fournis par l'énoncé, exprimés pour 1 EUR. */
    private val taux = mapOf(
        "EUR" to 1.0, "USD" to 1.17, "TND" to 3.40,
        "GBP" to 0.87, "JPY" to 172.50, "CAD" to 1.62
    )

    /** Liste des devises lue depuis res/values/strings.xml (string-array "devises"). */
    private lateinit var devises: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        devises = resources.getStringArray(R.array.devises).toList()

        initialiserSpinners()
        initialiserClavier()
        initialiserBoutons()
        initialiserTauxReels()
        restaurerAffichage()
    }

    // ------------------------------------------------------------------
    // Initialisation de l'interface
    // ------------------------------------------------------------------

    /** Étape 4 : remplit les deux Spinner avec un ArrayAdapter construit sur R.array.devises. */
    private fun initialiserSpinners() {
        val adaptateurSource = ArrayAdapter.createFromResource(
            this, R.array.devises, R.layout.item_devise
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val adaptateurCible = ArrayAdapter.createFromResource(
            this, R.array.devises, R.layout.item_devise
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.spDeviseSource.adapter = adaptateurSource
        binding.spDeviseCible.adapter = adaptateurCible

        // Sélection par défaut : USD -> EUR (sauf si un état a été conservé par le ViewModel)
        if (etat.positionSource < 0) etat.positionSource = devises.indexOf(DEVISE_SOURCE_PAR_DEFAUT)
        if (etat.positionCible < 0) etat.positionCible = devises.indexOf(DEVISE_CIBLE_PAR_DEFAUT)
        binding.spDeviseSource.setSelection(etat.positionSource)
        binding.spDeviseCible.setSelection(etat.positionCible)

        // Bonus : conversion automatique dès qu'une devise change
        val ecouteurDevise = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, vue: View?, position: Int, id: Long) {
                etat.positionSource = binding.spDeviseSource.selectedItemPosition
                etat.positionCible = binding.spDeviseCible.selectedItemPosition
                convertirAutomatiquement()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.spDeviseSource.onItemSelectedListener = ecouteurDevise
        binding.spDeviseCible.onItemSelectedListener = ecouteurDevise
    }

    /** Étape 5 : branche les touches du clavier numérique. */
    private fun initialiserClavier() {
        val touchesCaracteres: List<Button> = with(binding) {
            listOf(btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9, btnPoint)
        }
        // Chaque touche ajoute le texte qu'elle affiche (défini dans strings.xml)
        touchesCaracteres.forEach { touche ->
            touche.setOnClickListener { ajouterCaractere(touche.text.toString()) }
        }
        binding.btnEffacer.setOnClickListener { effacer() }
    }

    /** Étapes 6 et 7 : boutons « Convertir » et « Inverser ». */
    private fun initialiserBoutons() {
        binding.btnConvertir.setOnClickListener { lancerConversion(afficherErreur = true) }
        binding.btnInverser.setOnClickListener { inverserDevises() }
    }

    /** Bonus : bascule entre taux fixes et taux réels téléchargés. */
    private fun initialiserTauxReels() {
        // Restaure l'état de la bascule AVANT de poser l'écouteur (pas de rechargement inutile)
        binding.swTauxReels.isChecked = etat.tauxReelsDemandes
        binding.swTauxReels.setOnCheckedChangeListener { _, active ->
            if (active) {
                etat.chargerTauxReels(devises)
            } else {
                etat.utiliserTauxFixes()
            }
        }

        // Dès que les taux changent (chargés ou abandonnés), on recalcule le résultat
        etat.tauxReels.observe(this) { convertirAutomatiquement() }

        // Message ponctuel après une tentative de chargement
        etat.evenement.observe(this) { evenement ->
            when (evenement) {
                EvenementTaux.TAUX_REELS_CHARGES ->
                    afficherMessage(getString(R.string.taux_reels_charges))
                EvenementTaux.RESEAU_INDISPONIBLE -> {
                    afficherMessage(getString(R.string.taux_reels_indisponibles))
                    binding.swTauxReels.isChecked = false // repli sur les taux fixes
                }
                null -> return@observe
            }
            etat.evenementTraite()
        }
    }

    /** Réaffiche le montant et le résultat conservés par le ViewModel. */
    private fun restaurerAffichage() {
        binding.tvMontant.text = etat.montant
        binding.tvResultat.text = etat.resultat
    }

    // ------------------------------------------------------------------
    // Clavier numérique
    // ------------------------------------------------------------------

    /**
     * Ajoute un caractère (chiffre 0-9 ou « . ») à la fin du montant.
     * Règles : un seul point décimal, 10 caractères maximum,
     * le premier chiffre tapé remplace le « 0 » initial.
     */
    private fun ajouterCaractere(c: String) {
        val montantActuel = etat.montant

        // Deuxième point décimal : ignoré
        if (c == POINT_DECIMAL && montantActuel.contains(POINT_DECIMAL)) return
        // Longueur maximale atteinte : ignoré
        if (montantActuel.length >= LONGUEUR_MAX_MONTANT) return

        val nouveauMontant =
            if (montantActuel == ConvertisseurViewModel.MONTANT_INITIAL && c != POINT_DECIMAL) {
                c // le premier chiffre remplace le « 0 » initial
            } else {
                montantActuel + c // « 0 » + « . » donne « 0. »
            }

        etat.montant = nouveauMontant
        binding.tvMontant.text = nouveauMontant
        convertirAutomatiquement()
    }

    /** Touche « C » : remet le montant à « 0 » et vide le résultat. */
    private fun effacer() {
        etat.montant = ConvertisseurViewModel.MONTANT_INITIAL
        etat.resultat = ""
        binding.tvMontant.text = etat.montant
        binding.tvResultat.text = ""
    }

    // ------------------------------------------------------------------
    // Conversion
    // ------------------------------------------------------------------

    /**
     * Étape 6 : convertit [montant] de la devise [source] vers la devise [cible]
     * en passant par l'euro (devise pivot).
     */
    fun convertir(montant: Double, source: String, cible: String): Double {
        // Taux réels si disponibles (bonus), sinon taux fixes de l'énoncé
        val tauxUtilises = etat.tauxReels.value ?: taux
        return convertirViaEuro(montant, source, cible, tauxUtilises)
    }

    /**
     * Lit le montant et les devises, lance la conversion et affiche le résultat
     * arrondi à 2 décimales.
     *
     * @param afficherErreur vrai pour un clic sur « Convertir » (Toast si montant nul),
     *                       faux pour la conversion automatique (pas de Toast à chaque touche).
     */
    private fun lancerConversion(afficherErreur: Boolean) {
        val montant = etat.montant.toDoubleOrNull() ?: 0.0

        if (montant == 0.0) {
            if (afficherErreur) afficherMessage(getString(R.string.erreur_montant))
            etat.resultat = ""
            binding.tvResultat.text = ""
            return
        }

        val deviseSource = binding.spDeviseSource.selectedItem.toString()
        val deviseCible = binding.spDeviseCible.selectedItem.toString()
        val resultat = convertir(montant, deviseSource, deviseCible)

        // Locale.US : point décimal « 102.56 » quel que soit la langue du téléphone
        etat.resultat = String.format(Locale.US, "%.2f", resultat)
        binding.tvResultat.text = etat.resultat
    }

    /** Bonus : conversion sans passer par le bouton (après une touche ou un changement de devise). */
    private fun convertirAutomatiquement() {
        lancerConversion(afficherErreur = false)
    }

    /** Étape 7 : échange les deux devises puis relance la conversion si un résultat est affiché. */
    private fun inverserDevises() {
        val positionSource = binding.spDeviseSource.selectedItemPosition
        val positionCible = binding.spDeviseCible.selectedItemPosition

        binding.spDeviseSource.setSelection(positionCible)
        binding.spDeviseCible.setSelection(positionSource)
        etat.positionSource = positionCible
        etat.positionCible = positionSource

        if (binding.tvResultat.text.isNotEmpty()) {
            lancerConversion(afficherErreur = false)
        }
    }

    private fun afficherMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val DEVISE_SOURCE_PAR_DEFAUT = "USD"
        private const val DEVISE_CIBLE_PAR_DEFAUT = "EUR"
        private const val POINT_DECIMAL = "."
        private const val LONGUEUR_MAX_MONTANT = 10
    }
}
