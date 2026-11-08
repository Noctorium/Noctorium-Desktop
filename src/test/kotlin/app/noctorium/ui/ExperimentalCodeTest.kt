package app.noctorium.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExperimentalCodeTest {
    @Test
    fun `a code is found however it is typed`() {
        assertEquals(ExperimentalCode.CRASH, ExperimentalCode.find("crash"))
        assertEquals(ExperimentalCode.CRASH, ExperimentalCode.find("  CRASH "))
        assertEquals(ExperimentalCode.CRASH, ExperimentalCode.find("Crash"))
        assertEquals(ExperimentalCode.SHADOW, ExperimentalCode.find("sh@d0w"))
        assertEquals(ExperimentalCode.SHADOW_CHAOS, ExperimentalCode.find("sh@d0w2"))
        assertEquals(ExperimentalCode.SHADOW_CHAOS, ExperimentalCode.find(" SH@D0W2 "))
        assertNull(ExperimentalCode.find("sh@d0w22"))
        assertEquals(ExperimentalCode.SHADOW, ExperimentalCode.find(" SH@D0W "))
    }

    @Test
    fun `anything else is no code`() {
        assertNull(ExperimentalCode.find(""))
        assertNull(ExperimentalCode.find("crashes"))
        assertNull(ExperimentalCode.find("cr ash"))
        assertNull(ExperimentalCode.find("shadow"), "the code is spelt as it is")
    }
}
