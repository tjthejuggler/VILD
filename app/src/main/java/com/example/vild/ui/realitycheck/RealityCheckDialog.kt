package com.example.vild.ui.realitycheck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.vild.data.RealityCheckTrigger
import com.example.vild.ui.theme.Grey10
import com.example.vild.ui.theme.Grey15
import com.example.vild.ui.theme.Grey20
import com.example.vild.ui.theme.Grey60

/**
 * Dialog for managing reality check triggers.
 * Shows existing triggers with edit/delete, plus an input field to add new ones.
 * Same pattern as [com.example.vild.ui.advice.AdviceDialog].
 */
@Composable
fun RealityCheckDialog(
    triggers: List<RealityCheckTrigger>,
    onAdd: (String) -> Unit,
    onUpdate: (RealityCheckTrigger, String) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var newText by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 520.dp)
                .background(Grey15, RoundedCornerShape(12.dp))
                .border(1.dp, Grey20, RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Text(
                text = "Reality Check Triggers",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Add triggers that will randomly appear as a daily morning notification at 8 AM.",
                style = MaterialTheme.typography.bodySmall,
                color = Grey60,
            )

            Spacer(Modifier.height(12.dp))

            // ── Add new trigger ─────────────────────────────────────────────
            OutlinedTextField(
                value = newText,
                onValueChange = { newText = it },
                placeholder = { Text("Enter new trigger…", color = Grey60) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Grey60,
                    unfocusedBorderColor = Grey20,
                    cursorColor = Grey60,
                ),
                maxLines = 3,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onAdd(newText)
                    newText = ""
                },
                enabled = newText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                ),
            ) {
                Text("Add Trigger")
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Grey20)
            Spacer(Modifier.height(8.dp))

            // ── Existing triggers list ──────────────────────────────────────
            if (triggers.isEmpty()) {
                Text(
                    text = "No triggers yet. Add one above!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Grey60,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(triggers, key = { it.id }) { item ->
                        if (editingId == item.id) {
                            // ── Inline edit mode ────────────────────────────
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Grey10, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = editText,
                                    onValueChange = { editText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White,
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Grey60,
                                        unfocusedBorderColor = Grey20,
                                        cursorColor = Grey60,
                                    ),
                                    maxLines = 3,
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            onUpdate(item, editText)
                                            editingId = null
                                        },
                                        enabled = editText.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = Color.White,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    ) { Text("Save", fontSize = 12.sp) }
                                    OutlinedButton(
                                        onClick = { editingId = null },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    ) { Text("Cancel", fontSize = 12.sp) }
                                }
                            }
                        } else {
                            // ── Display mode ────────────────────────────────
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Grey10, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = {
                                        editingId = item.id
                                        editText = item.text
                                    },
                                    modifier = Modifier.padding(start = 4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Grey60,
                                        modifier = Modifier.padding(4.dp),
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(item.id) },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFE57373),
                                        modifier = Modifier.padding(4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                Text("Done")
            }
        }
    }
}
