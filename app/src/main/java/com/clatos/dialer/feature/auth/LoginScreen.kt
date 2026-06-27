package com.clatos.dialer.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var server by remember { mutableStateOf(viewModel.lastServerUrl()) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val canSubmit = server.isNotBlank() && username.isNotBlank() && password.isNotBlank() &&
        state !is LoginUiState.Loading
    fun submit() = viewModel.login(server, username, password)

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Clatos Dialer", modifier = Modifier.padding(bottom = 24.dp))
        OutlinedTextField(
            value = server,
            onValueChange = { server = it },
            label = { Text("CRM server URL") },
            placeholder = { Text("https://yourcompany.crm.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("CRM username") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (canSubmit) submit() }),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        )
        when (val s = state) {
            is LoginUiState.Loading -> CircularProgressIndicator()
            is LoginUiState.Error -> Text(s.message, modifier = Modifier.padding(bottom = 8.dp))
            else -> Unit
        }
        Button(
            onClick = { submit() },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Log in") }
    }
}
