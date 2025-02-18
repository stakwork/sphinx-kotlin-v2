package chat.sphinx.common.state

import chat.sphinx.wrapper.PhotoUrl

data class AliasMatcherState(
    val isOn:  Boolean = false,
    val inputText: String = "",
    val suggestedAliasAndPicList: List<Triple<String, PhotoUrl?, Int?>> = emptyList(),
    val selectedItem: Int = 0,
)
