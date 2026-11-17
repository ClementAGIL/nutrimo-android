package com.clement.nutrimo.domain.calc

import com.clement.nutrimo.R
import com.clement.nutrimo.domain.model.Loggable
import com.clement.nutrimo.domain.model.Nutrients
import com.clement.nutrimo.domain.model.Portion
import com.clement.nutrimo.domain.model.QuantityUnit
import com.clement.nutrimo.ui.profile.ProfileDraft
import com.clement.nutrimo.util.UiText
import com.clement.nutrimo.util.uiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivitiesTest {
    @Test
    fun `activity calories are net of the resting energy`() {
        // Walking (MET 3.5), 130 kg, 1 h: (3.5 - 1) × 130 = 325 kcal, not 455.
        assertEquals(325.0, Activities.burned(3.5, 130.0, 60), 0.001)
    }

    @Test
    fun `an effort at or below rest burns nothing extra`() {
        assertEquals(0.0, Activities.burned(1.0, 80.0, 60), 0.0)
        assertEquals(0.0, Activities.burned(0.5, 80.0, 60), 0.0)
    }
}

class LoggableUnitsTest {
    @Test
    fun `unit labels are resources or plain symbols, never French text`() {
        val food = Loggable(name = "Yaourt", per100 = Nutrients(kcal = 60.0), portions = listOf(Portion("1 pot", 125.0)))
        assertEquals(listOf(QuantityUnit.GRAM, QuantityUnit.PORTION), food.units.map { it.unit })
        assertEquals(UiText.Raw("g"), food.units[0].label)
        val recipe = Loggable(recipeId = "r", name = "Gâteau", per100 = null, perPart = Nutrients(kcal = 250.0))
        assertEquals(uiText(R.string.common_parts_unit), recipe.units.single().label)
    }
}

class ProfileDraftTest {
    @Test
    fun `the water goal must be a real amount`() {
        assertTrue(ProfileDraft(height = "180", waterGoal = "2000").isValid)
        assertFalse(ProfileDraft(height = "180", waterGoal = "0").isValid)
        assertFalse(ProfileDraft(height = "180", waterGoal = "").isValid)
    }
}

class WeightRangeTest {
    @Test
    fun `typos are outside the plausible weight range`() {
        assertTrue(130.0 in VALID_WEIGHT_KG)
        assertFalse(1300.0 in VALID_WEIGHT_KG)
        assertFalse(1.0 in VALID_WEIGHT_KG)
    }
}
