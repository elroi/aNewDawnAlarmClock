package com.elroi.alarmpal.ui.components

import android.Manifest
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun BuddySelectionDialog(
    onDismiss: () -> Unit,
    onBuddySelected: (name: String, phone: String) -> Unit,
    globalBuddies: Set<String>,
    startInManualMode: Boolean = false
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var showManualEntry by remember { mutableStateOf(startInManualMode || globalBuddies.isEmpty()) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.query(uri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) name = cursor.getString(0) ?: ""
        }
        context.contentResolver.query(uri, arrayOf(ContactsContract.Contacts._ID), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val contactId = cursor.getString(0)
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId), null
                )?.use { phoneCursor ->
                    if (phoneCursor.moveToFirst()) {
                        phone = phoneCursor.getString(0) ?: ""
                        showManualEntry = true
                    }
                }
            }
        }
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) contactPickerLauncher.launch(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (showManualEntry) "Add New Buddy" else "Select Buddy") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showManualEntry) {
                    OutlinedButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.READ_CONTACTS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasPermission) contactPickerLauncher.launch(null)
                            else contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pick from Contacts")
                    }

                    Text("— or enter manually —",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Buddy Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        placeholder = { Text("+1...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(globalBuddies.toList()) { buddyStr ->
                            val parts = buddyStr.split("|")
                            val bName = parts.getOrNull(0) ?: "Unknown"
                            val bPhone = parts.getOrNull(1) ?: ""

                            Surface(
                                onClick = { onBuddySelected(bName, bPhone) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bName, fontWeight = FontWeight.Bold)
                                        Text(bPhone, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { showManualEntry = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add New Buddy")
                    }
                }
            }
        },
        confirmButton = {
            if (showManualEntry) {
                TextButton(
                    onClick = { if (name.isNotBlank() && phone.isNotBlank()) onBuddySelected(name, phone) },
                    enabled = name.isNotBlank() && phone.isNotBlank()
                ) {
                    Text("Add & Select")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (showManualEntry && globalBuddies.isNotEmpty()) showManualEntry = false
                else onDismiss()
            }) {
                Text(if (showManualEntry && globalBuddies.isNotEmpty()) "Back" else "Cancel")
            }
        }
    )
}
