package com.example.familydocumentsapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.familydocumentsapp.viewmodel.ChangeEmailConfirmViewModel

@Composable
fun ChangeEmailConfirmScreen(
    navController: NavHostController,
    onConfirmSuccess: () -> Unit,
    onBackToChange: () -> Unit,
    viewModel: ChangeEmailConfirmViewModel = viewModel(),
) {
    val code by viewModel.code.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val confirmSuccess by viewModel.confirmSuccess.collectAsState()

    val context = LocalContext.current

    // При успешном подтверждении переходим на экран логина
    LaunchedEffect(confirmSuccess) {
        if (confirmSuccess) {
            onConfirmSuccess()
        }
    }

    // Показываем Toast при ошибке
    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotBlank()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearErrorMessage()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Введите код подтверждения, отправленный на вашу новую почту",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = code,
                onValueChange = viewModel::onCodeChange,
                label = { Text("Код подтверждения") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))


            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.confirmCode(context) },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000), contentColor = Color.White),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Подтвердить")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            TextButton(
                onClick = onBackToChange,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Вернуться на экран восстановления")
            }
        }
    }
}