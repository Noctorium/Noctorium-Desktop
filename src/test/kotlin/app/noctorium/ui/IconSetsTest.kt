package app.noctorium.ui

import app.noctorium.settings.IconSet
import app.noctorium.ui.icons.DESKTOP_ICON_SETS
import app.noctorium.ui.icons.ICON_VARIANTS
import app.noctorium.ui.icons.LineIcons
import app.noctorium.ui.icons.inSet
import app.noctorium.ui.icons.onDesktop
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The icon sets stay whole: every icon the desktop draws has its place in every set, and every icon is drawn
 * through the one call that looks the set up.
 */
class IconSetsTest {

    private val sources: List<File> = File("src/main/kotlin").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** Every icon named anywhere in the desktop's code, by the name its vector carries: "Filled.Pause". */
    private val named: Set<String> = sources.filterNot { it.parentFile.name == "icons" }.flatMap { file ->
        Regex("""Icons\.(Default|Filled|Outlined|AutoMirrored\.Filled)\.(\w+)""").findAll(file.readText()).map { match ->
            val set = when (match.groupValues[1]) {
                "Default" -> "Filled"
                else -> match.groupValues[1]
            }
            "$set.${match.groupValues[2]}"
        }.toList()
    }.toSet()

    @Test
    fun `every icon the desktop draws has its place in the table`() {
        assertTrue(named.size > 100, "the search for icons found too few to be right: ${named.size}")
        val missing = named.filter { it !in ICON_VARIANTS }
        assertTrue(missing.isEmpty(), "No icon set knows these: $missing")
    }

    @Test
    fun `every icon has a version in every set, of that set`() {
        ICON_VARIANTS.values.forEach { icon ->
            val standard = icon.standard()
            assertEquals(icon.name, standard.name, "the table's name for ${icon.name} is not its icon's")
            IconSet.entries.forEach { set ->
                val drawn = icon.inSet(set)
                assertTrue(drawn.root.size > 0 || drawn.name.isNotEmpty(), "${icon.name} has nothing in $set")
                val expected = when (set) {
                    IconSet.STANDARD, IconSet.FILLED -> listOf("Filled.", "Outlined.PushPin", "AutoMirrored.Filled.")
                    IconSet.OUTLINED -> listOf("Outlined.", "AutoMirrored.Outlined.", "Filled.Favorite", "Filled.PushPin")
                    IconSet.ROUNDED -> listOf("Rounded.", "AutoMirrored.Rounded.", "Outlined.PushPin")
                    IconSet.SHARP -> listOf("Sharp.", "AutoMirrored.Sharp.", "Outlined.PushPin")
                    IconSet.TWO_TONE -> listOf("TwoTone.", "AutoMirrored.TwoTone.", "Outlined.PushPin")
                    IconSet.LINE -> listOf("Line.")
                }
                assertTrue(expected.any { drawn.name.startsWith(it) }, "${icon.name} in $set is ${drawn.name}")
            }
        }
    }

    @Test
    fun `every line icon the table names is drawn`() {
        ICON_VARIANTS.values.forEach { icon ->
            assertTrue(icon.line in LineIcons.names, "${icon.name} asks for a line icon there is none of: ${icon.line}")
            val drawn = LineIcons.get(icon.line, icon.lineFilled)
            assertTrue(drawn.root.size > 0, "${icon.line} draws nothing")
        }
    }

    @Test
    fun `a liked heart and a pinned pin never look like their opposites`() {
        IconSet.entries.forEach { set ->
            assertNotEquals(ICON_VARIANTS.getValue("Filled.Favorite").inSet(set).name, ICON_VARIANTS.getValue("Filled.FavoriteBorder").inSet(set).name, "hearts in $set")
            assertNotEquals(ICON_VARIANTS.getValue("Filled.PushPin").inSet(set).name, ICON_VARIANTS.getValue("Outlined.PushPin").inSet(set).name, "pins in $set")
        }
    }

    @Test
    fun `an icon looked up in a set is drawn from it, and Standard is the icon itself`() {
        val pause = ICON_VARIANTS.getValue("Filled.Pause").standard()
        assertEquals(pause, pause.inSet(IconSet.STANDARD))
        assertEquals("Outlined.Pause", pause.inSet(IconSet.OUTLINED).name)
        assertEquals("Line.pause", pause.inSet(IconSet.LINE).name)
    }

    @Test
    fun `the desktop offers every set but the one Standard already is`() {
        assertEquals(IconSet.entries.toSet() - IconSet.FILLED, DESKTOP_ICON_SETS.toSet())
        assertEquals(IconSet.STANDARD, DESKTOP_ICON_SETS.first())
        assertEquals(IconSet.STANDARD, IconSet.FILLED.onDesktop())
        assertEquals(IconSet.LINE, IconSet.LINE.onDesktop())
    }

    @Test
    fun `every icon goes through the call that looks up the set`() {
        sources.filterNot { it.parentFile.name == "icons" }.forEach { file ->
            val text = file.readText()
            assertFalse(Regex("""^import androidx\.compose\.material3\.Icon$""", RegexOption.MULTILINE).containsMatchIn(text), "${file.name} imports Material's Icon")
            assertFalse("androidx.compose.material3.Icon(" in text, "${file.name} calls Material's Icon by name")
            // A file that takes all of Material 3 also has to take this Icon, or Material's would be the one drawn.
            if ("import androidx.compose.material3.*" in text && Regex("""\bIcon\(""").containsMatchIn(text)) {
                assertTrue("import app.noctorium.ui.icons.Icon" in text, "${file.name} draws icons with Material's Icon")
            }
        }
    }
}
