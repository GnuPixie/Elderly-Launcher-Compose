package com.example.deka_launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.deka_launcher.models.Contact
import com.example.deka_launcher.ui.theme.DekaLauncherTheme
import android.database.Cursor
import android.provider.Telephony.Sms
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.app.role.RoleManager
import android.content.Context
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.telephony.SmsManager
import kotlinx.coroutines.flow.update
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import kotlinx.coroutines.delay
//import androidx.compose.ui.DisposableEffect

fun requestDefaultSmsRole(context: Context, resultLauncher: ActivityResultLauncher<Intent>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        // Check if the app is already the default SMS app
        if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
            // Create an intent to request the role
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
            resultLauncher.launch(intent)
        }
    }
    // For older APIs, you would use a different, more complex method.
    // But targeting modern APIs, RoleManager is the standard.
}

class SmsActivity : ComponentActivity() {
    private val conversations = MutableStateFlow<List<Contact>>(emptyList())
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            setDefaultSmsApp()
        }
    }

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.example.deka_launcher.SMS_RECEIVED" -> {
                    // Refresh the conversation list
                    loadConversations(context!!, conversations)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            checkPermissions()
            registerReceiver(
                smsReceiver,
                IntentFilter("com.example.deka_launcher.SMS_RECEIVED"),
                Context.RECEIVER_NOT_EXPORTED
            )
            setContent {
                DekaLauncherTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        SmsScreen(
                            onBackClick = { finish() },
                            conversations = conversations
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmsActivity", "Error in onCreate", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(smsReceiver)
        } catch (e: Exception) {
            Log.e("SmsActivity", "Error unregistering receiver", e)
        }
    }

    private fun checkPermissions() {
        try {
            val permissions = arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS
            )

            if (permissions.all { 
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED 
            }) {
                setDefaultSmsApp()
            } else {
                requestPermissionLauncher.launch(permissions)
            }
        } catch (e: Exception) {
            Log.e("SmsActivity", "Error checking permissions", e)
        }
    }

    private fun setDefaultSmsApp() {
        try {
            if (!Telephony.Sms.getDefaultSmsPackage(this).equals(packageName)) {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("SmsActivity", "Error setting default SMS app", e)
        }
    }

    public fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            
            // Add the sent message to the conversation immediately
            conversations.update { currentList ->
                val updatedList = currentList.toMutableList()
                val contactIndex = updatedList.indexOfFirst { contact -> contact.phoneNumber == phoneNumber }
                
                if (contactIndex != -1) {
                    val contact = updatedList[contactIndex]
                    updatedList[contactIndex] = contact.copy(
                        lastMessage = message,
                        date = System.currentTimeMillis()
                    )
                } else {
                    // If contact doesn't exist in the list, add it
                    updatedList.add(Contact(
                        name = phoneNumber,
                        phoneNumber = phoneNumber,
                        lastMessage = message,
                        date = System.currentTimeMillis()
                    ))
                }
                updatedList.sortedByDescending { it.date }
            }
            
            // Broadcast the update
            val updateIntent = Intent("com.example.deka_launcher.SMS_RECEIVED")
            context.sendBroadcast(updateIntent)
            
        } catch (e: Exception) {
            Log.e("SmsActivity", "Error sending SMS", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsScreen(
    onBackClick: () -> Unit,
    conversations: MutableStateFlow<List<Contact>>
) {
    var showConversation by remember { mutableStateOf(false) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    val context = LocalContext.current
    val conversationsState by conversations.collectAsState()
    
    LaunchedEffect(Unit) {
        try {
            loadConversations(context, conversations)
        } catch (e: Exception) {
            Log.e("SmsScreen", "Error loading conversations", e)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { 
                Text(
                    text = if (showConversation) "Messages" else "Conversations",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                if (showConversation) {
                    IconButton(onClick = { 
                        showConversation = false
                        selectedContact = null
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            }
        )

        if (!showConversation) {
            ConversationList(
                conversations = conversationsState,
                onConversationClick = { contact ->
                    selectedContact = contact
                    showConversation = true
                }
            )
        } else {
            selectedContact?.let { contact ->
                ConversationScreen(
                    contact = contact,
                    onBackClick = onBackClick,
                    onSendMessage = { message ->
                        (context as? SmsActivity)?.sendSms(context, contact.phoneNumber, message)
                    }
                )
            }
        }
    }
}

@Composable
fun ConversationList(
    conversations: List<Contact>,
    onConversationClick: (Contact) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(conversations) { contact ->
            ConversationItem(
                contact = contact,
                onClick = { onConversationClick(contact) }
            )
        }
    }
}

@Composable
fun ConversationItem(
    contact: Contact,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = contact.lastMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (contact.unreadCount > 0) {
                Badge(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(text = contact.unreadCount.toString())
                }
            }
        }
    }
}

@Composable
fun ConversationScreen(
    contact: Contact,
    onBackClick: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember { MutableStateFlow<List<Message>>(emptyList()) }
    val context = LocalContext.current
    val messagesState by messages.collectAsState()

    // Add a LaunchedEffect to periodically refresh messages
    LaunchedEffect(contact) {
        loadMessages(context, contact.phoneNumber, messages)
        // Set up periodic refresh
        while (true) {
            delay(1000) // Refresh every second
            loadMessages(context, contact.phoneNumber, messages)
        }
    }

    // Add a LaunchedEffect to listen for new messages
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.deka_launcher.SMS_RECEIVED") {
                    loadMessages(context!!, contact.phoneNumber, messages)
                }
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter("com.example.deka_launcher.SMS_RECEIVED"),
            Context.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.e("ConversationScreen", "Error unregistering receiver", e)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messagesState) { message ->
                MessageItem(message = message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = { Text("Type a message") }
            )

            Button(
                onClick = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText)
                        messageText = ""
                    }
                },
                modifier = Modifier.height(56.dp)
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
fun MessageItem(message: Message) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isFromMe) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.secondary
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = message.sender,
                style = MaterialTheme.typography.labelLarge,
                color = if (message.isFromMe) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSecondary
            )
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = if (message.isFromMe) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}

