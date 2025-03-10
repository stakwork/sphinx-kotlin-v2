package chat.sphinx.common.components


import Roboto
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.Indicator
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import chat.sphinx.common.components.tribe.JoinTribeScreen
import chat.sphinx.common.state.AuthorizeViewState
import chat.sphinx.common.state.ContactScreenState
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.common.viewmodel.WebAppViewModel
import chat.sphinx.common.viewmodel.dashboard.ChatListViewModel
import chat.sphinx.concepts.repository.connect_manager.model.NetworkStatus
import chat.sphinx.utils.SphinxFonts
import chat.sphinx.wrapper.lightning.asFormattedString
import theme.*

@Composable
fun DashboardSidebarUI(
    dashboardViewModel: DashboardViewModel,
    webAppViewModel: WebAppViewModel,
) {
    val chatListViewModel = remember { ChatListViewModel() }
    val uriHandler = LocalUriHandler.current
    var selectedTabIndex = dashboardViewModel.selectedTabStateFlow.collectAsState().value
    var isMenuExpanded by remember { mutableStateOf(false) }
    val unseenTribeMessagesCount by dashboardViewModel.unseenTribeMessagesCount.collectAsState()

    Box(
        Modifier
            .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
            TopAppBar(
                backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                elevation = 0.dp,
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PhotoUrlImage(
                        photoUrl = dashboardViewModel.accountOwnerStateFlow.collectAsState().value?.photoUrl,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            androidx.compose.material3.Text(
                                text = dashboardViewModel.accountOwnerStateFlow.collectAsState().value?.alias?.value ?: "User",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.W600,
                                fontFamily = SphinxFonts.montserratFamily,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            val networkState by dashboardViewModel.networkStatusStateFlow.collectAsState()
                            if (networkState is NetworkStatus.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.FlashOn,
                                    contentDescription = "Connection Status",
                                    tint = if (networkState is NetworkStatus.Connected) {
                                        primary_green
                                    } else {
                                        primary_red
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            val balance by dashboardViewModel.balanceStateFlow.collectAsState()
                            androidx.compose.material3.Text(
                                text = balance?.balance?.asFormattedString(' ') ?: "0",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontFamily = SphinxFonts.montserratFamily,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            androidx.compose.material3.Text(
                                "sat",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = SphinxFonts.montserratFamily,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        modifier = Modifier.size(24.dp),
                        onClick = {dashboardViewModel.triggerOwnerQRCode()}
                    ) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = "QR Code",
                            tint = place_holder_text
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        modifier = Modifier.size(24.dp),
                        onClick = {
                            dashboardViewModel.triggerNetworkRefresh()
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = place_holder_text
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        modifier = Modifier.size(24.dp),
                        onClick = {
                            isMenuExpanded = !isMenuExpanded
                        }
                    ) {
                        Icon(
                            if (isMenuExpanded) Icons.Default.Close else Icons.Default.Menu,
                            contentDescription = if (isMenuExpanded) "Close Menu" else "Open Menu",
                            tint = place_holder_text
                        )
                    }
                }
            }

            TopAppBar(
                backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                title = {
                    CustomTextField(
                        leadingIcon = {
                            Spacer(modifier = Modifier.width(8.dp))
                        },
                        trailingIcon = {
                            if (chatListViewModel.searchText.value?.text?.isNotEmpty() == true) {
                                Icon(
                                    Icons.Filled.Cancel,
                                    contentDescription = null,
                                    tint = place_holder_text,
                                    modifier = Modifier
                                        .width(28.dp)
                                        .clickable {
                                            chatListViewModel.filterChats(TextFieldValue(""))
                                        }
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    modifier = Modifier.width(28.dp),
                                    tint = place_holder_text
                                )
                            }
                        },
                        modifier = Modifier
                            .background(
                                androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                RoundedCornerShape(percent = 50)
                            )
                            .padding(4.dp)
                            .height(30.dp),
                        fontSize = 14.sp,
                        placeholderText = "Search",
                        onValueChange = { input ->
                            chatListViewModel.filterChats(input)
                        },
                        value = chatListViewModel.searchText.value ?: TextFieldValue("")
                    )
                },
                elevation = 4.dp,
                actions = {
                    IconButton(onClick = {
                    }) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Hide",
                            tint = place_holder_text,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            )


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.fillMaxWidth(0.6f)) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        val tabs = listOf("Friends", "Tribes")
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { dashboardViewModel.setSelectedTab(index)},
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        color = if (selectedTabIndex == index) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            )
                        }
                    }

                    if ((unseenTribeMessagesCount ?: 0L) > 0L) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 4.dp)
                                .background(color = primary_blue, shape = CircleShape)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterVertically),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        onClick = {
                            dashboardViewModel.showFullScreenView(DashboardViewModel.FullScreenView.ContactScreen(ContactScreenState.Choose))
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = place_holder_text
                        )
                    }
                }
            }

            // Display content based on the selected tab
            when (selectedTabIndex) {
                0 -> ChatListUI(chatListViewModel, dashboardViewModel, false)
                1 -> ChatListUI(chatListViewModel, dashboardViewModel, true)
            }

            AboutSphinxWindow(dashboardViewModel)
            WebAppWindow(dashboardViewModel, webAppViewModel)
            AuthorizeWindow(webAppViewModel)
            ConfirmationWindow(dashboardViewModel)
        }

        if (isMenuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight()
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
                    .background(
                        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
                    .zIndex(2f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Profile Option
                    MenuItem(icon = Icons.Default.Person, title = "Profile", clickAction = {
                        dashboardViewModel.showFullScreenView(DashboardViewModel.FullScreenView.Profile)
                        isMenuExpanded = !isMenuExpanded
                    })
                    // Transactions Option
                    MenuItem(icon = Icons.Default.Subject, title = "Transactions", clickAction = {
                        dashboardViewModel.showFullScreenView(DashboardViewModel.FullScreenView.Transactions)
                        isMenuExpanded = !isMenuExpanded
                    })
                    // Request Payment Option
                    MenuItem(icon = Icons.Default.NorthEast, title = "Request Payment", clickAction = {
                        dashboardViewModel.showFullScreenView(DashboardViewModel.FullScreenView.CreateInvoice)
                        isMenuExpanded = !isMenuExpanded
                    })
                    // Pay Invoice Option
                    MenuItem(icon = Icons.Default.SouthWest, title = "Pay Invoice", clickAction = {
                        dashboardViewModel.showFullScreenView(DashboardViewModel.FullScreenView.PayInvoice)
                        isMenuExpanded = !isMenuExpanded
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                    // Connect with Friend
                    CommonMenuButton(
                        text = "Connect with Friend",
                        customColor = MaterialTheme.colorScheme.tertiary,
                        iconColor = Color.Black,
                        startIcon = Icons.Default.QrCode,
                        textColor = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp) // Make the button smaller
                            .padding(vertical = 4.dp),
                        callback = {
                            dashboardViewModel.triggerOwnerQRCode()
                            isMenuExpanded = !isMenuExpanded
                        }
                    )
                    // Add Friend Button
                    CommonMenuButton(
                        text = "Add Friend",
                        customColor = primary_green,
                        startIcon = Icons.Default.Add,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(vertical = 4.dp),
                        textColor = MaterialTheme.colorScheme.tertiary,
                        callback = {
                            dashboardViewModel.showFullScreenView(DashboardViewModel.FullScreenView.ContactScreen(ContactScreenState.Choose))
                            isMenuExpanded = !isMenuExpanded
                        }
                    )
                    // Create Tribe Button
                    CommonMenuButton(
                        text = "Create Tribe",
                        customColor = primary_blue,
                        startIcon = Icons.Default.Add,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(vertical = 4.dp),
                        textColor = MaterialTheme.colorScheme.tertiary,
                        callback = {
                            dashboardViewModel.showFullScreenView(DashboardViewModel.FullScreenView.CreateTribeScreen(null))
                            isMenuExpanded = !isMenuExpanded
                        }

                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Version 1.0.3",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "© 2025 Stakwork All rights reserved",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = place_holder_text,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
fun MenuItem(
    icon: ImageVector,
    title: String,
    clickAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                clickAction?.invoke()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = place_holder_text
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CommonMenuButton(
    text: String,
    enabled: Boolean? = true,
    customColor: Color? = null,
    startIcon: ImageVector? = null,
    iconColor: Color = Color.White,
    textButtonSize: TextUnit = 12.sp,
    textColor: Color,
    fontWeight: FontWeight = FontWeight.W500,
    centerContent: Boolean = false,
    modifier: Modifier = Modifier,
    callback: () -> Unit
) {
    val color = if (enabled == true) {
        textColor
    } else {
        textColor.copy(alpha = 0.7f)
    }

    Button(
        shape = RoundedCornerShape(23.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = customColor ?: color),
        modifier = modifier,
        onClick = {
            if (enabled == true) callback()
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (centerContent) Arrangement.Center else Arrangement.Start, // Adjust arrangement
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (startIcon != null) {
                Icon(
                    imageVector = startIcon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .padding(end = if (centerContent) 0.dp else 8.dp)
                        .size(20.dp)
                )
            }
            if (centerContent) {
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = textButtonSize,
                color = textColor,
                fontWeight = fontWeight,
                fontFamily = Roboto,
                textAlign = if (centerContent) TextAlign.Center else TextAlign.Start, // Adjust alignment
                modifier = if (centerContent) Modifier else Modifier.weight(1f) // Preserve old behavior
            )
        }
    }
}
@Composable
fun TribesListUI(
    dashboardViewModel: DashboardViewModel,
) {
    // TODO V2 Implement tribe list
    Text(
        text = "Tribes content goes here",
        modifier = Modifier.padding(16.dp),
    )
}

@Composable
fun AboutSphinxWindow(
    dashboardViewModel: DashboardViewModel
) {
    val aboutSphinxWindowState by dashboardViewModel.aboutSphinxStateFlow.collectAsState()
    if (aboutSphinxWindowState) {
        AboutSphinx(dashboardViewModel)
    }
}

@Composable
fun ConfirmationWindow(
    dashboardViewModel: DashboardViewModel
) {
    val confirmationState by dashboardViewModel.confirmationStateFlow.collectAsState()
    if (confirmationState.first) {
        confirmationState.second?.let { confirmationType ->
            ConfirmationUI(dashboardViewModel, confirmationType)
        }
    }
}

@Composable
fun WebAppWindow(
    dashboardViewModel: DashboardViewModel,
    webAppViewModel: WebAppViewModel
) {
    val webAppWindowState by webAppViewModel.webAppWindowStateFlow.collectAsState()
    if (webAppWindowState) {
        WebAppUI(dashboardViewModel, webAppViewModel)
    }
}

@Composable
fun AuthorizeWindow(
    webAppViewModel: WebAppViewModel
) {
    val authorizeView by webAppViewModel.authorizeViewStateFlow.collectAsState()
    (authorizeView as? AuthorizeViewState.Opened)?.let {
        AuthorizeViewUI(webAppViewModel, it.budgetField)
    }
}