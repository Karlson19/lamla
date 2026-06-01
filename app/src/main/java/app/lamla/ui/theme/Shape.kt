package app.lamla.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shapes.
 *
 * Material 3 default leans pill-shaped. We pull it back - small components 8dp,
 * medium 14dp (default card), large 20dp. Avoids the "every button is a fingerlike
 * pill" look that screams Material You.
 */
internal val LamlaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
