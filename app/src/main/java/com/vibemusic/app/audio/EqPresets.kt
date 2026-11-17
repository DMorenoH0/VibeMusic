package com.vibemusic.app.audio

/**
 * Five conceptual bands shared by every preset. Real devices expose a
 * different number of bands; [EqualizerController] maps each real band to the
 * nearest anchor instead of pretending the hardware has five controls.
 */
data class EqPreset(
    val name: String,
    val description: String,
    val gainsDb: List<Float>
)

object EqPresets {

    val anchorsHz = listOf(60, 230, 910, 3600, 14000)
    val anchorLabels = listOf("Subgraves", "Graves", "Medios", "Medios-altos", "Agudos")

    val all: List<EqPreset> = listOf(
        EqPreset("Bass Boost", "Graves presentes sin tapar la voz", listOf(5f, 4f, 0f, -1f, 0f)),
        EqPreset("Deep Bass", "Subgraves profundos para electrónica", listOf(8f, 6f, 0f, -1f, -2f)),
        EqPreset("Reggaetón", "Graves y brillo para ritmos latinos", listOf(6f, 3f, 1f, 2f, 3f)),
        EqPreset("Dembow", "Percusión y graves marcados", listOf(7f, 4f, 0f, 2f, 2f)),
        EqPreset("Hip-Hop", "Batería con medios limpios", listOf(6f, 3f, 0f, 1f, 2f)),
        EqPreset("Pop", "Voz y medios equilibrados", listOf(1f, 2f, 2f, 2f, 1f)),
        EqPreset("Rock", "Guitarras y presencia", listOf(3f, 1f, -1f, 2f, 4f)),
        EqPreset("Electrónica", "Graves y agudos energéticos", listOf(5f, 3f, 0f, 2f, 4f)),
        EqPreset("Vocal", "Voces al frente", listOf(-2f, -1f, 4f, 3f, 0f)),
        EqPreset("Balanced", "Respuesta plana", listOf(0f, 0f, 0f, 0f, 0f)),
        EqPreset("Acoustic", "Cuerdas y sala natural", listOf(1f, 1f, 2f, 1f, 1f)),
        EqPreset("Custom", "Ajusta cada banda a tu gusto", listOf(0f, 0f, 0f, 0f, 0f))
    )

    fun byName(name: String?): EqPreset =
        all.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: all.first { it.name == "Balanced" }
}
