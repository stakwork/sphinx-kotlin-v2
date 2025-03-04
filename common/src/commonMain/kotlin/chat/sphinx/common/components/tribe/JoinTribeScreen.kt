package chat.sphinx.common.components.tribe

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.wrapper.tribe.TribeJoinLink

@Composable
expect fun JoinTribeScreen(dashboardViewModel: DashboardViewModel, tribeJoinLink: TribeJoinLink, preferredSize: DpSize)