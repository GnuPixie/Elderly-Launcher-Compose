package com.example.deka_launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.deka_launcher.ui.theme.DekaLauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showSettings by remember { mutableStateOf(false) }
            
            DekaLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSettings) {
                        SettingsScreen(
                            onBackClick = { showSettings = false }
                        )
                    } else {
                        LauncherScreen(
                            onSmsClick = { startActivity(Intent(this, SmsActivity::class.java)) },
                            onPhoneClick = { startActivity(Intent(this, PhoneActivity::class.java)) },
                            onContactsClick = { startActivity(Intent(this, ContactsActivity::class.java)) },
                            onSettingsClick = { showSettings = true }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val initialPrimaryColor = MaterialTheme.colorScheme.primary
    val initialSecondaryColor = MaterialTheme.colorScheme.secondary

    var primaryColor by remember { mutableStateOf(initialPrimaryColor) }
    var secondaryColor by remember { mutableStateOf(initialSecondaryColor) }
    var uiScale by remember { mutableStateOf(1f) }
    var darkMode by remember { mutableStateOf(false) }
    var useDeviceTheme by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Theme Settings", style = MaterialTheme.typography.titleLarge)
            
            ThemeModeSelector(
                useDeviceTheme = useDeviceTheme,
                darkMode = darkMode,
                onUseDeviceTheme = { useDeviceTheme = true },
                onLightMode = { 
                    useDeviceTheme = false
                    darkMode = false
                },
                onDarkMode = { 
                    useDeviceTheme = false
                    darkMode = true
                }
            )

            UiScaleSelector(
                uiScale = uiScale,
                onUiScaleChange = { uiScale = it }
            )

            ColorSettings(
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                onPrimaryColorChange = { primaryColor = it },
                onSecondaryColorChange = { secondaryColor = it }
            )
        }
    }
}

@Composable
private fun ThemeModeSelector(
    useDeviceTheme: Boolean,
    darkMode: Boolean,
    onUseDeviceTheme: () -> Unit,
    onLightMode: () -> Unit,
    onDarkMode: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = useDeviceTheme,
            onClick = onUseDeviceTheme,
            label = { Text("Device Theme") }
        )
        FilterChip(
            selected = !useDeviceTheme && !darkMode,
            onClick = onLightMode,
            label = { Text("Light") }
        )
        FilterChip(
            selected = !useDeviceTheme && darkMode,
            onClick = onDarkMode,
            label = { Text("Dark") }
        )
    }
}

@Composable
private fun UiScaleSelector(
    uiScale: Float,
    onUiScaleChange: (Float) -> Unit
) {
    Column {
        Text("UI Scale")
        Slider(
            value = uiScale,
            onValueChange = onUiScaleChange,
            valueRange = 0.8f..1.2f,
            steps = 4
        )
    }
}

@Composable
private fun ColorSettings(
    primaryColor: androidx.compose.ui.graphics.Color,
    secondaryColor: androidx.compose.ui.graphics.Color,
    onPrimaryColorChange: (androidx.compose.ui.graphics.Color) -> Unit,
    onSecondaryColorChange: (androidx.compose.ui.graphics.Color) -> Unit
) {
    Column {
        Text("Primary Color")
        // TODO: Add color picker
        
        Text("Secondary Color")
        // TODO: Add color picker
    }
}

@Composable
fun LauncherScreen(
    onSmsClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onContactsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Deka Launcher",
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        GridLayout(
            onSmsClick = onSmsClick,
            onPhoneClick = onPhoneClick,
            onContactsClick = onContactsClick
        )
    }
}

@Composable
fun GridLayout(
    onSmsClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onContactsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GridButton(
                text = "Contacts",
                icon = Icons.Default.Person,
                onClick = onContactsClick,
                modifier = Modifier.weight(1f)
            )
            GridButton(
                text = "Messages",
                icon = Icons.Default.Email,
                onClick = onSmsClick,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GridButton(
                text = "Phone",
                icon = Icons.Default.Call,
                onClick = onPhoneClick,
                modifier = Modifier.weight(1f)
            )
            GridButton(
                text = "More",
                icon = Icons.Default.Menu,
                onClick = { /* TODO: Implement additional features */ },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun GridButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f),
        //shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}