package com.example.familydocumentsapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.example.familydocumentsapp.model.Family
import com.example.familydocumentsapp.viewmodel.HomeViewModel
import com.example.familydocumentsapp.viewmodel.ProfileViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    onHomeClicked: () -> Unit,
    onFamilySelected: (Family) -> Unit,
    onLogoutClicked: () -> Unit,
    navToHome: () -> Unit,
    onEditEmailClicked: () -> Unit,
    onEditPasswordClicked: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = remember {
        ProfileViewModel(context = context)
    }

    LaunchedEffect(Unit) {
        viewModel.loadprofile()
        viewModel.loadUserFamilies()
    }


    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val error by viewModel.error.collectAsState()
    val families by viewModel.families.collectAsState()
    val name by viewModel.name.collectAsState()
    val oldName by viewModel.oldName.collectAsState()
    val email by viewModel.email.collectAsState()
    val scope = rememberCoroutineScope()
    var showCreateFamilyDialog by remember { mutableStateOf(false) }
    var showJoinFamilyDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    Button(
                        onClick = navToHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B0000),
                            contentColor = Color.White
                        ),
                    ) {
                        Text("Главное меню")
                    }
                    Divider()

                    families.forEach { family ->
                        Button(
                            onClick = { onFamilySelected(family) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B0000),
                                contentColor = Color.White
                            ),
                        ) {
                            Text("${family.family_name} (${family.role})")
                        }
                        Divider()
                    }

                    Button(
                        onClick = { showCreateFamilyDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B0000),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Создать семью")
                    }

                    Button(
                        onClick = { showJoinFamilyDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B0000),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Вступить в семью")
                    }
                    if (showCreateFamilyDialog) {
                        var familyName by remember { mutableStateOf("") }
                        var familyPassword by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { showCreateFamilyDialog = false },
                            title = { Text("Создать Семью") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = familyName,
                                        onValueChange = { familyName = it },
                                        label = { Text("Название Семьи") },
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = familyPassword,
                                        onValueChange = { familyPassword = it },
                                        label = { Text("Пароль Семьи") },
                                        singleLine = true
                                    )
                                    // Можно добавить поле для parentId, если нужно выбирать родителя
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showCreateFamilyDialog = false
                                    coroutineScope.launch {
                                        viewModel.createFamily(
                                            name = familyName.trim(),
                                            password = familyPassword
                                        )
                                        viewModel.loadUserFamilies()
                                    }
                                }) {
                                    Text("Создать")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreateFamilyDialog = false }) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }
                    if (showJoinFamilyDialog) {
                        var familyName by remember { mutableStateOf("") }
                        var familyPassword by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { showJoinFamilyDialog = false },
                            title = { Text("Создать Семью") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = familyName,
                                        onValueChange = { familyName = it },
                                        label = { Text("Название Семьи") },
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = familyPassword,
                                        onValueChange = { familyPassword = it },
                                        label = { Text("Пароль Семьи") },
                                        singleLine = true
                                    )
                                    // Можно добавить поле для parentId, если нужно выбирать родителя
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showJoinFamilyDialog = false
                                    coroutineScope.launch {
                                        viewModel.createFamily(
                                            name = familyName.trim(),
                                            password = familyPassword
                                        )
                                    }
                                }) {
                                    Text("Создать")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showJoinFamilyDialog = false }) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }

                    Button(
                        onClick = onLogoutClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B0000),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Выйти из аккаунта")
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Личный кабинет") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF8B0000),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.loadUserFamilies()
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                                viewModel.loadprofile()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Имя пользователя с кнопкой редактирования
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text(oldName.toString()) }
                    )
                    Button(onClick = { viewModel.onNameChangeClicked() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B0000),
                            contentColor = Color.White
                        )) {
                        Text("Готово")
                    }
                }

                // Email с кнопкой редактирования
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Email: $email", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = onEditEmailClicked, colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B0000),
                        contentColor = Color.White
                    )) {
                        Text("Изменить")
                    }
                }

                // Кнопка смены пароля
                Button(
                    onClick = {onEditPasswordClicked()},
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B0000),
                        contentColor = Color.White
                    )
                ) {
                    Text("Сменить пароль")
                }
            }
        }
    }
}