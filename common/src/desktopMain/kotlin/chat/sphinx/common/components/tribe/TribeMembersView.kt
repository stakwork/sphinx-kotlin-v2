package chat.sphinx.common.components.tribe

import Roboto
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import chat.sphinx.common.viewmodel.chat.TribeMembersViewModel
import chat.sphinx.utils.getPreferredWindowSize
import chat.sphinx.wrapper.PhotoUrl
import chat.sphinx.wrapper.mqtt.TribeMember
import theme.light_divider


@Composable
fun TribeMembersView(
    tribeMembersViewModel: TribeMembersViewModel
) {
    var isOpen by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    val viewState = tribeMembersViewModel.tribeMembersViewState

    val endOfListReached by remember {
        derivedStateOf {
            listState.isScrolledToEnd()
        }
    }

    if (isOpen) {
        Window(
            onCloseRequest = { isOpen = false },
            title = "Tribe Members",
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = getPreferredWindowSize(420, 700)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (viewState.loadingTribeMembers) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(viewState.tribeMembersList.size) { index ->
                            viewState.tribeMembersList[index].let { tribeMember ->
                                TribeMemberRow(tribeMember)
                            }
                        }
                        if (viewState.loadingMore) {
                            item { LoadingRow() }
                        }
                    }
                    if (endOfListReached) {
//                        tribeMembersViewModel.loadMoreMembers()
                    }
                }
            }
        }
    }
}

@Composable
fun TribeMemberRow(tribeMember: TribeMember) {
    val dividerColor = light_divider

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
                    .padding(start = 16.dp ,end = 16.dp)
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
                onClick = { /* Handle member removal */ },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Remove Member",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Divider(color = dividerColor, modifier = Modifier.padding(start = 96.dp ,end = 16.dp))
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
