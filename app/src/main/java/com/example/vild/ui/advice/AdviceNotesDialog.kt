package com.example.vild.ui.advice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.vild.data.AdviceItem
import com.example.vild.ui.theme.Grey15
import com.example.vild.ui.theme.Grey20
import com.example.vild.ui.theme.Grey60

/**
 * A popup dialog that lets the user write and save personal notes about a specific
 * [advice] item. Notes are persisted per-advice-item and shown again next time the
 * dialog is opened for the same item.
 *
 * @param advice     The advice item whose notes are being viewed/edited.
 * @param onSave     Called with the new notes string when the user taps "Save".
 * @param onDismiss  Called when the dialog should be closed without saving.
 */
@Composable
fun AdviceNotesDialog(
    advice: AdviceItem,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var notes by remember(advice.id) { mutableStateOf(advice.notes) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 480.dp)
                .background(Grey15, RoundedCornerShape(12.dp))
                .border(1.dp, Grey20, RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Text(
                text = "My Notes",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Advice:",
                style = MaterialTheme.typography.labelSmall,
                color = Grey60,
            )
            Text(
                text = advice.text,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = Color(0xFFD0D0D0),
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Grey20)
            Spacer(Modifier.height(12.dp))

            // ── Notes input ─────────────────────────────────────────────────
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("Write your thoughts here…", color = Grey60) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 120.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Grey60,
                    unfocusedBorderColor = Grey20,
                    cursorColor = Grey60,
                ),
                maxLines = 10,
            )

            Spacer(Modifier.height(12.dp))

            // ── Actions ─────────────────────────────────────────────────────
            Button(
                onClick = {
                    onSave(notes)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                ),
            ) {
                Text("Save")
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                Text("Cancel")
            }
        }
    }
}
