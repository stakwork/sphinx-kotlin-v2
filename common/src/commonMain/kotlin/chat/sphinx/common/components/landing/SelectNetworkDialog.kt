package chat.sphinx.common.components.landing

import Roboto
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import chat.sphinx.utils.SphinxFonts
@Composable
fun SelectNetworkDialog(
    onDismiss: () -> Unit,
    onRegtestSelected: () -> Unit,
    onBitcoinSelected: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .background(
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Text(
                    text = "Network",
                    textAlign = TextAlign.Center,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp),
                    fontFamily = Roboto
                )
                Text(
                    text = "Please choose a network:",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    fontSize = 13.sp,
                    fontFamily = Roboto,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            onRegtestSelected()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary
                        )
                    ) {

                        Text(
                            "Regtest",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                            fontFamily = Roboto
                        )
                    }
                    Button(
                        onClick = {
                            onBitcoinSelected()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            "Bitcoin",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                            fontFamily = Roboto
                        )
                    }
                }
            }
        }
    }
}
