package chat.sphinx.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.sphinx.common.components.profile.ProfileScreen
import chat.sphinx.common.viewmodel.DashboardViewModel

@Composable
fun FullScreenOverlay(
    fullScreenView: DashboardViewModel.FullScreenView,
    dashboardViewModel: DashboardViewModel,
    onClose: () -> Unit
) {
    if (fullScreenView != DashboardViewModel.FullScreenView.None) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.85f))
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.TopStart)
                    .padding(20.dp)
                    .clickable { onClose() }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(50.dp),
                contentAlignment = Alignment.Center
            ) {
                when (fullScreenView) {
                    is DashboardViewModel.FullScreenView.Profile -> ProfileScreen(dashboardViewModel)
                    else -> {}
                }
            }
        }
    }
}
