package chat.sphinx.common.state

sealed class SearchState {
    object Inactive : SearchState()
    object Active : SearchState()
    object Searching : SearchState()
}
