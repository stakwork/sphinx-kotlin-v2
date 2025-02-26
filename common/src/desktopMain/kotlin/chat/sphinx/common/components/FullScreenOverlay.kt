package chat.sphinx.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.components.profile.ProfileScreen
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.utils.getPreferredWindowSize

@Composable
fun FullScreenOverlay(
    fullScreenView: DashboardViewModel.FullScreenView,
    dashboardViewModel: DashboardViewModel,
    onClose: () -> Unit
) {
    if (fullScreenView != DashboardViewModel.FullScreenView.None) {
        val preferredSize = remember { getPreferredWindowSize(420, 830) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black.copy(0.60f))
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
                Card(
                    modifier = Modifier.size(preferredSize),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
                ) {
                    when (fullScreenView) {
                        is DashboardViewModel.FullScreenView.Profile -> ProfileScreen(dashboardViewModel, preferredSize)
                        is DashboardViewModel.FullScreenView.Transactions -> TransactionsScreen(dashboardViewModel, preferredSize)
                        is DashboardViewModel.FullScreenView.ContactScreen -> AddContactScreen(dashboardViewModel, preferredSize)
                        else -> {}
                    }
                }
            }
        }
    }
}


@Composable
fun TopHeaderContainer(
    title: String,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .size(20.dp)
                        .align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = if (showBackButton) 48.dp else 24.dp),
            )

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .clickable { onClose() }
            )
        }
        Divider(color = Color.Black.copy(0.60f), thickness = 1.dp)
        content()
    }
}

