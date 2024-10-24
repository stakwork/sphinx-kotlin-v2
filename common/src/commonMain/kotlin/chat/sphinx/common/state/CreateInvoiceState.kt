package chat.sphinx.common.state

data class CreateInvoiceState(
    val memo: String = "",
    val amount: String = "",
    val saveButtonEnabled: Boolean = false,
)