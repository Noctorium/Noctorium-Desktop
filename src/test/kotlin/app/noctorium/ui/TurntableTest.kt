package app.noctorium.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The turntable's arm, which is worked out from the song's progress and nothing else. */
class TurntableTest {

    private fun radiusAt(degrees: Float) = Tonearm.stylus(degrees).getDistance()

    @Test
    fun `the arm sets down at the record's edge and finishes at the run-out groove`() {
        assertEquals(Tonearm.LEAD_IN, radiusAt(Tonearm.degrees(0f)), 1e-3f)
        assertEquals(Tonearm.RUN_OUT, radiusAt(Tonearm.degrees(1f)), 1e-3f)
    }

    @Test
    fun `the stylus moves in evenly as the song plays, as along a groove of even pitch`() {
        (0..20).forEach { step ->
            val progress = step / 20f
            val expected = Tonearm.LEAD_IN + (Tonearm.RUN_OUT - Tonearm.LEAD_IN) * progress
            assertEquals(expected, radiusAt(Tonearm.degrees(progress)), 1e-3f, "at $progress")
        }
    }

    @Test
    fun `the arm only ever swings inwards, and about as far as a real one does`() {
        val angles = (0..100).map { Tonearm.degrees(it / 100f) }
        angles.zipWithNext().forEach { (before, after) -> assertTrue(after > before, "$after does not follow $before") }
        val sweep = angles.last() - angles.first()
        assertTrue(sweep in 15f..30f, "the arm swings $sweep degrees across a song")
    }

    @Test
    fun `with nothing loaded the arm rests clear of the record`() {
        assertEquals(Tonearm.REST_DEGREES, Tonearm.degrees(null))
        assertTrue(radiusAt(Tonearm.degrees(null)) > 1.15f, "the resting stylus is over the record")
    }

    @Test
    fun `a place outside the song is the nearer end of it`() {
        assertEquals(Tonearm.degrees(0f), Tonearm.degrees(-.4f))
        assertEquals(Tonearm.degrees(1f), Tonearm.degrees(1.6f))
    }

    @Test
    fun `the stylus crosses the side of the record nearer the arm`() {
        (0..10).forEach { step ->
            val stylus = Tonearm.stylus(Tonearm.degrees(step / 10f))
            assertTrue(stylus.x > 0f && stylus.y > 0f, "the stylus is at $stylus, not between the spindle and the arm")
        }
    }
}
