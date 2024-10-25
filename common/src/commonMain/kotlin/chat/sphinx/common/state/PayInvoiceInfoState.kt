package chat.sphinx.common.state

data class PayInvoiceInfo(
    val invoiceString: String?,
    val amount: Long? = null,
    val expirationDate: String? = null,
    val memo: String? = null
)