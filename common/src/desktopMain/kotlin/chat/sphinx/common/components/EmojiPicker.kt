package chat.sphinx.common.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import chat.sphinx.utils.EmojiCategory
import chat.sphinx.utils.EmojiData
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiPicker(
    isVisible: Boolean,
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .width(400.dp)
                .height(350.dp)
                .padding(8.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
        ) {
            Column {
                // Header with close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 12.dp, 12.dp, 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Emojis",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                EmojiPickerContent(
                    onEmojiSelected = { emoji ->
                        EmojiData.addToRecent(emoji)
                        onEmojiSelected(emoji)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmojiPickerContent(
    onEmojiSelected: (String) -> Unit
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val allCategories = buildList {
        EmojiData.getRecentCategory()?.let { add(it) }
        addAll(EmojiData.categories)
    }

    val filteredEmojis = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            emptyList()
        } else {
            EmojiData.categories
                .flatMap { it.emojis }
                .filter { emoji ->
                    // You can implement more sophisticated search logic here
                    // For now, just return all emojis when searching
                    true
                }
        }
    }

    Column {
        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (searchQuery.isEmpty()) {
            // Category tabs
            CategoryTabs(
                categories = allCategories,
                selectedIndex = selectedCategoryIndex,
                onCategorySelected = { selectedCategoryIndex = it },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Emoji grid for selected category
            EmojiGrid(
                emojis = allCategories[selectedCategoryIndex].emojis,
                onEmojiSelected = onEmojiSelected,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Search results
            EmojiGrid(
                emojis = filteredEmojis,
                onEmojiSelected = onEmojiSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(
                androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )

            Spacer(Modifier.width(8.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(androidx.compose.material3.MaterialTheme.colorScheme.primary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            "Search emojis...",
                            fontSize = 14.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun CategoryTabs(
    categories: List<EmojiCategory>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        items(categories.size) { index ->
            val category = categories[index]
            val isSelected = index == selectedIndex

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected)
                            androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else
                            Color.Transparent
                    )
                    .clickable { onCategorySelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.icon,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
private fun EmojiGrid(
    emojis: List<String>,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(emojis) { emoji ->
            EmojiItem(
                emoji = emoji,
                onEmojiSelected = onEmojiSelected
            )
        }
    }
}

@Composable
private fun EmojiItem(
    emoji: String,
    onEmojiSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onEmojiSelected(emoji) }
            .background(Color.Transparent)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 22.sp
        )
    }
}