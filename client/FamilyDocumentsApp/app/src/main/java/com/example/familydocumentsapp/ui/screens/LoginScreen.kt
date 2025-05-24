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
import com.example.familydocumentsapp.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    navController: NavHostController,
    onLoginSuccess: () -> Unit,
    onEmailNotConfirmed: (String) -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val loginSuccess by viewModel.loginSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val emailNotConfirmed by viewModel.emailNotConfirmed.collectAsState()
    val context = LocalContext.current

    // Наблюдаем за успешным логином
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(emailNotConfirmed) {
        emailNotConfirmed?.let { email ->
            onEmailNotConfirmed(email)
            viewModel.clearEmailNotConfirmed()
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotBlank()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            // После показа очищаем ошибку, чтобы Toast не показывался повторно
            viewModel.clearErrorMessage()
        }
    }



    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) { Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Здравствуйте!",
                fontSize = 40.sp,
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B0000),
                    fontStyle = FontStyle.Normal,
                    textAlign = TextAlign.Center
                ))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Пароль") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.login(email, password, context) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000), contentColor = Color.White),
                modifier = Modifier.height(48.dp).fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)) {
                Text("Войти")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("register") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000), contentColor = Color.White),
                modifier = Modifier.height(48.dp).fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)){
                Text("Регистрация")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { navController.navigate("forgot_password")},
                modifier = Modifier.height(48.dp).fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)){
                Text("Забыли пароль?")
            }
        }
    }
}