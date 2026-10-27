// Profile form (sex, birth date, height, activity, water goal), shared by onboarding and the profile screen.
package com.clement.nutrimo.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.data.db.ProfileEntity
import com.clement.nutrimo.domain.calc.DEFAULT_WATER_GOAL_ML
import com.clement.nutrimo.domain.model.ActivityLevel
import com.clement.nutrimo.domain.model.Sex
import com.clement.nutrimo.ui.common.ChoiceChips
import com.clement.nutrimo.ui.common.DateField
import com.clement.nutrimo.ui.common.Dropdown
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.toDecimal
import com.clement.nutrimo.ui.common.toInput
import java.time.LocalDate

private const val MIN_HEIGHT_CM = 100.0
private const val MAX_HEIGHT_CM = 250.0

data class ProfileDraft(
    val sex: Sex = Sex.FEMALE,
    val birthDate: LocalDate = LocalDate.now().minusYears(30),
    val height: String = "",
    val activity: ActivityLevel = ActivityLevel.LIGHT,
    val waterGoal: String = DEFAULT_WATER_GOAL_ML.toString(),
) {
    val heightCm: Double? get() = height.toDecimal()?.takeIf { it in MIN_HEIGHT_CM..MAX_HEIGHT_CM }
    val isValid: Boolean get() = heightCm != null

    fun toEntity() = ProfileEntity(
        sex = sex,
        birthDate = birthDate,
        heightCm = heightCm ?: 0.0,
        activity = activity,
        waterGoalMl = waterGoal.toDecimal()?.toInt() ?: DEFAULT_WATER_GOAL_ML,
    )

    companion object {
        fun from(p: ProfileEntity) = ProfileDraft(p.sex, p.birthDate, p.heightCm.toInput(), p.activity, p.waterGoalMl.toString())
    }
}

@Composable
fun ProfileForm(draft: ProfileDraft, onChange: (ProfileDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceChips(Sex.entries, draft.sex, { it.label }) { onChange(draft.copy(sex = it)) }
        DateField(
            label = "Date de naissance",
            date = draft.birthDate,
            onDateChange = { onChange(draft.copy(birthDate = it)) },
            max = LocalDate.now().minusYears(10),
            modifier = Modifier.fillMaxWidth(),
        )
        NumberField(
            value = draft.height,
            onValueChange = { onChange(draft.copy(height = it)) },
            label = "Taille",
            suffix = "cm",
            isError = draft.height.isNotEmpty() && draft.heightCm == null,
            modifier = Modifier.fillMaxWidth(),
        )
        Dropdown(
            title = "Activité physique",
            selected = draft.activity,
            options = ActivityLevel.entries,
            label = { "${it.label} — ${it.description}" },
            onSelect = { onChange(draft.copy(activity = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        NumberField(
            value = draft.waterGoal,
            onValueChange = { onChange(draft.copy(waterGoal = it)) },
            label = "Objectif d'eau par jour",
            suffix = "ml",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
