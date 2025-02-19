package chat.sphinx.common.chatMesssageUI

import Roboto
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.models.ChatMessage
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.di.container.SphinxContainer
import chat.sphinx.wrapper.message.MessageType
import kotlinx.coroutines.launch
import theme.badge_red
import theme.darker_gray
import theme.light_divider
import theme.primary_green
import java.awt.Robot

@Composable
fun GroupActionsUI(
    chatMessage: ChatMessage,
    chatViewModel: ChatViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val requestType = chatMessage.message.type

        if (
            chatMessage.isAdmin && requestType is MessageType.GroupAction.MemberRequest ||
            chatMessage.isAdmin && requestType is MessageType.GroupAction.MemberApprove ||
            chatMessage.isAdmin && requestType is MessageType.GroupAction.MemberReject
        ) {
            MemberRequest(chatMessage, chatViewModel, requestType)
        } else if (
            requestType is MessageType.GroupAction.MemberReject ||
            requestType is MessageType.GroupAction.Kick ||
            requestType is MessageType.GroupAction.TribeDelete
        ) {
            val isKickedMember = chatMessage.message.sender.value != 0L
            if (requestType is MessageType.GroupAction.Kick && !isKickedMember) return

            KickDeclinedOrTribeDeleted(chatMessage.message.senderAlias?.value ?: "", requestType, chatViewModel)
        } else {
            GroupActionAnnouncement(chatMessage)
        }
    }
}

@Composable
fun GroupActionAnnouncement(chatMessage: ChatMessage) {
    chatMessage.groupActionLabelText?.let { groupActionLabelText ->
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                backgroundColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, light_divider)
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    text = groupActionLabelText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.W300,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun MemberRequest(chatMessage: ChatMessage, viewModel: ChatViewModel, requestType: MessageType) {
    val scope = SphinxContainer.appModule.applicationScope
    val dispatchers = SphinxContainer.appModule.dispatchers

    val subjectName = chatMessage.message.senderAlias?.value ?: ""

    val requestText = when (requestType) {
        is MessageType.GroupAction.MemberRequest -> {
            "$subjectName wants to\njoin the tribe"
        }
        is MessageType.GroupAction.MemberApprove -> {
            "You have approved\nthe request from $subjectName"
        }
        is MessageType.GroupAction.MemberReject -> {
            "You have declined\nthe request from $subjectName"
        }
        else -> {
            ""
        }
    }

    val buttonsEnabled = requestType == MessageType.GroupAction.MemberRequest
    val isLoading = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Card(
            backgroundColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(9.dp),
            border = BorderStroke(1.dp, light_divider)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = requestText,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.tertiary
                )
                if (isLoading.value) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .background(color = MaterialTheme.colorScheme.onSecondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.tertiary,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            scope.launch(dispatchers.mainImmediate) {
                                isLoading.value = true
                                chatMessage.message.uuid?.let { messageUuid ->
                                    viewModel.processMemberRequest(
                                        chatMessage.message.chatId,
                                        messageUuid,
                                        MessageType.GroupAction.MemberApprove,
                                        chatMessage.message.senderAlias
                                    )
                                }
                                isLoading.value = false
                            }
                        },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(CircleShape)
                            .background(primary_green)
                            .size(24.dp),
                        enabled = buttonsEnabled
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
                            scope.launch(dispatchers.mainImmediate) {
                                isLoading.value = true
                                chatMessage.message.uuid?.let { messageUuid ->
                                    viewModel.processMemberRequest(
                                        chatMessage.message.chatId,
                                        messageUuid,
                                        MessageType.GroupAction.MemberReject,
                                        chatMessage.message.senderAlias
                                    )
                                }
                                isLoading.value = false
                            }
                        },
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(CircleShape)
                            .background(badge_red)
                            .size(24.dp),
                        enabled = buttonsEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Decline",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KickDeclinedOrTribeDeleted(
    alias: String,
    requestType: MessageType,
    chatViewModel: ChatViewModel
) {
    val requestText = when (requestType) {
        is MessageType.GroupAction.MemberReject -> {
            "The admin\ndeclined your request"
        }
        is MessageType.GroupAction.Kick -> {
            "The admin has removed\nyou from this tribe"
        }
        else -> {
            "The admin has\ndeleted this tribe"
        }
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Card(
            backgroundColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(9.dp),
            border = BorderStroke(1.dp, light_divider),
            modifier = Modifier.padding(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = requestText,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.tertiary
                )

                Button(
                    onClick = { chatViewModel.deleteTribe() },
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = darker_gray,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                ) {
                    Text(
                        text = "Delete Tribe",
                        fontFamily = Roboto,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary

                    )
                }
            }
        }
    }
}
