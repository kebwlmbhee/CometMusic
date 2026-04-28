package com.example.cometmusic.ui.permission

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.cometmusic.R

@Composable
fun PermissionDialog(
    message: String,
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.requesting_permission)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text(
                    text = stringResource(R.string.allow),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    )
}
