package com.example.familydocumentsapp.ui.screens


import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.example.familydocumentsapp.viewmodel.ChangePasswordViewModel

@Composable
fun ChangePasswordScreen(
    navController: NavHostController,
    navProfile: () -> Unit,
    onChangeSuccess: (String) -> Unit,
    viewModel: ChangePasswordViewModel = viewModel()
) {
    val newemail by viewModel.newemail.collectAsState()
    val password by viewModel.password.collectAsState()
    val secondpassword by viewModel.secondpassword.collectAsState()
    val message by viewModel.message.collectAsState()
    val error by viewModel.error.collectAsState()
    val changeSuccess by viewModel.changeSuccess.collectAsState()
    val context = LocalContext.current

    if (changeSuccess) {
        onChangeSuccess(password)
    }

    LaunchedEffect(message) {
        if (message.isNotBlank()) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(error) {
        if (error.isNotBlank()) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Смена почты",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),)

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = newemail,
            onValueChange = { viewModel.onEmailChange(it) },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.onPasswordChange(it) },
            label = { Text("новый пароль") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = secondpassword,
            onValueChange = { viewModel.onSecondPasswordChange(it) },
            label = { Text("Подтвердите пароль") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.sendResetPasswordRequest(context) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000), contentColor = Color.White),
        ) {
            Text("Отправить код")
        }

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(
            onClick = navProfile,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Вернуться в личный кабинет")
        }
    }
}