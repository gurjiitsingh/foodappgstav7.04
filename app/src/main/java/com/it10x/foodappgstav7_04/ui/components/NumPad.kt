package com.it10x.foodappgstav7_04.ui.components



import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NumPad(
    onInput: (String) -> Unit
) {

    val buttons = listOf(
        listOf("1", "2", "3", "." ),
        listOf("4", "5", "6", "0"),
        listOf("7", "8", "9", "←"),

    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

        buttons.forEach { row ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                row.forEach { label ->

                    Button(
                        onClick = { onInput(label) },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE0E0E0),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(label, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