private fun loadConversations(context: android.content.Context, conversations: MutableStateFlow<List<Contact>>) {
    try {
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ
            ),
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )

        val contactMap = mutableMapOf<String, Contact>()
        
        cursor?.use {
            while (it.moveToNext()) {
                try {
                    val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                    val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                    val read = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1

                    val contact = contactMap.getOrPut(address) {
                        Contact(
                            name = address,
                            phoneNumber = address,
                            lastMessage = body,
                            date = date,
                            unreadCount = 0
                        )
                    }

                    if (!read) {
                        contact.unreadCount++
                    }
                } catch (e: Exception) {
                    Log.e("SmsActivity", "Error processing message", e)
                }
            }
        }

        conversations.value = contactMap.values.sortedByDescending { it.date }
    } catch (e: Exception) {
        Log.e("SmsActivity", "Error loading conversations", e)
        conversations.value = emptyList()
    }
}

private fun loadMessages(context: android.content.Context, phoneNumber: String, messages: MutableStateFlow<List<Message>>) {
    val cursor = context.contentResolver.query(
        Telephony.Sms.CONTENT_URI,
        arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        ),
        "${Telephony.Sms.ADDRESS} = ?",
        arrayOf(phoneNumber),
        "${Telephony.Sms.DATE} ASC"
    )

    val messageList = mutableListOf<Message>()
    
    cursor?.use {
        while (it.moveToNext()) {
            val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
            val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
            val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
            val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))

            messageList.add(
                Message(
                    sender = address,
                    content = body,
                    isFromMe = type == Telephony.Sms.MESSAGE_TYPE_SENT,
                    timestamp = date
                )
            )
        }
    }

    messages.value = messageList
}

data class Contact(
    val name: String,
    val phoneNumber: String,
    val lastMessage: String,
    val date: Long,
    var unreadCount: Int = 0
)

data class Message(
    val sender: String,
    val content: String,
    val isFromMe: Boolean,
    val timestamp: Long
) 