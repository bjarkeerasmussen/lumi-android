package com.lumi.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

/** Bottom-bar destinations. Skin Check and Settings are reached from screens. */
enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Today("today", "Today", Icons.Outlined.CameraAlt),
    Progress("progress", "Progress", Icons.Outlined.Timeline),
    Routine("routine", "Routine", Icons.Outlined.Spa),
    Learn("learn", "Learn", Icons.Outlined.AutoStories)
}

object Routes {
    const val SKIN_CHECK = "skincheck"
    const val SETTINGS = "settings"
}
