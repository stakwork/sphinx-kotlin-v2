package chat.sphinx.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.sphinx.common.viewmodel.chat.ChatViewModel
import chat.sphinx.wrapper.toPhotoUrl
import com.soywiz.korio.serialization.xml.Xml.Companion.Text

@Composable
fun GiphyPickerUI(
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val gifs by chatViewModel.giphySearchResults.collectAsState()
    var query by remember { mutableStateOf(TextFieldValue("")) }

    Box(
        modifier = modifier
            .fillMaxSize() // Full available space to allow bottom alignment
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter) // 👈 Align to bottom here
                .fillMaxWidth()
                .height(400.dp)
                .padding(top = 16.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        ) {
            Spacer(Modifier.height(8.dp))
            // Search bar
            CustomTextField(
                leadingIcon = { Spacer(modifier = Modifier.width(8.dp)) },
                trailingIcon = {
                    if (query.text.isNotEmpty()) {
                        Icon(
                            Icons.Filled.Cancel,
                            contentDescription = "Clear search",
                            tint = Color.LightGray,
                            modifier = Modifier
                                .width(28.dp)
                                .clickable {
                                    query = TextFieldValue("")
                                    chatViewModel.fetchTrendingGifs()
                                }
                        )
                    } else {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.width(28.dp),
                            tint = Color.LightGray
                        )
                    }
                },
                modifier = Modifier
                    .background(
                        androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(percent = 50)
                    )
                    .padding(4.dp)
                    .height(36.dp),
                fontSize = 14.sp,
                placeholderText = "Search GIFs on Giphy",
                onFocusChanged = {},
                onValueChange = {
                    query = it
                    chatViewModel.searchGiphy(query.text)
                },
                value = query,
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxSize()
                    .background( androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(gifs.size) { index ->
                    val gif = gifs[index]
                    val gifUrl = gif.images.fixed_width.url

                    PhotoUrlImage(
                        photoUrl = gifUrl.toPhotoUrl(),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                chatViewModel.sendGifMessage(gifUrl)
                            }
                    )
                }
            }
        }
    }
}