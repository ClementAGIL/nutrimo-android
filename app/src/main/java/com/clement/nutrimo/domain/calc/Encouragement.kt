// The daily encouragement: one kind message chosen from the user's real data. Celebrate weight milestones first, then
// protect a streak still open today, then be proud of goals beaten, then comfort after a hard day, else a gentle word.
// Wording is gender-neutral (no "fier / fière") and never guilt-inducing.
package com.clement.nutrimo.domain.calc

import com.clement.nutrimo.util.count
import com.clement.nutrimo.util.kg
import java.time.LocalDate
import kotlin.math.floor

data class Nudge(val title: String, val text: String)

data class EncouragementFacts(
    val today: LocalDate,
    val records: Map<LocalDate, DayRecord>,
    val targets: GoalTargets,
    /** Oldest first. */
    val weights: List<WeightPoint>,
    val goalStart: LocalDate? = null,
    val goalStartKg: Double? = null,
    val heightCm: Double? = null,
)

/** A streak is worth protecting from this many days in a row. */
private const val MIN_STREAK = 2
/** A weigh-in this recent (days) is still news. */
private const val RECENT_WEIGH_IN_DAYS = 1L
/** Days without anything logged before the "welcome back" message. */
private const val QUIET_DAYS = 2L

object Encouragement {
    fun pick(f: EncouragementFacts): Nudge =
        weightNews(f) ?: streakToKeep(f) ?: proudOfToday(f) ?: proudOfYesterday(f) ?: hardMoment(f) ?: gentle(f)

    private fun weightNews(f: EncouragementFacts): Nudge? {
        if (f.weights.size < 2) return null
        val last = f.weights.last()
        val previous = f.weights[f.weights.size - 2]
        if (last.date.isBefore(f.today.minusDays(RECENT_WEIGH_IN_DAYS)) || last.kg >= previous.kg) return null
        f.heightCm?.let { h ->
            val now = Bmi.zoneOf(Bmi.of(last.kg, h))
            if (Bmi.ZONES.indexOf(now) < Bmi.ZONES.indexOf(Bmi.zoneOf(Bmi.of(previous.kg, h)))) {
                return Nudge(
                    "Nouvelle catégorie d'IMC 🎉",
                    "Vous voici en « ${now.label} ». C'est un vrai cap, franchi grâce à vos efforts de chaque jour : prenez le temps de le savourer.",
                )
            }
        }
        f.goalStartKg?.let { start ->
            val lostNow = floor(start - last.kg).toInt()
            if (lostNow >= 1 && lostNow > floor(start - previous.kg).toInt()) {
                return Nudge(
                    "$lostNow kg de moins 🎉",
                    "Depuis le début de votre objectif, $lostNow kg de moins. Chaque kilo raconte des choix faits un jour après l'autre : bravo.",
                )
            }
        }
        val sinceStart = f.weights.filter { f.goalStart == null || !it.date.isBefore(f.goalStart) }.dropLast(1)
        if (sinceStart.isNotEmpty() && last.kg < sinceStart.minOf { it.kg }) {
            return Nudge("Nouveau plus bas 🌟", "${kg(last.kg)} : votre poids le plus bas depuis le début de l'objectif. Votre constance paie.")
        }
        return Nudge(
            "${kg(previous.kg - last.kg)} de moins",
            "Depuis votre dernière pesée, la balance descend. Continuez à votre rythme, vous êtes sur le bon chemin.",
        )
    }

    /** The longest streak still open today (not yet reached), with what is left to keep it. */
    private fun streakToKeep(f: EncouragementFacts): Nudge? {
        val today = f.records[f.today]
        val open = listOf(DailyGoal.STEPS, DailyGoal.WATER, DailyGoal.SPORT, DailyGoal.JOURNAL)
            .map { it to streak(it, f.records, f.targets, f.today).current }
            .filter { (goal, days) -> days >= MIN_STREAK && today?.achieved(goal, f.targets) != true }
            .maxByOrNull { it.second } ?: return null
        val (goal, days) = open
        val left = when (goal) {
            DailyGoal.STEPS -> "encore ${number(f.targets.steps - (today?.steps ?: 0))} pas, une petite marche peut suffire"
            DailyGoal.WATER -> "encore ${number(f.targets.waterMl - (today?.waterMl ?: 0))} ml d'eau"
            DailyGoal.JOURNAL -> "choisissez l'humeur du jour dans « Ma journée », un seul geste suffit"
            else -> "encore ${number(f.targets.sportKcal - (today?.sportKcal ?: 0.0).toInt())} kcal d'activité"
        }
        val what = when (goal) {
            DailyGoal.STEPS -> "Objectif de pas atteint"
            DailyGoal.WATER -> "Objectif d'eau atteint"
            DailyGoal.JOURNAL -> "Journée notée"
            else -> "Objectif de sport atteint"
        }
        return Nudge("$days jours d'affilée 🔥", "$what $days jours de suite. Pour continuer la série aujourd'hui : $left.")
    }

