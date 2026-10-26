// Small enums of the domain, with their French labels.
package com.clement.nutrimo.domain.model

enum class Sex(val label: String) { FEMALE("Femme"), MALE("Homme") }

/** Multipliers of the basal metabolic rate (standard Harris-Benedict / Mifflin activity factors). */
enum class ActivityLevel(val factor: Double, val label: String, val description: String) {
    SEDENTARY(1.2, "Sédentaire", "Peu ou pas d'exercice"),
    LIGHT(1.375, "Légère", "Exercice 1 à 3 jours / semaine"),
    MODERATE(1.55, "Modérée", "Exercice 3 à 5 jours / semaine"),
    HIGH(1.725, "Élevée", "Exercice 6 à 7 jours / semaine"),
    VERY_HIGH(1.9, "Très élevée", "Travail physique ou sport intense"),
}

enum class Meal(val label: String) {
    BREAKFAST("Petit-déjeuner"),
    LUNCH("Déjeuner"),
    DINNER("Dîner"),
    SNACK("Collations"),
}

enum class FoodSource(val label: String) { CIQUAL("CIQUAL"), OFF("OFF"), CUSTOM("Perso") }

/** How a diary quantity is expressed: grams, recipe parts, or a number of a named portion. */
enum class QuantityUnit { GRAM, PART, PORTION }

enum class GoalMode { PACE, DATE }

enum class GoalStatus(val label: String) { ACTIVE("En cours"), ACHIEVED("Atteint"), ARCHIVED("Archivé") }
