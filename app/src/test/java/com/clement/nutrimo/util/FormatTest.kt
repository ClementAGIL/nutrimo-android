package com.clement.nutrimo.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun `numbers use a French comma and drop useless zeros`() {
        assertEquals("72,5", fmt(72.5))
        assertEquals("72", fmt(72.0))
        assertEquals("1,25", fmt(1.25, 2))
        assertEquals("2497 kcal", kcal(2497.4))
        assertEquals("130,1 kg", kg(130.1))
    }

    @Test
    fun `counts group thousands`() {
        // French grouping uses a (narrow) no-break space.
        assertEquals("12 500", count(12_500).replace(' ', ' ').replace(' ', ' '))
    }

    @Test
    fun `decimals are parsed with a comma or a dot`() {
        assertEquals(72.5, "72,5".toDecimal()!!, 0.0)
        assertEquals(72.5, " 72.5 ".toDecimal()!!, 0.0)
        assertEquals(null, "abc".toDecimal())
        assertEquals("72,5", 72.5.toInput())
    }
}
