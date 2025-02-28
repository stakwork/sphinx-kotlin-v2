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
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.sp
import chat.sphinx.common.components.tribe.TribeTextField
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.utils.getPreferredWindowSize
import theme.light_divider
import theme.place_holder_text
import theme.primary_blue

@Composable
fun PayInvoiceScreen(dashboardViewModel: DashboardViewModel, preferredSize: DpSize) {
    Box(
        modifier = Modifier
            .size(preferredSize)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopHeaderContainer(
                title = "Pay Invoice",
                onClose = { dashboardViewModel.closeFullScreenView() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val payInvoiceInfo by dashboardViewModel.payInvoiceInfoStateFlow.collectAsState()

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
fun CreateInvoiceScreen(dashboardViewModel: DashboardViewModel, preferredSize: DpSize) {
    Box(
        modifier = Modifier
            .size(preferredSize)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopHeaderContainer(
                title = "Create Invoice",
                onClose = { dashboardViewModel.closeFullScreenView() }
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
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Spacer(modifier = Modifier.width(66.dp))
                                androidx.compose.material.OutlinedTextField(
                                    value = dashboardViewModel.createInvoiceState.amount,
                                    onValueChange = {
                                        if (it.all { char -> char.isDigit() } && it.length <= 7) {
                                            dashboardViewModel.onInvoiceAmountChange(it)
                                        }
                                    },
                                    modifier = Modifier.width(150.dp),
                                    textStyle = TextStyle(
                                        textAlign = TextAlign.Center,
                                        color = Color.White,
                                        fontSize = 50.sp,
                                        fontFamily = Roboto
                                    ),
                                    placeholder = {
                                        Text(
                                            "0",
                                            modifier = Modifier.fillMaxWidth(),
                                            color = place_holder_text,
                                            fontFamily = Roboto,
                                            fontSize = 50.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    singleLine = true,
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = primary_blue
                                    )
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(
                                    modifier = Modifier.width(50.dp)
                                ) {
                                    Text(
                                        "sat",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontFamily = Roboto,
                                        fontWeight = FontWeight.Light,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(40.dp).fillMaxWidth())
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 40.dp)
                            ) {
                                androidx.compose.material.OutlinedTextField(
                                    value = dashboardViewModel.createInvoiceState.memo,
                                    onValueChange = {
                                        dashboardViewModel.onInvoiceMemoChange(it)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(
                                        textAlign = TextAlign.Center,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontFamily = Roboto
                                    ),
                                    placeholder = {
                                        Text(
                                            "Memo",
                                            modifier = Modifier.fillMaxWidth(),
                                            color = place_holder_text,
                                            fontFamily = Roboto,
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    singleLine = true,
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = primary_blue
                                    )
                                )
                                androidx.compose.material.Divider(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = light_divider
                                )
                            }
                            Spacer(modifier = Modifier.height(80.dp).fillMaxWidth())
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp)
                    ) {
                        CommonButton(
                            callback = {
                                dashboardViewModel.requestPayment()
                            },
                            text = "Confirm",
                            backgroundColor = primary_blue,
                            enabled = true
                        )
                    }
                }
            }
        }
    }
}
