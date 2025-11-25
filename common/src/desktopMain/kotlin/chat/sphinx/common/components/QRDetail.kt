package chat.sphinx.common.components

import CommonButton
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.common.viewmodel.contact.QRCodeViewModel
import chat.sphinx.utils.SphinxFonts
import chat.sphinx.utils.toAnnotatedString
import chat.sphinx.wrapper.util.getInitials
import theme.primary_red

@Composable
fun QRDetailScreen(
    dashboardViewModel: DashboardViewModel,
    viewModel: QRCodeViewModel,
    preferredSize: DpSize
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val isInvite = viewModel.contactQRCodeState.viewTitle.uppercase() == "INVITE CODE"

    var showDeleteInviteDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(preferredSize)
            .background(MaterialTheme.colors.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopHeaderContainer(
                title = "QR Code",
                onClose = { dashboardViewModel.closeFullScreenView() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(Modifier.height(24.dp))

                    // TITLE
                    Text(
                        text = viewModel.contactQRCodeState.viewTitle.uppercase(),
                        fontFamily = SphinxFonts.montserratFamily,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(24.dp))

                    // HINT ROW
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = "QR Code",
                            tint = Color.Gray,
                            modifier = Modifier.size(30.dp)
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = "CLICK TO COPY",
                            fontFamily = SphinxFonts.montserratFamily,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // QR CODE
                    viewModel.contactQRCodeState.bitMatrix?.let { bitMatrix ->
                        val qrCodeSize = 200.dp

                        Box(
                            modifier = Modifier
                                .size(qrCodeSize)
                                .clickable {
                                    clipboardManager.setText(
                                        viewModel.contactQRCodeState.string.toAnnotatedString()
                                    )
                                    viewModel.toast("Code copied to clipboard")
                                }
                        ) {
                            Canvas(
                                modifier = Modifier.size(qrCodeSize)
                            ) {
                                val scaleX = size.width / bitMatrix.width
                                val scaleY = size.height / bitMatrix.height

                                for (x in 0 until bitMatrix.width) {
                                    for (y in 0 until bitMatrix.height) {
                                        drawRect(
                                            brush = SolidColor(
                                                if (bitMatrix.get(x, y)) Color.Black else Color.White
                                            ),
                                            topLeft = Offset(x * scaleX, y * scaleY),
                                            size = Size(scaleX, scaleY)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // CODE TEXT - Improved wrapping and centering
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        text = viewModel.contactQRCodeState.string,
                        fontFamily = SphinxFonts.montserratFamily,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp,
                        overflow = TextOverflow.Visible
                    )

                    // DELETE INVITE SECTION
                    if (isInvite) {
                        Spacer(Modifier.height(32.dp))

                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White.copy(alpha = 0.08f)
                        )

                        Spacer(Modifier.height(24.dp))

                        // Delete button styled like CommonMenuButton
                        Button(
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = primary_red),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            onClick = { showDeleteInviteDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete invite",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(20.dp)
                                )
                                Text(
                                    text = "Delete invite",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.W500,
                                    fontFamily = SphinxFonts.montserratFamily,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }

        // CONFIRMATION DIALOG
        if (showDeleteInviteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteInviteDialog = false },
                title = {
                    Text(
                        text = "Delete this invite?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "This action cannot be undone. People with this invite code will no longer be able to use it."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dashboardViewModel.deleteInvite(viewModel.contactQRCodeState.string)
                            showDeleteInviteDialog = false
                            dashboardViewModel.closeFullScreenView()
                        }
                    ) {
                        Text(
                            text = "Delete",
                            color = primary_red,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteInviteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun QRDetailProfileScreen(
    dashboardViewModel: DashboardViewModel,
    viewModel: QRCodeViewModel
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val qrCodeSize = 200.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        // Close Icon (Top-right)
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(16.dp)
                .size(24.dp)
                .align(Alignment.TopEnd)
                .clickable { dashboardViewModel.closeFullScreenView() }
        )

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Profile picture
            PhotoUrlImage(
                photoUrl = viewModel.contactQRCodeState.ownerPicture,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
                firstNameLetter = "Unknown Chat".getInitials(),
                color = null,
                fontSize = 16
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Username
            Text(
                text = viewModel.contactQRCodeState.ownerAlias ?: "USERNAME",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            // QR Code
            viewModel.contactQRCodeState.bitMatrix?.let { bitMatrix ->
                Box(
                    modifier = Modifier
                        .size(qrCodeSize)
                        .clickable {
                            clipboardManager.setText(viewModel.contactQRCodeState.string.toAnnotatedString())
                            viewModel.toast("Code copied to clipboard")
                        }
                ) {
                    Canvas(modifier = Modifier.size(qrCodeSize)) {
                        val scaleX = size.width / bitMatrix.width
                        val scaleY = size.height / bitMatrix.height

                        for (x in 0 until bitMatrix.width) {
                            for (y in 0 until bitMatrix.height) {
                                drawRect(
                                    color = if (bitMatrix.get(x, y)) Color.Black else Color.White,
                                    topLeft = Offset(x * scaleX, y * scaleY),
                                    size = Size(scaleX, scaleY)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // QR Code Text (with horizontal scroll for long strings)
            Box(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = viewModel.contactQRCodeState.string,
                    fontSize = 12.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Copy Button
            Button(
                onClick = {
                    clipboardManager.setText(viewModel.contactQRCodeState.string.toAnnotatedString())
                    viewModel.toast("Code copied to clipboard")
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Copy", color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
fun QRDetailSplitScreen(
    dashboardViewModel: DashboardViewModel,
    viewModel: QRCodeViewModel
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(end = 24.dp)
        ) {
            Icon(
                Icons.Default.TouchApp,
                contentDescription = "QR Code",
                tint = Color.Gray,
                modifier = Modifier.size(30.dp)
            )

            Text(
                text = "CLICK TO COPY",
                fontFamily = SphinxFonts.montserratFamily,
                color = Color.Gray,
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.height(18.dp))

        viewModel.contactQRCodeState.bitMatrix?.let { bitMatrix ->
            val qrCodeSize = 200.dp

            Box(
                modifier = Modifier
                    .size(qrCodeSize)
                    .clickable {
                        clipboardManager.setText(viewModel.contactQRCodeState.string.toAnnotatedString())
                        viewModel.toast("Code copied to clipboard")
                    }
            ) {
                Canvas(modifier = Modifier.size(qrCodeSize).clickable {
                    clipboardManager.setText(viewModel.contactQRCodeState.string.toAnnotatedString())
                    viewModel.toast("Code copied to clipboard")
                }) {
                    val scaleX = size.width / bitMatrix.width
                    val scaleY = size.height / bitMatrix.height

                    for (x in 0 until bitMatrix.width) {
                        for (y in 0 until bitMatrix.height) {
                            drawRect(
                                brush = SolidColor(if (bitMatrix.get(x, y)) Color.Black else Color.White),
                                topLeft = Offset(x * scaleX, y * scaleY),
                                size = Size(scaleX, scaleY)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            text = viewModel.contactQRCodeState.string,
            fontFamily = SphinxFonts.montserratFamily,
            color = Color.Gray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}
