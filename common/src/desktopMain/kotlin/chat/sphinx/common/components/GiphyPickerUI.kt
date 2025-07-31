package chat.sphinx.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.wrapper.toPhotoUrl
import com.soywiz.korio.serialization.xml.Xml.Companion.Text

@Composable
fun GiphyPickerUI(
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val gifs by chatViewModel.giphySearchResults.collectAsState()
    var query by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color.DarkGray)
            .padding(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                chatViewModel.searchGiphy(query)
            },
            placeholder = { Text("Search GIFs") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.White,
                placeholderColor = Color.LightGray
            )
        )

        Spacer(Modifier.height(8.dp))

        LazyRow {
            items(gifs.size) { index ->
                val gif = gifs[index]
                val gifUrl = gif.images.fixed_width.url
                PhotoUrlImage(
                    photoUrl = gifUrl.toPhotoUrl(),
                    modifier = Modifier
                        .padding(4.dp)
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            chatViewModel.sendGifMessage(gifUrl)
                        }
                )
            }
        }
    }
}