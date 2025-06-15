package com.example.deka_launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.deka_launcher.models.Contact
import com.example.deka_launcher.ui.theme.DekaLauncherTheme
import kotlinx.coroutines.delay

class ContactsActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, load contacts
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
        setContent {
            DekaLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ContactsScreen(
                        onBackClick = { finish() },
                        onAddContact = { showAddContactDialog() },
                        onEditContact = { contact -> showEditContactDialog(contact) },
                        onDeleteContact = { contact -> deleteContact(contact) }
                    )
                }
            }
        }
    }

    private fun checkPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun showAddContactDialog() {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
        }
        startActivity(intent)
    }

    private fun showEditContactDialog(contact: Contact) {
        val intent = Intent(Intent.ACTION_EDIT).apply {
            data = ContactsContract.Contacts.getLookupUri(
                contact.id.toLongOrNull() ?: 0L,
                contact.lookupKey
            )
        }
        startActivity(intent)
    }

    private fun deleteContact(contact: Contact) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = ContactsContract.Contacts.getLookupUri(
                contact.id.toLongOrNull() ?: 0L,
                contact.lookupKey
            )
        }
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onBackClick: () -> Unit,
    onAddContact: () -> Unit,
    onEditContact: (Contact) -> Unit,
    onDeleteContact: (Contact) -> Unit
) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    // Load contacts initially
    LaunchedEffect(Unit) {
        loadContacts(context) { loadedContacts ->
            contacts = loadedContacts
        }
    }

    // Set up periodic refresh
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // Refresh every second
            loadContacts(context) { loadedContacts ->
                contacts = loadedContacts
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { Text("Contacts") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onAddContact) {
                    Icon(Icons.Default.Add, contentDescription = "Add Contact")
                }
            }
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            placeholder = { Text("Search contacts") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                contacts.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phoneNumber.contains(searchQuery)
                }
            ) { contact ->
                ContactItem(
                    contact = contact,
                    onEdit = { onEditContact(contact) },
                    onDelete = { onDeleteContact(contact) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactItem(
    contact: Contact,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Contact",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun loadContacts(context: android.content.Context, onContactsLoaded: (List<Contact>) -> Unit) {
    val contacts = mutableListOf<Contact>()
    
    context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
        ),
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            val phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
            val lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY))
            
            contacts.add(Contact(
                id = id,
                name = name,
                phoneNumber = phoneNumber,
                lookupKey = lookupKey
            ))
        }
    }
    
    onContactsLoaded(contacts)
} 
