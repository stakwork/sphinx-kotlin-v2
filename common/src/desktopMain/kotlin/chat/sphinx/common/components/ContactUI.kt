package chat.sphinx.common.components

import CommonButton
import Roboto
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.components.notifications.DesktopSphinxToast
import chat.sphinx.common.state.ConfirmationType
import chat.sphinx.common.state.ContactScreenState
import chat.sphinx.common.viewmodel.contact.AddContactViewModel
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.common.viewmodel.contact.EditContactViewModel
import chat.sphinx.common.viewmodel.contact.InviteFriendViewModel
import chat.sphinx.response.LoadResponse
import chat.sphinx.response.Response
import chat.sphinx.utils.SphinxFonts
import chat.sphinx.wrapper.dashboard.ContactId
import chat.sphinx.wrapper.lightning.LightningNodeDescriptor
import theme.badge_red
import theme.light_divider
import theme.primary_red

@Composable
fun AddContactScreen(dashboardViewModel: DashboardViewModel, preferredSize: DpSize) {
    val screenState = dashboardViewModel.contactScreenStateFlow.value

    Box(
        modifier = Modifier
            .size(preferredSize)
            .background(MaterialTheme.colors.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopHeaderContainer(
                title = if (screenState is ContactScreenState.Choose) "New Contact" else "Contact",
                showBackButton = screenState is ContactScreenState.AlreadyOnSphinx || screenState is ContactScreenState.NewToSphinx,
                onClose = { dashboardViewModel.closeFullScreenView() },
                onBack = { dashboardViewModel.showFullScreenView(DashboardViewModel.FullScreenView.ContactScreen(ContactScreenState.Choose)) }
            ) {

            when (screenState) {
                is ContactScreenState.Choose -> AddContact(dashboardViewModel)
                is ContactScreenState.NewToSphinx -> AddNewContactOnSphinx(dashboardViewModel)
                is ContactScreenState.AlreadyOnSphinx -> ContactForm(dashboardViewModel, null, screenState.pubKey)
                else -> {}
            }

            DesktopSphinxToast("Add New Friend")
        }
        }
    }
}

@Composable
fun AddContact(dashboardViewModel: DashboardViewModel) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(color = androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(75.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CommonButton(
                callback = {
                    dashboardViewModel.showFullScreenView(DashboardViewModel.FullScreenView.ContactScreen(ContactScreenState.NewToSphinx))
                },
                text = "New to Sphinx",
                backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
                enabled = true
            )
            Divider(Modifier.padding(12.dp), color = Color.Transparent)
            CommonButton(
                callback = {
                    dashboardViewModel.showFullScreenView(DashboardViewModel.FullScreenView.ContactScreen(ContactScreenState.AlreadyOnSphinx()))
                },
                text = "Already on Sphinx",
                enabled = true
            )
        }
    }
}

