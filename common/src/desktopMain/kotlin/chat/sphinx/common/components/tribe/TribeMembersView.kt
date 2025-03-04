package chat.sphinx.common.components.tribe

import Roboto
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import chat.sphinx.common.components.PhotoUrlImage
import chat.sphinx.common.state.ConfirmationType
import chat.sphinx.common.viewmodel.DashboardViewModel
import chat.sphinx.common.viewmodel.chat.TribeMembersViewModel
import chat.sphinx.utils.getPreferredWindowSize
import chat.sphinx.wrapper.PhotoUrl
import chat.sphinx.wrapper.lightning.toLightningNodePubKey
import chat.sphinx.wrapper.message.MessageType
import chat.sphinx.wrapper.message.SenderAlias
import chat.sphinx.wrapper.message.toSenderAlias
import chat.sphinx.wrapper.mqtt.TribeMember
import theme.badge_red
import theme.light_divider
import theme.primary_green


@Composable
fun TribeMembersView(
    tribeMembersViewModel: TribeMembersViewModel,
    dashboardViewModel: DashboardViewModel
) {
    val tribeMembersListState = rememberLazyListState()
    val pendingTribeMembersListState = rememberLazyListState()
    val viewState = tribeMembersViewModel.tribeMembersViewState

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (viewState.tribeMembersList.isEmpty() && viewState.pendingTribeMembersList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                if (viewState.pendingTribeMembersList.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PENDING TRIBE MEMBERS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W500,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${viewState.pendingTribeMembersList.size}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W500,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    LazyColumn(
                        state = pendingTribeMembersListState,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(viewState.pendingTribeMembersList.size) { index ->
                            viewState.pendingTribeMembersList[index].let { pendingMember ->
                                PendingMemberRow(pendingMember, tribeMembersViewModel)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRIBE MEMBERS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W500,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${viewState.tribeMembersList.size}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W500,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                LazyColumn(
                    state = tribeMembersListState,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(viewState.tribeMembersList.size) { index ->
                        viewState.tribeMembersList[index].let { tribeMember ->
                            TribeMemberRow(tribeMember, dashboardViewModel, tribeMembersViewModel)
                        }
                    }
                    if (viewState.loadingMore) {
                        item { LoadingRow() }
                    }
                }
            }
        }
    }
}

@Composable
fun TribeMemberRow(
    tribeMember: TribeMember,
    dashboardViewModel: DashboardViewModel,
    tribeMembersViewModel: TribeMembersViewModel
) {
    val dividerColor = light_divider
    val ownerPubKey = tribeMembersViewModel.ownerState

    val isOwner = tribeMember.pubkey == ownerPubKey

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp, end = 16.dp)
        ) {
            val initial = (tribeMember.alias ?: "Unknown").getInitial()

            Text(
                text = initial,
                fontSize = 14.sp,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
            )

            PhotoUrlImage(
                photoUrl = tribeMember.photo_url?.let { PhotoUrl(it) },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                firstNameLetter = initial,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                val displayName = if (isOwner) {
                    "${tribeMember.alias ?: "Unknown Member"} (You)"
                } else {
                    tribeMember.alias ?: "Unknown Member"
                }

                Text(
                    text = displayName,
                    maxLines = 1,
                    fontSize = 14.sp,
                    fontFamily = Roboto,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            if (!isOwner) {
                IconButton(
                    onClick = {
                        dashboardViewModel.toggleConfirmationWindow(true, tribeMember.pubkey?.toLightningNodePubKey()?.let {
                            ConfirmationType.TribeDeleteMember(
                                alias = tribeMember.alias?.toSenderAlias(),
                                memberPubKey = it,
                                chatId = tribeMembersViewModel.chatId
                            )
                        })
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Remove Member",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
        Divider(color = dividerColor, modifier = Modifier.padding(start = 96.dp, end = 16.dp))
    }
}

@Composable
fun PendingMemberRow(tribeMember: TribeMember, tribeMembersViewModel: TribeMembersViewModel) {
    val dividerColor = light_divider
    val buttonsEnabled = remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp, end = 16.dp)
        ) {
            val initial = (tribeMember.alias ?: "Unknown").getInitial()

            Text(
                text = initial,
                fontSize = 14.sp,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
            )

            PhotoUrlImage(
                photoUrl = tribeMember.photo_url?.let { PhotoUrl(it) },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                firstNameLetter = initial,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tribeMember.alias ?: "Unknown Member",
                    maxLines = 1,
                    fontSize = 14.sp,
                    fontFamily = Roboto,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            IconButton(
                onClick = {
                    buttonsEnabled.value = false
                    tribeMembersViewModel.processMemberRequest(
                        SenderAlias(tribeMember.alias ?: "Unknown"),
                        MessageType.GroupAction.MemberApprove
                    )
                    tribeMembersViewModel.removePendingMember(tribeMember.pubkey ?: "")
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(CircleShape)
                    .background(primary_green)
                    .size(24.dp),
                enabled = buttonsEnabled.value
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Accept",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(4.dp)
                )
            }
            IconButton(
                onClick = {
                    buttonsEnabled.value = false
                    tribeMembersViewModel.processMemberRequest(
                        SenderAlias(tribeMember.alias ?: "Unknown"),
                        MessageType.GroupAction.MemberReject
                    )
                    tribeMembersViewModel.removePendingMember(tribeMember.pubkey ?: "")
                },
                modifier = Modifier
                    .padding(start = 6.dp)
                    .clip(CircleShape)
                    .background(badge_red)
                    .size(24.dp),
                enabled = buttonsEnabled.value
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Decline",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
        Divider(color = dividerColor, modifier = Modifier.padding(start = 96.dp, end = 16.dp))
    }
}


@Composable
fun LoadingRow() {
    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        androidx.compose.material.CircularProgressIndicator(
            Modifier.size(25.dp),
            color = MaterialTheme.colorScheme.onBackground,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Loading more...",
            fontSize = 15.sp,
            maxLines = 1,
            fontFamily = Roboto,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

fun LazyListState.isScrolledToEnd(): Boolean {
    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull() ?: return false
    return lastVisibleItem.index == layoutInfo.totalItemsCount - 1
}

fun String.getInitial(): String =
    split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(1).joinToString("")
