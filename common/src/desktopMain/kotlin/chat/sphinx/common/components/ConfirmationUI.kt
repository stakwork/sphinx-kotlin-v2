package chat.sphinx.common.components

import Roboto
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import chat.sphinx.common.Res
import chat.sphinx.common.state.ConfirmationType
import chat.sphinx.platform.imageResource
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.utils.getPreferredWindowSize
import theme.primary_blue

@Composable
fun ConfirmationUI(
    dashboardViewModel: DashboardViewModel,
    confirmationType: ConfirmationType
) {
    var isOpen by remember { mutableStateOf(true) }
    val confirmationWindowState by dashboardViewModel.confirmationStateFlow.collectAsState()

    if (isOpen && confirmationWindowState.first) {
        Window(
            onCloseRequest = {
                dashboardViewModel.toggleConfirmationWindow(false)
            },
            title = when (confirmationType) {
                is ConfirmationType.PayInvoice -> "Confirm Payment"
                is ConfirmationType.TribeDeleteMember -> "Confirm Delete Member"
                is ConfirmationType.ContactDelete -> "Confirm Delete Contact"
            },
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = getPreferredWindowSize(300, 190)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(4.dp))

                Image(
                    painter = imageResource(Res.drawable.sphinx_logo),
                    contentDescription = null,
                    modifier = Modifier.height(50.dp).width(50.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(12.dp))

                Text(
                    text = when (confirmationType) {
                        is ConfirmationType.PayInvoice -> "Are you sure you want to pay this invoice?"
                        is ConfirmationType.TribeDeleteMember -> "Are you sure you want to remove ${confirmationType.alias?.value}?"
                        is ConfirmationType.ContactDelete -> "Are you sure you want to delete this contact?"
                    },
                    color = MaterialTheme.colorScheme.tertiary,
                    fontFamily = Roboto,
                    fontWeight = FontWeight.Light,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            isOpen = false
                            dashboardViewModel.toggleConfirmationWindow(false)
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.tertiary
                        ),
                        elevation = ButtonDefaults.elevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            when (confirmationType) {
                                is ConfirmationType.PayInvoice -> {
                                    confirmationType.message?.let { dashboardViewModel.payContactInvoice(it) }
                                }
                                is ConfirmationType.TribeDeleteMember -> {
                                    dashboardViewModel.kickMemberFromTribe(
                                        confirmationType.memberPubKey,
                                        confirmationType.alias,
                                        confirmationType.chatId
                                    )
                                    dashboardViewModel.toggleTribeMembersWindow(false, null)
                                }
                                is ConfirmationType.ContactDelete -> {
                                    dashboardViewModel.deleteSelectedContact()
                                }
                            }
                            isOpen = false
                            dashboardViewModel.toggleConfirmationWindow(false)
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = primary_blue,
                            contentColor = MaterialTheme.colorScheme.tertiary
                        ),
                        elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}