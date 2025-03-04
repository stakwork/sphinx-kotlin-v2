package chat.sphinx.common.components.tribe

import Roboto
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import chat.sphinx.common.components.PhotoFileImage
import chat.sphinx.common.components.PhotoUrlImage
import chat.sphinx.common.components.TopHeaderContainer
import chat.sphinx.common.components.notifications.DesktopSphinxToast
import chat.sphinx.common.state.ContentState
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.common.viewmodel.chat.JoinTribeViewModel
import chat.sphinx.response.LoadResponse
import chat.sphinx.response.Response
import chat.sphinx.utils.getPreferredWindowSize
import chat.sphinx.wrapper.chat.fixedAlias
import chat.sphinx.wrapper.message.media.isImage
import chat.sphinx.wrapper.tribe.TribeJoinLink
import kotlinx.coroutines.launch
import theme.light_divider
import utils.deduceMediaType

@Composable
actual fun JoinTribeScreen(
    dashboardViewModel: DashboardViewModel,
    tribeJoinLink: TribeJoinLink,
    preferredSize: DpSize
) {
    val viewModel = JoinTribeViewModel(tribeJoinLink, dashboardViewModel)

    Box(
        modifier = Modifier
            .size(preferredSize)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopHeaderContainer(
                title = "Join Tribe",
                onClose = { dashboardViewModel.closeFullScreenView() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PhotoUrlImage(
                        photoUrl = viewModel.joinTribeState.img,
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        viewModel.joinTribeState.name,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        viewModel.joinTribeState.description,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Card(
                            border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.onBackground),
                            shape = RoundedCornerShape(4.dp),
                            backgroundColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Column {
                                PriceBox("Price to join:", viewModel.joinTribeState.price_to_join)
                                Divider(color = MaterialTheme.colorScheme.onBackground, thickness = 1.dp)
                                PriceBox("Price per message:", viewModel.joinTribeState.price_per_message)
                                Divider(color = MaterialTheme.colorScheme.onBackground, thickness = 1.dp)
                                PriceBox("Amount to stake:", viewModel.joinTribeState.escrow_amount)
                                Divider(color = MaterialTheme.colorScheme.onBackground, thickness = 1.dp)
                                PriceBox("Time to stake: (hours)", viewModel.joinTribeState.hourToStake)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        TribeTextField("Alias", viewModel.joinTribeState.userAlias) {
                            viewModel.onAliasTextChanged(it)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                shape = RoundedCornerShape(30.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(45.dp),
                                onClick = {
                                    viewModel.joinTribe()
                                }
                            ) {
                                Text(
                                    text = "JOIN TRIBE",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.W800,
                                    fontFamily = Roboto
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriceBox(
    labelName: String,
    value: String,
) {
    Row (
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        Text(labelName, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp)
        Text(value, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp)
    }
}
