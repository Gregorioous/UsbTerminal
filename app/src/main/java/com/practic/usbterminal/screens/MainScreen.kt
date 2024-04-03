@file:OptIn(ExperimentalMaterial3Api::class)

package com.practic.usbterminal.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TerminalScreen(logs: List<String>, modifier: Modifier) {
    Column(
        modifier = modifier.padding(8.dp)
    ) {
        Text("Terminal Log", fontSize = 20.sp, modifier = Modifier.padding(8.dp))
        Divider(color = Color.Black, thickness = 1.dp)
        Box(
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn {
                items(logs) { log ->
                    Text(log.toString(), fontSize = 14.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
fun ChatScreen(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier.padding(8.dp)
    ) {
        Text("Chat Screen", fontSize = 20.sp, modifier = Modifier.padding(8.dp))
        Divider(color = Color.Black, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var text by remember { mutableStateOf(message) }
            TextField(
                value = text,
                onValueChange = {
                    text = it
                    onMessageChange(text)
                },
                label = { Text("Enter Message") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendMessage() })
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onSendMessage() }) {
                Text("Send")
            }
        }
    }
}


@Composable
fun MainScreen() {
    val logs = remember { List(10) { "Log Entry $it" } }
    val messageState = remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Верхняя часть экрана (терминал), занимает 80% высоты
        TerminalScreen(logs = logs, modifier = Modifier.weight(0.8f))

        // Нижняя часть экрана (чат), занимает 20% высоты
        ChatScreen(
            message = messageState.value,
            onMessageChange = { messageState.value = it },
            onSendMessage = { /* Implement your send message logic here */ },
            modifier = Modifier.weight(0.2f)
        )
    }
}

@Preview
@Composable
fun PreviewMainScreen() {
    MainScreen()
}


