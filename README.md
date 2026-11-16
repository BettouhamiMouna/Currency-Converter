# Currency Converter — TP Android Kotlin

Application Android écrite en Kotlin qui convertit un montant d'une devise vers une autre à partir de taux fixes (USD, EUR, TND, GBP, JPY, CAD). L'euro sert de devise pivot.

## Fonctionnalités

- **Un seul écran** (`activity_main.xml`) construit avec un `ConstraintLayout` :
  - zone d'affichage turquoise (`#2BB8E8`), qui occupe 45 % de l'écran grâce à une `Guideline` : titre, deux `Spinner` de devises, montant, bouton d'inversion, résultat, bouton **Convertir** ;
  - clavier numérique personnalisé dans un `TableLayout` de 4 × 3 (chiffres `#1BA6C9`, touche **C** `#F0727A`, boutons sans bordure en 32sp).
- **Saisie uniquement par le clavier de l'application** : le montant et le résultat sont des `TextView`.
- **Deux `Spinner`** remplis par un `ArrayAdapter` à partir du `string-array` `devises` (par défaut, USD → EUR).
- **View Binding** pour accéder aux vues.
- **Conversion par l'euro** : `montant / taux(source) × taux(cible)`. Le résultat est arrondi à 2 décimales.
- **Inversion** des devises avec relance de la conversion.
- **Règles de gestion** :
  - un seul point décimal ;
  - 10 caractères au maximum ;
  - le premier chiffre tapé remplace le « 0 » initial ;
  - le Toast « Veuillez saisir un montant » s'affiche si le montant vaut 0 ;
  - si les deux devises sont identiques, le résultat est égal au montant ;
  - l'écran est bloqué en portrait.

### Bonus réalisés

| Bonus | Mise en œuvre |
|---|---|
| Conversion automatique | La conversion se relance à chaque touche et à chaque changement de devise (`OnItemSelectedListener`). |
| Taux réels via une API | Bascule **Live** : Retrofit et les coroutines interrogent `https://open.er-api.com/v6/latest/EUR`. En cas d'échec (pas de réseau, délai de 5 s dépassé), l'application revient aux taux fixes et affiche un Toast. |
| Conservation de l'état | `ConvertisseurViewModel` conserve le montant, le résultat, les devises et la source des taux quand l'Activity est recréée. |

## Taux fixes (pour 1 EUR)

| EUR | USD | TND | GBP | JPY | CAD |
|---|---|---|---|---|---|
| 1.00 | 1.17 | 3.40 | 0.87 | 172.50 | 1.62 |

## Cas de test

| Montant | Source | Cible | Résultat attendu |
|---|---|---|---|
| 120 | USD | EUR | 102.56 |
| 100 | EUR | TND | 340.00 |
| 50 | GBP | USD | 67.24 |
| 10 | EUR | EUR | 10.00 |

Ces cas sont aussi vérifiés par des tests unitaires JVM (`app/src/test/.../ConversionTest.kt`) :

```bash
./gradlew test
```

Les captures d'écran se trouvent dans le dossier [`screenshots/`](screenshots/).

## Structure

```
app/src/main/
├── java/com/example/currencyconverter/
│   ├── MainActivity.kt            # interface, clavier, conversion, inversion
│   ├── Conversion.kt              # calcul pur (euro pivot), testable
│   ├── ConvertisseurViewModel.kt  # état conservé + chargement des taux réels
│   └── ServiceTauxChange.kt       # interface Retrofit de l'API de taux
├── res/layout/activity_main.xml   # écran (ConstraintLayout + TableLayout)
├── res/layout/item_devise.xml     # élément des Spinner (texte blanc)
├── res/values/strings.xml         # textes + string-array « devises »
├── res/values/colors.xml          # couleurs de la maquette
└── res/values/dimens.xml          # tailles et marges
```

## Configuration

- Android Studio, template *Empty Views Activity*, Kotlin
- `minSdk` 24, `targetSdk`/`compileSdk` 35
- AGP 8.7.3, Kotlin 2.0.21, Gradle 8.9
- `buildFeatures { viewBinding = true }`

## Lancer le projet

1. Ouvrir le dossier dans Android Studio (**File → Open**).
2. Laisser Gradle se synchroniser.
3. Lancer l'application sur un émulateur ou un téléphone (API 24 ou plus).
