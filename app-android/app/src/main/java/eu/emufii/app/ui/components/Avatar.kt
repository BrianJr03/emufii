package eu.emufii.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import eu.emufii.app.profile.avatarPaletteFor
import eu.emufii.app.profile.initialsFor
import eu.emufii.app.ui.theme.Coral
import eu.emufii.app.ui.theme.GlyphInk
import eu.emufii.app.ui.theme.Teal
import eu.emufii.app.ui.theme.Violet
import eu.emufii.app.ui.theme.VioletDark
import java.io.File

/**
 * A player, as a circle. Without a picture, initials on a colour derived from the
 * name: stable across sessions, so a player is recognisable without uploading anything.
 */
@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    imageFile: File? = null,
    size: Dp = 40.dp,
    ring: Color? = null
) {
    val context = LocalContext.current
    val (c1, c2) = AVATAR_PALETTE[avatarPaletteFor(name, AVATAR_PALETTE.size)]

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(c1, c2)))
            .then(
                if (ring != null) Modifier.border(BorderStroke(2.dp, ring), CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (imageFile != null) {
            AsyncImage(
                // The file path never changes: without a cache key tied to its mtime,
                // Coil keeps serving the previous picture after the user picks a new one.
                model = ImageRequest.Builder(context)
                    .data(imageFile)
                    .memoryCacheKey("avatar-${imageFile.lastModified()}")
                    .diskCacheKey("avatar-${imageFile.lastModified()}")
                    .build(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            // The bright cuts are light, so white initials vanish on them: each axis's own
            // ink reads on its bright face, white on everything else.
            val onFace =
                if (c1 == Coral.bright || c1 == Teal.bright) GlyphInk
                else Color.White
            androidx.compose.material3.Text(
                text = initialsFor(name),
                color = onFace,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.36f).sp
            )
        }
    }
}

/**
 * Remixes of the logo's own two axes, nothing else: coral crossing teal, each
 * crossing violet (the gradient's depth end), each axis against its own deep
 * cut. Eight rather than twelve: the old palette imported hues this world does
 * not carry (blues, yellows, pure greens); the cost is a collision more often,
 * and the gain is that a player's circle is made of the app's own colours.
 * pourquoi : docs/decisions/theme-duotone-shelves.md § Avatars
 */
private val AVATAR_PALETTE = listOf(
    Coral.bright to Teal.bright,
    Coral.bright to Violet,
    Teal.bright to Violet,
    Coral.deep to Coral.bright,
    Teal.deep to Teal.bright,
    Violet to Coral.deep,
    VioletDark to Teal.bright,
    Coral.ink to Coral.bright
)
