package chat.sphinx.common.components

import CommonButton
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
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
fun QRDetailScreen(dashboardViewModel: DashboardViewModel, viewModel: QRCodeViewModel, preferredSize: DpSize) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val isInvite = viewModel.contactQRCodeState.viewTitle.uppercase() == "INVITE CODE"

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
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isInvite) {
                        Box(
                            modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
                        ) {
                            CommonButton(
                                text = "DELETE INVITE",
                                enabled = true,
                                customColor = primary_red,
                                textButtonSize = 12.sp,
                                fontWeight = FontWeight.W500,
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(40.dp),
                                callback = {
                                    dashboardViewModel.deleteInvite(viewModel.contactQRCodeState.string)
                                    dashboardViewModel.closeFullScreenView()
                                }
                            )
                        }
                    }

                    Text(
                        text = viewModel.contactQRCodeState.viewTitle.uppercase(),
                        fontFamily = SphinxFonts.montserratFamily,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(24.dp))

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
                        val qrCodeSize = 200.dp // Fixed size to ensure proportion

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
        }
    }
}

@Composable
fun QRDetailProfileScreen(
    dashboardViewModel: DashboardViewModel,
    viewModel: QRCodeViewModel
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .zIndex(2f)
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(120.dp)
                    .padding(top = 24.dp)
                    .align(Alignment.BottomCenter)
            ) {
                PhotoUrlImage(
                    viewModel.contactQRCodeState.ownerPicture,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    firstNameLetter = "Unknown Chat".getInitials(),
                    color = null,
                    fontSize = 16
                )

            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopStart)
                .padding(top = 60.dp)
                .zIndex(1f)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .clickable { dashboardViewModel.closeFullScreenView() }
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(40.dp))

                Text(
                    text = viewModel.contactQRCodeState.ownerAlias ?: "USERNAME",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                Spacer(Modifier.height(16.dp))

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
                        Canvas(modifier = Modifier.size(qrCodeSize)) {
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

                Spacer(Modifier.height(16.dp))

                Text(
                    text = viewModel.contactQRCodeState.string,
                    fontSize = 12.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Button(
                    onClick = {
                        clipboardManager.setText(viewModel.contactQRCodeState.string.toAnnotatedString())
                        viewModel.toast("Code copied to clipboard")
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(0.6f).padding(top = 32.dp)
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
}



//@Composable
//fun QRDetailProfileScreen(
//    dashboardViewModel: DashboardViewModel,
//    viewModel: QRCodeViewModel,
//    preferredSize: DpSize
//) {
//    val clipboardManager: ClipboardManager = LocalClipboardManager.current
//
//    Box(
//        modifier = Modifier.fillMaxSize()
//    ) {
//        // Red Box (QR Content) - Lower zIndex so the blue box is above it
//        Box(
//            modifier = Modifier
//                .size(preferredSize)
//                .background(
//                    Color.Red,
//                    shape = RoundedCornerShape(16.dp)
//                )
//                .padding(top = 80.dp) // Ensures it's placed below the blue box
//                .align(Alignment.TopCenter)
//                .zIndex(1f) // Lower zIndex so blue box appears above it
//        ) {
//            Column(
//                modifier = Modifier.fillMaxSize(),
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center
//            ) {
//                Spacer(Modifier.height(40.dp))
//
//                Text(
//                    text = viewModel.contactQRCodeState.viewTitle,
//                    fontWeight = FontWeight.Bold,
//                    fontSize = 18.sp,
//                    color = Color.White
//                )
//
//                Spacer(Modifier.height(16.dp))
//
//                viewModel.contactQRCodeState.bitMatrix?.let { bitMatrix ->
//                    val qrCodeSize = 200.dp
//
//                    Box(
//                        modifier = Modifier
//                            .size(qrCodeSize)
//                            .clickable {
//                                clipboardManager.setText(viewModel.contactQRCodeState.string.toAnnotatedString())
//                                viewModel.toast("Code copied to clipboard")
//                            }
//                    ) {
//                        Canvas(modifier = Modifier.size(qrCodeSize)) {
//                            val scaleX = size.width / bitMatrix.width
//                            val scaleY = size.height / bitMatrix.height
//
//                            for (x in 0 until bitMatrix.width) {
//                                for (y in 0 until bitMatrix.height) {
//                                    drawRect(
//                                        brush = SolidColor(if (bitMatrix.get(x, y)) Color.Black else Color.White),
//                                        topLeft = Offset(x * scaleX, y * scaleY),
//                                        size = Size(scaleX, scaleY)
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//
//                Spacer(Modifier.height(16.dp))
//
//                Text(
//                    text = viewModel.contactQRCodeState.string,
//                    fontSize = 12.sp,
//                    color = Color.Gray,
//                    maxLines = 1,
//                    textAlign = TextAlign.Center,
//                    modifier = Modifier.padding(horizontal = 20.dp)
//                )
//
//                Spacer(Modifier.height(12.dp))
//
//                Button(
//                    onClick = {
//                        clipboardManager.setText(viewModel.contactQRCodeState.string.toAnnotatedString())
//                        viewModel.toast("Code copied to clipboard")
//                    },
//                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray),
//                    shape = RoundedCornerShape(8.dp),
//                    modifier = Modifier.fillMaxWidth(0.6f)
//                ) {
//                    Text(text = "Copy", color = Color.White)
//                }
//            }
//        }
//
//        // Blue Box (Title Bar) - Higher zIndex so it's above the red box
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(80.dp)
//                .background(Color.Blue)
//                .zIndex(2f), // Ensures it's above the red box
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                text = "Profile QR Code",
//                modifier = Modifier.padding(top = 64.dp),
//                fontWeight = FontWeight.Bold,
//                fontSize = 20.sp,
//                color = Color.White
//            )
//        }
//    }
//}
