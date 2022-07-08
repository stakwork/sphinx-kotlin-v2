package chat.sphinx.common.components.landing

import Roboto
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import chat.sphinx.utils.getPreferredWindowSize

@Composable
fun AddFriendWindow() {
    var isOpen by remember { mutableStateOf(true) }
    var screenState: AddFriendScreenState by remember { mutableStateOf(AddFriendScreenState.Home) }

    if (isOpen) {
        Window(
            onCloseRequest = {isOpen = false},
            title = "Add Contact",
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = getPreferredWindowSize(420, 580)

            )
        ) {
            when(screenState){
                AddFriendScreenState.Home -> AddFriend(){
                    screenState = it
                }
                AddFriendScreenState.NewToSphinx -> AddNewFriendOnSphinx()
                AddFriendScreenState.AlreadyOnSphinx -> AddFriendAlreadyOnSphinx()
            }

        }
    }
}

@Composable
fun AddFriend(updateState: (AddFriendScreenState) -> Unit){
    Box(
        modifier = Modifier.fillMaxSize()
            .background(color = androidx.compose.material3.MaterialTheme.colorScheme.background )
    ){
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(75.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    updateState(AddFriendScreenState.NewToSphinx)

                },
                modifier = Modifier.clip(CircleShape)
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer )
            )
            { Text(
                text = "New to Sphinx",
                fontFamily = Roboto,
                color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
            )
            }
            Divider(Modifier.padding(12.dp), color = Color.Transparent)
            Button(
                onClick = {
                          updateState(AddFriendScreenState.AlreadyOnSphinx)
                },
                modifier = Modifier.clip(CircleShape)
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary )
            )
            { Text(
                text = "Already on Sphinx",
                fontFamily = Roboto,
                color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
            )
            }
        }
    }
}

@Composable
fun AddNewFriendOnSphinx() {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(color = androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(75.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(color = Color.White, text = "Que tal")
        }
    }
}
@Composable
fun AddFriendAlreadyOnSphinx() {

    var nicknameText by remember {
        mutableStateOf("")
    }
    var addressText by remember {
        mutableStateOf("")
    }
    var routeHintText by remember {
        mutableStateOf("")
    }
    var switchState = remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(color = androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        )
        {
            Spacer(modifier = Modifier.height(18.dp))

            Column {
                Text(
                    text = "Nickname*",
                    fontSize = 12.sp,
                    fontFamily = Roboto,
                    color = Color.Gray,
                )
                BasicTextField(
                    value = nicknameText,
                    onValueChange = {
                        nicknameText = it
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    textStyle = TextStyle(fontSize = 18.sp, color = Color.White, fontFamily = Roboto),
                    singleLine = true

                )
                Divider(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Column {
                Text(
                    text = "Address*",
                    fontSize = 12.sp,
                    fontFamily = Roboto,
                    color = Color.Gray,
                )
                BasicTextField(
                    value = addressText,
                    onValueChange = {
                        addressText = it
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    textStyle = TextStyle(fontSize = 18.sp, color = Color.White, fontFamily = Roboto),
                    singleLine = true

                )
                Divider(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Column {
                Text(
                    text = "Route Hint*",
                    fontSize = 12.sp,
                    fontFamily = Roboto,
                    color = Color.Gray,
                )
                BasicTextField(
                    value = routeHintText,
                    onValueChange = {
                        routeHintText = it
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    textStyle = TextStyle(fontSize = 18.sp, color = Color.White, fontFamily = Roboto),
                    singleLine = true

                )
                Divider(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Privacy Settings",
                        fontSize = 12.sp,
                        fontFamily = Roboto,
                        color = Color.Gray,
                    )
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = "Privacy Settings",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp).padding(start = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Standard PIN / Privacy PIN",
                        fontSize = 18.sp,
                        fontFamily = Roboto,
                        color = Color.LightGray,
                    )
                    Switch(
                        checked = switchState.value,
                        onCheckedChange = { switchState.value = it },
                        enabled = false
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                    },
                    modifier = Modifier.clip(CircleShape)
                        .fillMaxWidth()
                        .height(50.dp),

                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary
                    )
                )
                {
                    Text(
                        text = "SAVE TO CONTACTS",
                        color = Color.White,
                        fontFamily = Roboto
                    )
                }
            }
        }
    }
}



sealed class AddFriendScreenState {
    object Home: AddFriendScreenState()
    object NewToSphinx: AddFriendScreenState()
    object AlreadyOnSphinx: AddFriendScreenState()

}
