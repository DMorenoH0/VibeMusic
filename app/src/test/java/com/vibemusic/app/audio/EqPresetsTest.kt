package com.vibemusic.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqPresetsTest {

    @Test
    fun `there are twelve uniquely named presets`() {
        assertEquals(12, EqPresets.all.size)
        assertEquals(
            EqPresets.all.size,
            EqPresets.all.map { it.name.lowercase() }.distinct().size
        )
    }

    @Test
    fun `every preset defines one gain per anchor band inside the allowed range`() {
        EqPresets.all.forEach { preset ->
            assertEquals(preset.name, EqPresets.anchorsHz.size, preset.gainsDb.size)
            preset.gainsDb.forEach { gain ->
                assertTrue("${preset.name} gain $gain", gain in -12f..12f)
            }
            assertEquals(preset.name, EqPresets.anchorLabels.size, EqPresets.anchorsHz.size)
        }
    }

    @Test
    fun `balanced preset is flat`() {
        assertTrue(EqPresets.byName("Balanced").gainsDb.all { it == 0f })
    }

    @Test
    fun `lookup is case insensitive and falls back to balanced`() {
        assertEquals("Vocal", EqPresets.byName("vocal").name)
        assertEquals("Balanced", EqPresets.byName("does not exist").name)
        assertEquals("Balanced", EqPresets.byName(null).name)
    }
}
