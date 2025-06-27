package chat.sphinx.common.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.sphinx.common.viewmodel.DashboardViewModel

@Composable
fun FeedListUI(
    dashboardViewModel: DashboardViewModel
) {
    Text(
        text = "Feed content goes here",
        modifier = Modifier.padding(16.dp),
    )
}