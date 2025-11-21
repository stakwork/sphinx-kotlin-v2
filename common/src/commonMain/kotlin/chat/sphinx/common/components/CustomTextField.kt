package chat.sphinx.common.components

import Roboto
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import theme.place_holder_text

@Composable
fun CustomTextField(
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    placeholderText: String = "Placeholder",
    value: TextFieldValue,
    color: Color? = null,
    onValueChange: (TextFieldValue) -> Unit,
    fontSize: TextUnit = MaterialTheme.typography.body2.fontSize,
    singleLine: Boolean = true,
    maxLines: Int = 4,
    cursorBrush: Color? = null,
    enabled: Boolean = true,
    onFocusChanged: ((Boolean) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    BasicTextField(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                onFocusChanged?.invoke(focusState.isFocused)
            },
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        maxLines = maxLines,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(cursorBrush ?: MaterialTheme.colors.primary),
        textStyle = LocalTextStyle.current.copy(
            fontFamily = Roboto,
            fontWeight = FontWeight.Normal,
            fontSize = fontSize,
            color = color ?: place_holder_text
        ),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) leadingIcon()
                Box(Modifier.weight(1f)) {
                    if (value.text.isEmpty())
                        Text(
                            placeholderText,
                            style = LocalTextStyle.current.copy(
                                fontFamily = Roboto,
                                fontWeight = FontWeight.Normal,
                                fontSize = fontSize,
                                color = place_holder_text
                            )
                        )
                    innerTextField()
                }
                if (trailingIcon != null) trailingIcon()
            }
        }
    )
}