    /** Goals already beaten today (steps, sport, water), the biggest margin first. */
    private fun proudOfToday(f: EncouragementFacts): Nudge? {
        val d = f.records[f.today] ?: return null
        val steps = d.steps?.takeIf { d.achieved(DailyGoal.STEPS, f.targets) == true }
        if (steps != null) {
            return Nudge(
                "Objectif de pas atteint 👟",
                "${number(steps)} pas aujourd'hui, ${number(steps - f.targets.steps)} de plus que votre objectif. Votre corps vous remercie.",
            )
        }
        if (d.achieved(DailyGoal.SPORT, f.targets) == true) {
            return Nudge("Sport fait 💪", "${number(d.sportKcal.toInt())} kcal d'activité aujourd'hui, au-delà de votre objectif. Une belle victoire, savourez-la.")
        }
        if (d.achieved(DailyGoal.WATER, f.targets) == true) {
            return Nudge("Objectif d'eau atteint 💧","Objectif d'eau atteint aujourd'hui. Ce sont ces petits gestes qui font les grands changements.")
        }
        return null
    }

    private fun proudOfYesterday(f: EncouragementFacts): Nudge? {
        val d = f.records[f.today.minusDays(1)] ?: return null
        if (d.achieved(DailyGoal.CALORIES, f.targets) == true) {
            val eaten = d.eatenKcal ?: return null
            val budget = d.budgetKcal ?: return null
            return Nudge(
                "Belle journée hier ✨",
                "Hier, ${number(eaten.toInt())} kcal pour un budget de ${number(budget.toInt())} : une journée réussie de plus. Vous pouvez vous en féliciter.",
            )
        }
        if (d.achieved(DailyGoal.SPORT, f.targets) == true) {
            return Nudge("Hier, du sport 💪", "Votre objectif de sport d'hier est atteint. Un jour après l'autre, vous construisez quelque chose de solide.")
        }
        return null
    }

    private fun hardMoment(f: EncouragementFacts): Nudge? {
        val quiet = (1..QUIET_DAYS).none { f.records.containsKey(f.today.minusDays(it)) } && !f.records.containsKey(f.today)
        if (quiet && f.records.isNotEmpty()) {
            return Nudge(
                "Bon retour 🌱",
                "Quelques jours sans nouvelles, et ce n'est pas grave. Reprendre, c'est déjà réussir : notez simplement un repas aujourd'hui.",
            )
        }
        if (f.records[f.today.minusDays(1)]?.achieved(DailyGoal.CALORIES, f.targets) == false) {
            return Nudge(
                "Une nouvelle page 🌅",
                "Hier a été plus difficile, et c'est humain. Une journée ne défait pas vos progrès : accordez-vous de la douceur, aujourd'hui est une nouvelle page.",
            )
        }
        if (f.weights.size >= 2 && f.weights.last().kg > f.weights[f.weights.size - 2].kg &&
            !f.weights.last().date.isBefore(f.today.minusDays(RECENT_WEIGH_IN_DAYS))
        ) {
            return Nudge(
                "Regardez la tendance 📉",
                "Le poids varie d'un jour à l'autre (eau, sel, sommeil). Un chiffre ne dit pas tout : vos efforts comptent, continuez.",
            )
        }
        return null
    }

    private fun gentle(f: EncouragementFacts): Nudge = GENTLE[Math.floorMod(f.today.toEpochDay(), GENTLE.size.toLong()).toInt()]

    private val GENTLE = listOf(
        Nudge("Un pas après l'autre 🌱", "Le progrès n'est pas une ligne droite. Ce que vous faites aujourd'hui compte, même si c'est petit."),
        Nudge("De la douceur pour vous 💛","Parlez-vous comme à quelqu'un que vous aimez : avec patience et douceur. Vous faites de votre mieux."),
        Nudge("Un moment pour vous ✍️", "Qu'est-ce qui vous a fait du bien aujourd'hui ? Prenez une minute pour le noter dans « Ma journée »."),
        Nudge("Vous êtes encore là 💪", "Rester, même les jours difficiles, c'est la vraie force. Ne lâchez rien, vous avancez."),
        Nudge("Petites victoires ✨", "Un verre d'eau, quelques pas, un repas noté : chaque petite victoire mérite d'être fêtée."),
        Nudge("Votre rythme 🐢", "Il n'y a pas de course. Avancer doucement, c'est toujours avancer."),
        Nudge("Gratitude 🙏", "Ce soir, pensez à une chose pour laquelle vous ressentez de la gratitude. Notez-la dans « Ma journée »."),
    )

    /** Counts in messages are never negative ("encore 0 pas" rather than "-200"). */
    private fun number(n: Number): String = count(n.toLong().coerceAtLeast(0))
}
