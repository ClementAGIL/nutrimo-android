package com.clement.nutrimo.domain.calc

import com.clement.nutrimo.domain.model.NutrientCatalog
import com.clement.nutrimo.domain.model.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyNeedsTest {
    @Test
    fun `references depend on sex`() {
        assertEquals(360.0, DailyNeeds.of(Sex.FEMALE, 40).getValue("magnesium").amount, 0.0)
        assertEquals(420.0, DailyNeeds.of(Sex.MALE, 40).getValue("magnesium").amount, 0.0)
    }

    @Test
    fun `young adults need more calcium`() {
        assertEquals(1000.0, DailyNeeds.of(Sex.MALE, 22).getValue("calcium").amount, 0.0)
        assertEquals(950.0, DailyNeeds.of(Sex.MALE, 30).getValue("calcium").amount, 0.0)
    }

    @Test
    fun `fiber is a need, salt a limit`() {
        val needs = DailyNeeds.of(Sex.FEMALE, 40)
        val fiber = needs.getValue(DailyNeeds.FIBER)
        assertFalse(fiber.limit)
        assertEquals(0.71, fiber.ratio(21.4), 0.01)
        assertTrue(needs.getValue(DailyNeeds.SALT).limit)
    }

    @Test
    fun `every reference is a known nutrient`() {
        val known = NutrientCatalog.MICROS.map { it.key }.toSet() + setOf(DailyNeeds.FIBER, DailyNeeds.SALT, DailyNeeds.SUGARS, DailyNeeds.SAT_FAT)
        Sex.entries.forEach { sex -> assertTrue(DailyNeeds.of(sex, 40).keys.all { it in known }) }
    }
}
