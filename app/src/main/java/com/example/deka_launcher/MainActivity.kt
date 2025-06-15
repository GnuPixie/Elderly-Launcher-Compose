package com.example.deka_launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
            DekaLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LauncherScreen(
                        onSmsClick = { startActivity(Intent(this, SmsActivity::class.java)) },
                        onPhoneClick = { startActivity(Intent(this, PhoneActivity::class.java)) }
                    )
                }
            }
        }
    }
}

@Composable
fun LauncherScreen(
    onSmsClick: () -> Unit,
    onPhoneClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Deka Launcher",
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 32.dp)
        )

        LauncherButton(
            text = "Contacts",
            onClick = { /* TODO: Implement contacts screen */ }
        )

        LauncherButton(
            text = "Messages",
            onClick = onSmsClick
        )

        LauncherButton(
            text = "Phone",
            onClick = onPhoneClick
        )

        LauncherButton(
            text = "More",
            onClick = { /* TODO: Implement additional features */ }
        )
    }
}

@Composable
fun LauncherButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}