@Composable
fun AddNewContactOnSphinx(dashboardViewModel: DashboardViewModel) {

    val viewModel = remember { InviteFriendViewModel(dashboardViewModel) }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(color = androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "NICKNAME",
                fontSize = 10.sp,
                fontFamily = SphinxFonts.montserratFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = viewModel.inviteFriendState.nickname,
                onValueChange = { viewModel.onNicknameChange(it) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = Roboto
                ),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = light_divider,
                    unfocusedBorderColor = light_divider,
                    backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "AMOUNT TO GIFT (SATS)",
                fontSize = 10.sp,
                fontFamily = SphinxFonts.montserratFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = viewModel.inviteFriendState.amount,
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) {
                        viewModel.onAmountChange(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = Roboto
                ),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = light_divider,
                    unfocusedBorderColor = light_divider,
                    backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "INCLUDE A MESSAGE",
                fontSize = 10.sp,
                fontFamily = SphinxFonts.montserratFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = viewModel.inviteFriendState.welcomeMessage,
                onValueChange = { viewModel.onWelcomeMessageChange(it) },
                modifier = Modifier.fillMaxWidth().height(108.dp),
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 16.sp
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = light_divider,
                    unfocusedBorderColor = light_divider,
                    backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                ),
                placeholder = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Welcome to Sphinx!",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    viewModel.inviteFriendState.nodePrice?.let { nodePrice ->
                        Text(
                            text = "ESTIMATED COST",
                            fontSize = 10.sp,
                            fontFamily = SphinxFonts.montserratFamily,
                            fontWeight = FontWeight.Normal,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        )
                        Box {
                            Row(modifier = Modifier.padding(top = 4.dp)) {
                                Text(
                                    text = nodePrice,
                                    fontSize = 20.sp,
                                    fontFamily = Roboto,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = "sat",
                                    fontSize = 20.sp,
                                    fontFamily = Roboto,
                                    fontWeight = FontWeight.Normal,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = {viewModel.createNewInvite()},
                    modifier = Modifier.clip(CircleShape)
                        .wrapContentWidth()
                        .height(50.dp),
                    enabled = viewModel.inviteFriendState.nickname.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
                        disabledBackgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer.copy(0.5f),
                    )
                )
                {
                    Text(
                        text = "Create Invitation",
                        color = if (viewModel.inviteFriendState.nickname.isNotEmpty()) {
                            Color.White
                        } else {
                            Color.White.copy(0.5f)
                        },
                        fontFamily = Roboto
                    )
                }
            }
            Column (
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            )
            {
                if (viewModel.inviteFriendState.createInviteStatus is LoadResponse.Loading) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
fun ContactForm(
    dashboardViewModel: DashboardViewModel,
    contactId: ContactId?,
    pubKey: LightningNodeDescriptor? = null
) {

    val editMode = (contactId != null)

    val viewModel = if (editMode) {
        remember { EditContactViewModel() }
    } else {
        remember { AddContactViewModel() }
    }

    (viewModel as? AddContactViewModel)?.fillPubKey(pubKey?.value)

    if ((viewModel as? EditContactViewModel)?.contactId != contactId) {
        (viewModel as? EditContactViewModel)?.loadContact(contactId)
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(color = androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            if (editMode) {
                Box(modifier = Modifier.align(Alignment.Start).wrapContentSize()) {
                    CommonButton(
                        text = "DELETE CONTACT",
                        enabled = true,
                        customColor = primary_red,
                        textButtonSize = 10.sp,
                        fontWeight = FontWeight.W500,
                        modifier = Modifier
                            .width(110.dp)
                            .height(42.dp),
                        callback = {
                            dashboardViewModel.toggleConfirmationWindow(open = true, ConfirmationType.ContactDelete)
                            dashboardViewModel.closeFullScreenView()
                        }
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth().height(112.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PhotoUrlImage(
                        photoUrl = viewModel.contactState.photoUrl,
                        modifier = Modifier.size(96.dp).clip(CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))

            Column {
                Text(
                    text = "Nickname*",
                    fontSize = 12.sp,
                    fontFamily = Roboto,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                )
                BasicTextField(
                    value = viewModel.contactState.contactAlias,
                    onValueChange = {
                        viewModel.onNicknameTextChanged(it)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    textStyle = TextStyle(fontSize = 18.sp, color = Color.White, fontFamily = Roboto),
                    singleLine = true,
                    cursorBrush = SolidColor(androidx.compose.material3.MaterialTheme.colorScheme.onBackground)

                )
                Divider(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Column {
                Text(
                    text = "Address*",
                    fontSize = 12.sp,
                    fontFamily = Roboto,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                )
                Row(
                    modifier = Modifier.height(32.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = viewModel.contactState.lightningNodePubKey,
                        onValueChange = {
                            viewModel.onAddressTextChanged(it)
                        },
                        enabled = !editMode,
                        modifier = Modifier.weight(1f).padding(top = 8.dp),
                        textStyle = TextStyle(fontSize = 18.sp, color = Color.White, fontFamily = Roboto),
                        singleLine = true,
                        cursorBrush = SolidColor(androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                    )
                    if(editMode) {
                        IconButton(onClick = {
                            dashboardViewModel.showFullScreenView(DashboardViewModel.FullScreenView.QRDetail( "PUBLIC KEY", viewModel.getNodeDescriptor() ?: ""))
                        }
                        ) {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = "",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                }
                Divider(modifier = Modifier.padding(top = 4.dp), color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Column {
                Text(
                    text = "Route Hint",
                    fontSize = 12.sp,
                    fontFamily = Roboto,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                )
                BasicTextField(
                    value = viewModel.contactState.lightningRouteHint ?: "",
                    onValueChange = {
                        viewModel.onRouteHintTextChanged(it)
                    },
                    enabled = !editMode,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    textStyle = TextStyle(fontSize = 18.sp, color = Color.White, fontFamily = Roboto),
                    singleLine = true,
                    cursorBrush = SolidColor(androidx.compose.material3.MaterialTheme.colorScheme.onBackground)

                )
                Divider(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(32.dp))

//            Column {
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Text(
//                        text = "Privacy Settings",
//                        fontSize = 12.sp,
//                        fontFamily = Roboto,
//                        color = Color.Gray,
//                    )
//                    Icon(
//                        Icons.Default.HelpOutline,
//                        contentDescription = "Privacy Settings",
//                        tint = Color.Gray,
//                        modifier = Modifier.size(20.dp).padding(start = 4.dp)
//                    )
//                }
//                Spacer(modifier = Modifier.height(4.dp))
//
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Text(
//                        text = "Standard PIN / Privacy PIN",
//                        fontSize = 18.sp,
//                        fontFamily = Roboto,
//                        color = Color.LightGray,
//                    )
//                    Switch(
//                        checked = switchState.value,
//                        onCheckedChange = { switchState.value = it },
//                        enabled = false
//                    )
//                }
//            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.fillMaxWidth().height(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.contactState.status is Response.Error) {
                        Text(
                            text = "There was an error, please try again later",
                            fontSize = 12.sp,
                            fontFamily = Roboto,
                            color = badge_red,
                        )
                    }
                    if (viewModel.contactState.status is LoadResponse.Loading) {
                        CircularProgressIndicator(
                            Modifier.padding(start = 8.dp).size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                CommonButton(
                    enabled = viewModel.contactState.saveButtonEnabled,
                    text = if (editMode) {
                        "SAVE"}
                    else {"SAVE TO CONTACTS"},
                    callback = {
                        viewModel.saveContact()
                    }
                )
            }
        }
    }

    if (viewModel.contactState.status is Response.Success) {
        dashboardViewModel.closeFullScreenView()
    }
}

