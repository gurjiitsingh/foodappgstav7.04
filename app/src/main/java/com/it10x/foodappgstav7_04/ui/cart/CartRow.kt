package com.it10x.foodappgstav7_04.ui.cart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.it10x.foodappgstav7_04.data.pos.entities.PosCartEntity
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextOverflow
import com.it10x.foodappgstav7_04.ui.theme.PosTheme

@Composable
fun CartRow(
    item: PosCartEntity,
    tableNo: String,
    cartViewModel: CartViewModel,
    onCartActionDirectMoveToBill: (item: PosCartEntity, print: Boolean) -> Unit
) {

    var showNoteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ✏️ Edit
        IconButton(
            onClick = { showNoteDialog = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Add Note",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // 🧾 NAME COLUMN
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 6.dp)
        ) {

            Text(
                text = item.name,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            item.note?.let { note ->
                if (note.isNotBlank()) {
                    Text(
                        text = "📝 $note",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ➕ QTY
        Row(
            modifier = Modifier.width(110.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // ➖ Remove
            IconButton(
                onClick = { cartViewModel.decrease(item.productId, tableNo) },
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        PosTheme.accent.cartRemoveBorder,
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                Text(
                    "−",
                    color = PosTheme.accent.cartRemoveText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            Text(
                text = item.quantity.toString(),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // ➕ Add
            IconButton(
                onClick = { cartViewModel.increase(item) },
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        PosTheme.accent.cartAddBg,
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                Text(
                    "+",
                    color = PosTheme.accent.cartAddText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        }

        Spacer(Modifier.width(15.dp))

        // 🍳 ACTION BUTTONS
        Row(
            modifier = Modifier.width(120.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {

//            Button(
//                onClick = { onCartActionDirectMoveToBill(item, true) },
//                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = MaterialTheme.colorScheme.primary
//                )
//            ) {
//                Icon(Icons.Default.SoupKitchen, null, Modifier.size(15.dp))
//                Spacer(Modifier.width(2.dp))
//                Icon(Icons.Default.Receipt, null, Modifier.size(15.dp))
//            }
//
//            Spacer(Modifier.width(4.dp))

            OutlinedButton(
                onClick = { onCartActionDirectMoveToBill(item, false) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                )
            ) {
                Icon(
                    Icons.Default.Receipt,
                    null,
                    Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    Spacer(Modifier.height(3.dp))
    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    Spacer(Modifier.height(3.dp))

    if (showNoteDialog) {

        var noteText by remember { mutableStateOf(item.note ?: "") }

        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Kitchen Note") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Special Instructions") },
                    singleLine = false
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        cartViewModel.updateNote(item, noteText)
                        showNoteDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNoteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

