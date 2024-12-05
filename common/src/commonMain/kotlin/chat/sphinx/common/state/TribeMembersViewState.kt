package chat.sphinx.common.state

import chat.sphinx.wrapper.mqtt.TribeMember

data class TribeMembersViewState(
    val loadingTribeMembers: Boolean = true,
    val loadingMore: Boolean = false,
    val tribeMembersList: List<TribeMember> = mutableListOf()
)