package com.example.familydocumentsapp.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.familydocumentsapp.model.Family
import com.example.familydocumentsapp.model.PersonalDocument
import com.example.familydocumentsapp.model.PersonalFolder
import com.example.familydocumentsapp.viewmodel.FamilyHomeViewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.*
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import com.example.familydocumentsapp.R
import com.example.familydocumentsapp.viewmodel.FamilyHomeViewModel.SortType
import com.example.familydocumentsapp.viewmodel.FamilyHomeViewModel.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyHomeScreen(
    family: String,
    navController: NavHostController,
    onProfileClicked: () -> Unit,
    navToHome: () -> Unit,
    onFamilySelected: (Family) -> Unit,
    onLogoutClicked: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = remember {
        FamilyHomeViewModel(context = context)
    }
    val folders by viewModel.filteredFolders.collectAsState()
    val documents by viewModel.filteredDocuments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val families by viewModel.families.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showInputDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var expanded2 by remember { mutableStateOf(false) }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFamilyDialog by remember { mutableStateOf(false) }
    var showJoinFamilyDialog by remember { mutableStateOf(false) }
    val isOnline by viewModel.isOnlineMode.collectAsState()

    val options = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(false) // Разрешить импорт из галереи
        .setPageLimit(20) // Максимальное количество страниц
        .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF) // Форматы результатов
        .setScannerMode(SCANNER_MODE_FULL) // Режим сканера
        .build()

    val scanner = GmsDocumentScanning.getClient(options)

    val activity = getActivity(context)

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pages?.forEach { page ->
                val imageUri = page.imageUri
                Log.d("MLKit", "Image URI: $imageUri")
            }
            scanResult?.pdf?.let { pdf ->
                pdfUri = pdf.uri
                showInputDialog = true
                Log.d("MLKit", "PDF URI: ${pdf.uri}, Pages: ${pdf.pageCount}")
            }
        }
    }

    if (showInputDialog && pdfUri != null) {
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }


        AlertDialog(
            onDismissRequest = { showInputDialog = false },
            title = { Text("Данные документа") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Название") }
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Описание") }
                    )
                    // Здесь можно добавить UI для выбора папки (например, DropdownMenu или Spinner)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        viewModel.uploadScannedDocument(
                            context = context,
                            uri = pdfUri!!,
                            name = name.ifBlank { "Новый документ" },
                            description = description,
                            folderId = viewModel.currentFolderId.value,
                            familyId = family
                        )
                        showInputDialog = false
                    }}) {
                    Text("Загрузить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInputDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }


    val scanDocument = {
        scanner.getStartScanIntent(context as Activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }
            .addOnFailureListener {
                Log.e("MLKit", "Ошибка запуска сканера", it)
            }
    }


    fun logout(context: Context) {
        val sharedPref = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().remove("TOKEN_KEY").apply()
        onLogoutClicked()
    }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                pdfUri = uri
                showInputDialog = true
            }
        }
    )



    LaunchedEffect(Unit) {
        viewModel.loadFamilyStorage(familyId = family.toInt())
        viewModel.loadUserFamilies()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()) // 👈 Добавили прокрутку
                ) {
                    Button(
                        onClick = { onProfileClicked() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000), contentColor = Color.White),
                    ) {
                        Text("Личный кабинет")
                    }
                    Divider()
                    Button(
                        onClick = { navToHome() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000), contentColor = Color.White),
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000), contentColor = Color.White),
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
                            containerColor = Color(0xFF8B0000),      // фон кнопки
                            contentColor = Color.White              // цвет текста/иконок внутри кнопки
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
                            containerColor = Color(0xFF8B0000),      // фон кнопки
                            contentColor = Color.White              // цвет текста/иконок внутри кнопки
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
                        onClick = { logout(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B0000),      // фон кнопки
                            contentColor = Color.White              // цвет текста/иконок внутри кнопки
                        )
                    ) {
                        Text("Выйти из аккаунта")
                    }

                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = { Text("Поиск...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(0.8f)
                        )
                    },
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
                            viewModel.loadFamilyStorage(viewModel.currentFolderId.value, familyId = family.toInt())
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                        }
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(painter = painterResource(id = R.drawable.sort_24dp_1f1f1f_fill0_wght400_grad0_opsz24), contentDescription = "Сортировка")
                            }
                            DropdownMenu(
                                expanded = expanded2,
                                onDismissRequest = { expanded2 = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("По названию") },
                                    onClick = {
                                        viewModel.setSortType(SortType.BY_NAME)
                                        expanded2 = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("По дате (сначала новые)") },
                                    onClick = {
                                        viewModel.setSortType(SortType.BY_DATE_ASC)
                                        expanded2 = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("По дате (сначала старые)") },
                                    onClick = {
                                        viewModel.setSortType(SortType.BY_DATE_DESC)
                                        expanded2 = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF8B0000),     // фон — тёмно-красный
                        titleContentColor = Color.White,        // цвет текста в заголовке
                        actionIconContentColor = Color.White,   // цвет иконок справа
                        navigationIconContentColor = Color.White // цвет иконки меню
                    )
                )
            },
            floatingActionButton = {
                Box {
                    FloatingActionButton(onClick = {
                        expanded = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd) // якорь — правый верх относительно FAB
                    ) {
                        DropdownMenuItem(
                            text = { Text("Сканировать с камеры") },
                            onClick = {
                                expanded = false
                                scanDocument()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Загрузить PDF") },
                            onClick = {
                                expanded = false
                                pickPdfLauncher.launch(arrayOf("application/pdf"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Создать папку") },
                            onClick = {
                                expanded = false
                                showCreateFolderDialog = true
                            }
                        )
                    }
                    if (showCreateFolderDialog) {
                        var folderName by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { showCreateFolderDialog = false },
                            title = { Text("Создать папку") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = folderName,
                                        onValueChange = { folderName = it },
                                        label = { Text("Название папки") },
                                        singleLine = true
                                    )
                                    // Можно добавить поле для parentId, если нужно выбирать родителя
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showCreateFolderDialog = false
                                    coroutineScope.launch {
                                        viewModel.createFamilyFolder(
                                            name = folderName.trim(),
                                            family= family.toInt(),
                                            parentId = viewModel.currentFolderId.value // или выбери текущую папку, если нужно
                                        )
                                    }
                                }) {
                                    Text("Создать")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreateFolderDialog = false }) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }
                }
            }
        ) { padding ->

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error.isNotBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error, color = Color.Red)
                }
            } else {
                var expandedIndex by remember { mutableStateOf<Int?>(null) }
                Column(modifier = Modifier.padding(padding)) {
                    ModeSwitchRow(
                        isOnline = isOnline,
                        onModeChange = { viewModel.setOnlineMode(it) }
                    )
                    LazyColumn(modifier = Modifier.padding(horizontal = padding.calculateLeftPadding(LayoutDirection.Ltr)).weight(1f)) {
                        if (viewModel.currentFolder.value != null) {
                            val parentId = viewModel.currentFolder.value?.parent
                            item {
                                ListItem(
                                    headlineContent = { Text("◀ Назад") },
                                    leadingContent = {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.loadFamilyStorage(parentId, familyId = family.toInt())
                                        }
                                )
                                Divider()
                            }
                        }
                        items(folders) { folder ->
                            Column {

                                ListItem(
                                    headlineContent = { Text(folder.name) },
                                    leadingContent = {
                                        Icon(painterResource(id = R.drawable.folder_24dp_1f1f1f_fill0_wght400_grad0_opsz24), contentDescription = "Папка")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // TODO: Действие при клике на папку
                                            viewModel.loadFamilyStorage(folder.id, family.toInt())
                                            println("Clicked document: ${folder.name}")
                                        }
                                )
                                Divider()
                            }
                        }
                        itemsIndexed(documents) { index, doc ->
                            Box {
                                ListItem(
                                    headlineContent = { Text(doc.name) },
                                    leadingContent = {
                                        Icon(painterResource(id = R.drawable.description_24dp_1f1f1f_fill0_wght400_grad0_opsz24), contentDescription = "Документ")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedIndex = index
                                        }
                                )

                                // Показываем меню только для выбранного документа
                                DropdownMenu(
                                    expanded = expandedIndex == index,
                                    onDismissRequest = { expandedIndex = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Открыть") },
                                        onClick = {
                                            expandedIndex = null
                                            // TODO: открыть документ
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Удалить") },
                                        onClick = {
                                            expandedIndex = null
                                            // TODO: удалить документ
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Поделиться") },
                                        onClick = {
                                            expandedIndex = null
                                            // TODO: поделиться документом
                                        }
                                    )
                                    if(isOnline == true) {
                                        DropdownMenuItem(
                                            text = { Text("Скачать") },
                                            onClick = {
                                                expandedIndex = null
                                                // TODO: скачать документ
                                                viewModel.downloadDocument(context,doc)
                                            }
                                        )}
                                }
                            }
                            Divider()
                        }
                    }
                }}
        }
    }
}
