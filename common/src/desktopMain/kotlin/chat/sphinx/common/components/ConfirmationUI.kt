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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import chat.sphinx.utils.notifications.createSphinxNotificationManager
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
                is ConfirmationType.PodcastShare -> "Share Episode"
                is ConfirmationType.RemoveAccount -> "Logout"
            },
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = getPreferredWindowSize(360, 220)
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
                    modifier = Modifier.size(50.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = when (confirmationType) {
                        is ConfirmationType.PayInvoice -> "Are you sure you want to pay this invoice?"
                        is ConfirmationType.TribeDeleteMember -> "Are you sure you want to remove ${confirmationType.alias?.value}?"
                        is ConfirmationType.ContactDelete -> "Are you sure you want to delete this contact?"
                        is ConfirmationType.PodcastShare -> "Share from beginning or current time?"
                        is ConfirmationType.RemoveAccount -> "Are you sure you want to logout? All data and preferences will be deleted."
                    },
                    color = MaterialTheme.colorScheme.tertiary,

                    fontFamily = Roboto,
                    fontWeight = FontWeight.Light,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (confirmationType) {
                        is ConfirmationType.PayInvoice,
                        is ConfirmationType.TribeDeleteMember,
                        is ConfirmationType.ContactDelete,
                        is ConfirmationType.RemoveAccount -> {
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
                                            confirmationType.message?.let {
                                                dashboardViewModel.payContactInvoice(it)
                                            }
                                        }

                                        is ConfirmationType.TribeDeleteMember -> {
                                            dashboardViewModel.kickMemberFromTribe(
                                                confirmationType.memberPubKey,
                                                confirmationType.alias,
                                                confirmationType.chatId
                                            )
                                            dashboardViewModel.toggleTribeMembersSplitScreen(false, null)
                                        }

                                        is ConfirmationType.ContactDelete -> {
                                            dashboardViewModel.deleteSelectedContact()
                                        }

                                        is ConfirmationType.RemoveAccount -> {
                                            dashboardViewModel.removeAccountConfirmed()
                                        }

                                        else -> {}
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

                        is ConfirmationType.PodcastShare -> {
                            val clipboardManager = LocalClipboardManager.current

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(confirmationType.fromBeginningLink))
                                    isOpen = false
                                    dashboardViewModel.toggleConfirmationWindow(false)
                                    dashboardViewModel.showCopiedToClipboardToast()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = primary_blue,
                                    contentColor = MaterialTheme.colorScheme.tertiary
                                ),
                                elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "Share from Beginning",
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(confirmationType.fromCurrentTimeLink))
                                    isOpen = false
                                    dashboardViewModel.toggleConfirmationWindow(false)
                                    dashboardViewModel.showCopiedToClipboardToast()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colorScheme.onBackground,
                                    contentColor = MaterialTheme.colorScheme.tertiary
                                ),
                                elevation = ButtonDefaults.elevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "Share from Current Time",
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}