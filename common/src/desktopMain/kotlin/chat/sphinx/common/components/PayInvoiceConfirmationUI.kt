package chat.sphinx.common.components

import Roboto
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
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
import chat.sphinx.platform.imageResource
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.utils.getPreferredWindowSize

@Composable
fun PayInvoiceConfirmationUI(dashboardViewModel: DashboardViewModel) {
    var isOpen by remember { mutableStateOf(true) }
    val payInvoiceWindowState by dashboardViewModel.payInvoiceConfirmationStateFlow.collectAsState()

    if (isOpen && payInvoiceWindowState.first) {
        payInvoiceWindowState.second?.let { message ->
            Window(
                onCloseRequest = {
                    dashboardViewModel.togglePayInvoiceConfirmationWindow(false)
                },
                title = "Confirm Payment",
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
                        contentDescription = "Invoice Logo",
                        modifier = Modifier.height(50.dp).width(50.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Are you sure you want to pay this invoice?",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontFamily = Roboto,
                        fontWeight = FontWeight.Light,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center, // Center the buttons horizontally
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                isOpen = false
                                dashboardViewModel.togglePayInvoiceConfirmationWindow(false)
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                dashboardViewModel.payContactInvoice(message)
                                isOpen = false
                                dashboardViewModel.togglePayInvoiceConfirmationWindow(false)
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}
