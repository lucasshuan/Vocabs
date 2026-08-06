package io.github.lucasshuan.vocabu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class Tab(val route: String, val icon: ImageVector, val label: String, val badge: Int = 0)

/**
 * Icons only: four words competing with the capture button take away the one
 * thing it has to be. The names live in `contentDescription`.
 *
 * The `+` is not here, for a technical reason before a visual one: `Surface`
 * clips its content and the fan has to draw above the bar's top edge. The layer
 * above composes it against [BAR_HEIGHT], which is why that is public.
 */
@Composable
fun BottomBar(
    leftTabs: List<Tab>,
    rightTabs: List<Tab>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(BAR_HEIGHT)
                    .padding(horizontal = 8.dp),
            ) {
                leftTabs.forEach { TabItem(it, currentRoute == it.route, { onNavigate(it.route) }, Modifier.weight(1f)) }
                Spacer(Modifier.weight(1f))
                rightTabs.forEach { TabItem(it, currentRoute == it.route, { onNavigate(it.route) }, Modifier.weight(1f)) }
            }
        }
    }
}

/** Without insets. The capture button aligns to it. */
val BAR_HEIGHT = 68.dp

/**
 * With no label the icon is the only thing answering the touch, and a colour
 * change on a 23dp drawing is nearly invisible at the edge of vision. It grows
 * 12% and returns, confirming the touch before the new screen has drawn.
 */
@Composable
private fun TabItem(tab: Tab, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val tint by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(Motion.DEFAULT),
        label = "tabTint",
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = Motion.elasticSpring(),
        label = "tabScale",
    )
    val touch = rememberHaptics()

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = CircleShape,
        interactionSource = touch,
        modifier = modifier.height(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = tint,
                    modifier = Modifier.size(23.dp),
                )
                // Springs in and shrinks out: Pending changes on its own in the
                // background, and a number appearing from nowhere in the icon's
                // corner reads as a drawing defect, not as "something arrived".
                AnimatedVisibility(
                    visible = tab.badge > 0,
                    enter = scaleIn(Motion.elasticSpring()) + fadeIn(tween(Motion.FAST)),
                    exit = scaleOut(tween(Motion.FAST)) + fadeOut(tween(Motion.FAST)),
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Badge { Text(tab.badge.coerceAtMost(99).toString()) }
                }
            }
        }
    }
}
