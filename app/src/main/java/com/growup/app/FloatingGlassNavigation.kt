package com.growup.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Original implementation; reference design research is documented in docs/OPEN_SOURCE_RESEARCH.md. */
@Composable
fun FloatingGlassNavigation(selected: Int, labels: List<String>, icons: List<ImageVector>, select: (Int) -> Unit) {
    val position by animateFloatAsState(selected.toFloat(), spring(.82f, 430f), label = "glassPosition")
    Surface(
        modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(36.dp),
        color = Color(0xF0FAFCF9),
        border = BorderStroke(1.dp, Color.White),
        shadowElevation = 10.dp
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(70.dp).padding(5.dp)) {
            val tabWidth = maxWidth / labels.size
            Box(Modifier.width(tabWidth).fillMaxHeight().graphicsLayer { translationX = tabWidth.toPx() * position }
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD5EEE5))), RoundedCornerShape(30.dp))
                .border(1.dp, Color.White, RoundedCornerShape(30.dp)))
            Row(Modifier.fillMaxSize().selectableGroup()) {
                labels.forEachIndexed { index, label ->
                    val color by animateColorAsState(if (index == selected) Color(0xFF087453) else Color(0xFF68746F), label = "navTint")
                    val scale by animateFloatAsState(if (index == selected) 1.06f else 1f, spring(.7f, 500f), label = "navScale")
                    Column(Modifier.weight(1f).fillMaxHeight().selectable(index == selected, role = Role.Tab, onClick = { select(index) }),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(icons[index], contentDescription = null, tint = color, modifier = Modifier.size(23.dp).graphicsLayer { scaleX = scale; scaleY = scale })
                        Spacer(Modifier.height(3.dp))
                        Text(label, color = color, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
