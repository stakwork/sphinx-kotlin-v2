package chat.sphinx.common.components

import CommonButton
import Roboto
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import chat.sphinx.common.components.tribe.TribeTextField
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.utils.getPreferredWindowSize
import theme.primary_blue

@Composable
fun PayInvoiceWindowUI(dashboardViewModel: DashboardViewModel) {
    val isOpen by dashboardViewModel.payInvoiceWindowStateFlow.collectAsState()
    if (isOpen) {
        Window(
            onCloseRequest = {
                dashboardViewModel.togglePayInvoiceWindow(false)
            },
            title = "Pay Invoice",
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = getPreferredWindowSize(420, 520)
            )
        ) {
            PayInvoiceContent(dashboardViewModel)
        }
    }
}

@Composable
fun CreateInvoiceWindowUI(dashboardViewModel: DashboardViewModel) {
    val isOpen by dashboardViewModel.createInvoiceWindowStateFlow.collectAsState()
    if (isOpen) {
        Window(
            onCloseRequest = {
                dashboardViewModel.toggleCreateInvoiceWindow(false)
            },
            title = "Create Invoice",
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = getPreferredWindowSize(420, 520)
            )
        ) {
            CreateInvoiceContent(dashboardViewModel)
        }
    }
}

@Composable
fun PayInvoiceContent(dashboardViewModel: DashboardViewModel) {
    val payInvoiceInfo by dashboardViewModel.payInvoiceInfoStateFlow.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = androidx.compose.material3.MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        // Top Right Back Icon
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            if (dashboardViewModel.payInvoiceInfoStateFlow.value.amount != null) {
                IconButton(
                    onClick = {
                        dashboardViewModel.clearInvoice()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            TribeTextField(
                label = "Invoice String",
                value = payInvoiceInfo.invoiceString ?: "",
                onTextChange = { dashboardViewModel.setInvoiceString(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .heightIn(min = 80.dp)
            ) {
                if (payInvoiceInfo.amount != null) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Invoice Amount: ${payInvoiceInfo.amount} Sats",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontFamily = Roboto
                        )
                        Text(
                            text = "Expiration Date: ${payInvoiceInfo.expirationDate}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontFamily = Roboto
                        )
                        Text(
                            text = "Memo: ${payInvoiceInfo.memo ?: "None"}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontFamily = Roboto
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
            ) {
                CommonButton(
                    callback = {
                        if (payInvoiceInfo.amount != null) {
                            dashboardViewModel.processInvoicePayment()
                        } else {
                            dashboardViewModel.verifyInvoice()
                        }
                    },
                    text = if (payInvoiceInfo.amount != null) "Pay" else "Verify",
                    backgroundColor = primary_blue,
                    enabled = true
                )
            }
        }
    }
}
@Composable
fun CreateInvoiceContent(dashboardViewModel: DashboardViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(start = 32.dp)
                ) {
                    TextField(
                        value = dashboardViewModel.createInvoiceState.amount,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 7) {
                                dashboardViewModel.onInvoiceAmountChange(it)
                            }
                        },
                        modifier = Modifier
                            .width(180.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally),
                        singleLine = true,
                        maxLines = 1,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.W400,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            cursorColor = MaterialTheme.colorScheme.onBackground
                        ),
                        placeholder = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "0",
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.W400,
                                        textAlign = TextAlign.Center,
                                        fontFamily = Roboto
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "sat",
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 24.sp,
                            fontFamily = Roboto,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }

            Spacer(Modifier.height(42.dp))

            TribeTextField(
                label = "Memo",
                value = dashboardViewModel.createInvoiceState.memo,
                onTextChange = { dashboardViewModel.onInvoiceMemoChange(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(84.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
            ) {
                CommonButton(
                    callback = {
                        dashboardViewModel.requestPayment()
                        dashboardViewModel.toggleCreateInvoiceWindow(false)
                    },
                    text = "Confirm",
                    backgroundColor = primary_blue,
                    enabled = true
                )
            }
        }
    }
}
