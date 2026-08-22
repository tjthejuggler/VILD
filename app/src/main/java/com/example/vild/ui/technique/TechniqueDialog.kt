package com.example.vild.ui.technique

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.vild.data.TechniqueItem
import com.example.vild.ui.theme.AuroraTeal
import com.example.vild.ui.theme.DeepIndigo
import com.example.vild.ui.theme.DreamPink
import com.example.vild.ui.theme.Indigo
import com.example.vild.ui.theme.Mist
import com.example.vild.ui.theme.MoonLavender
import com.example.vild.ui.theme.Violet

/**
 * Dialog for managing reality check techniques.
 * Shows existing techniques (classics marked ✦) with edit/delete,
 * plus an input field to add new ones.
 */
@Composable
fun TechniqueDialog(
    techniques: List<TechniqueItem>,
    onAdd: (String) -> Unit,
    onUpdate: (TechniqueItem, String) -> Unit,
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
                .heightIn(max = 560.dp)
                .background(DeepIndigo, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Text(
                text = "Reality Check Techniques",
                style = MaterialTheme.typography.titleMedium,
                color = MoonLavender,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Ideas for how to test whether you're dreaming. " +
                    "✦ marks the classic methods; add your own below.",
                style = MaterialTheme.typography.bodySmall,
                color = Mist,
            )

            Spacer(Modifier.height(12.dp))

            // ── Add new technique ───────────────────────────────────────────
            OutlinedTextField(
                value = newText,
                onValueChange = { newText = it },
                placeholder = { Text("Enter a new technique…", color = Mist) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MoonLavender),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Violet,
                    unfocusedBorderColor = Indigo,
                    cursorColor = MoonLavender,
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
                    containerColor = Violet,
                    contentColor = DeepIndigo,
                ),
            ) {
                Text("Add Technique")
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Indigo)
            Spacer(Modifier.height(8.dp))

            // ── Existing techniques list ────────────────────────────────────
            if (techniques.isEmpty()) {
                Text(
                    text = "No techniques. Add one above!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Mist,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(techniques, key = { it.id }) { item ->
                        if (editingId == item.id) {
                            // ── Inline edit mode ────────────────────────────
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Indigo.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = editText,
                                    onValueChange = { editText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        color = MoonLavender,
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Violet,
                                        unfocusedBorderColor = Indigo,
                                        cursorColor = MoonLavender,
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
                                            containerColor = Violet,
                                            contentColor = DeepIndigo,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    ) { Text("Save", fontSize = 12.sp) }
                                    OutlinedButton(
                                        onClick = { editingId = null },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MoonLavender,
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
                                    .background(Indigo.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (item.isSeeded) {
                                    Text(
                                        text = "✦",
                                        color = AuroraTeal,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(end = 6.dp),
                                    )
                                }
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MoonLavender,
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
                                        tint = Mist,
                                        modifier = Modifier.padding(4.dp),
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(item.id) },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = DreamPink,
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MoonLavender),
            ) {
                Text("Done")
            }
        }
    }
}
