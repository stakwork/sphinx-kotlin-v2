package chat.sphinx.common.state

import chat.sphinx.wrapper.PhotoUrl
import com.google.zxing.common.BitMatrix

data class ContactQRCodeState(
    val viewTitle: String = "QR Code",
    val string: String = "",
    val bitMatrix: BitMatrix? = null,
    val ownerAlias: String? = null,
    val ownerPicture: PhotoUrl? = null
)
