package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ChatMessage
import com.example.data.database.ChatSession
import com.example.ui.models.Personality
import com.example.ui.theme.*
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isResponding by viewModel.isResponding.collectAsStateWithLifecycle()

    val currentPersonalityId by viewModel.preferredPersonality.collectAsStateWithLifecycle()
    val currentModelName by viewModel.preferredModel.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf<ChatSession?>(null) }
    var newSessionTitle by remember { mutableStateOf("") }

    val activePersonality = Personality.getById(activeSession?.personalityId ?: currentPersonalityId)

    val context = LocalContext.current
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = results?.firstOrNull() ?: ""
                if (spokenText.isNotBlank()) {
                    messageText = spokenText
                }
            }
        }
    )

    val startSpeechToText = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val langLocale = when (currentLanguage) {
                "ru" -> "ru-RU"
                "az" -> "az-AZ"
                else -> "en-US"
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langLocale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langLocale)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, langLocale)
            putExtra(RecognizerIntent.EXTRA_PROMPT, when (currentLanguage) {
                "ru" -> "Говорите..."
                "az" -> "Danışın..."
                else -> "Speak now..."
            })
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                when (currentLanguage) {
                    "ru" -> "Голосовой ввод не поддерживается на вашем устройстве"
                    "az" -> "Səsli daxiletmə cihazınızda dəstəklənmir"
                    else -> "Voice input is not supported on your device"
                },
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val isLiveModeActive by viewModel.isLiveModeActive.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()

    val liveModeSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = results?.firstOrNull() ?: ""
                if (spokenText.isNotBlank()) {
                    viewModel.sendMessage(spokenText)
                }
            }
        }
    )

    val startLiveSpeechToText: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val langLocale = when (currentLanguage) {
                "ru" -> "ru-RU"
                "az" -> "az-AZ"
                else -> "en-US"
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langLocale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langLocale)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, langLocale)
            putExtra(RecognizerIntent.EXTRA_PROMPT, when (currentLanguage) {
                "ru" -> "Говорите..."
                "az" -> "Danışın..."
                else -> "Speak now..."
            })
        }
        try {
            liveModeSpeechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                when (currentLanguage) {
                    "ru" -> "Голосовой ввод не поддерживается на вашем устройстве"
                    "az" -> "Səsli daxiletmə cihazınızda dəstəklənmir"
                    else -> "Voice input is not supported on your device"
                },
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(isLiveModeActive) {
        if (isLiveModeActive) {
            startLiveSpeechToText()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onTtsFinishedInLiveMode.collect {
            if (viewModel.isLiveModeActive.value) {
                startLiveSpeechToText()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(310.dp)
                    .fillMaxHeight(),
                drawerContainerColor = PureWhite,
                drawerTonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header Studio Info
                    Text(
                        text = translate("app_title", currentLanguage),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlueMain,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = translate("gbt_projects", currentLanguage),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Buttons
                    Button(
                        onClick = {
                            viewModel.startNewSession()
                            scope.launch { drawerState.close() }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalBlueMain,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("new_chat_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = translate("new_chat", currentLanguage))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(translate("new_chat", currentLanguage), fontWeight = FontWeight.SemiBold)
                    }

                    Text(
                        text = translate("chat_history", currentLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DeepNavyText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Sessions/Dialogue List
                    Box(modifier = Modifier.weight(1f)) {
                        if (sessions.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = translate("history_empty", currentLanguage),
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(sessions, key = { it.id }) { session ->
                                    val isSelected = activeSession?.id == session.id
                                    val sessionPersonality = Personality.getById(session.personalityId)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) SoftBgBlue else Color.Transparent)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) BorderSoftBlue else Color.Transparent,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                viewModel.selectSession(session)
                                                scope.launch { drawerState.close() }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sessionPersonality.emoji,
                                            fontSize = 18.sp,
                                            modifier = Modifier.padding(end = 10.dp)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = session.title,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                color = if (isSelected) RoyalBlueMain else DeepNavyText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = session.modelName.replace("gemini-", ""),
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                maxLines = 1
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                newSessionTitle = session.title
                                                showRenameDialog = session
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = translate("rename_session_desc", currentLanguage),
                                                tint = Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteSession(session.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = translate("delete_session_desc", currentLanguage),
                                                tint = Color.LightGray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // GBT Entertainment Creator info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SoftBgBlue)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "GBT Entertainment",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = RoyalBlueMain
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = translate("creator_shamkhal", currentLanguage),
                                fontSize = 12.sp,
                                color = DeepNavyText,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Active User Profile section
                    val userState by viewModel.currentUser.collectAsStateWithLifecycle()
                    userState?.let { user ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("drawer_user_profile_card"),
                            colors = CardDefaults.cardColors(containerColor = SoftBgBlue),
                            border = BorderStroke(1.dp, BorderSoftBlue)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // User Icon or first character of display name
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(RoyalBlueMain),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.displayName.take(1).uppercase(),
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(10.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = DeepNavyText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = user.email,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                IconButton(
                                    onClick = { viewModel.logout() },
                                    modifier = Modifier.size(28.dp).testTag("drawer_logout_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = "Sign Out",
                                        tint = Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            val defaultTitle = translate("default_chat_title", currentLanguage)
                            Text(
                                text = activeSession?.title ?: defaultTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${translate("mode_prefix", currentLanguage)} ${activePersonality.emoji} ${activePersonality.name}",
                                fontSize = 11.sp,
                                color = AccentSkyBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = RoyalBlueMain
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.setLiveMode(true) },
                            modifier = Modifier.testTag("action_live_mode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Live Voice Mode",
                                tint = RoyalBlueMain
                            )
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = RoyalBlueMain
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PureWhite,
                        titleContentColor = DeepNavyText
                    )
                )
            },
            containerColor = SoftBgBlue
        ) { innerPadding ->
            val listState = rememberLazyListState()

            // Scroll to bottom when messages list size changes
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (messages.isEmpty()) {
                    WelcomeStateView(
                        onSuggestionClick = { text ->
                            messageText = ""
                            viewModel.sendMessage(text)
                        },
                        currentPersonality = activePersonality,
                        currentLanguage = currentLanguage
                    )
                } else {
                    // Chat messages list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 90.dp), // Height of input panel
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                    ) {
                        items(messages) { message ->
                            MessageBubble(message = message, personality = activePersonality)
                        }

                        if (isResponding) {
                            item {
                                TypingIndicator(personality = activePersonality, currentLanguage = currentLanguage)
                            }
                        }
                    }
                }

                // Input box anchored to bottom
                InputPanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    text = messageText,
                    currentLanguage = currentLanguage,
                    onTextChanged = { messageText = it },
                    onSend = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    },
                    isResponding = isResponding,
                    onVoiceClick = startSpeechToText
                )
            }
        }
    }

    // Dynamic dialogue custom name rename dialog
    if (showRenameDialog != null) {
        val sessionToRename = showRenameDialog!!
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text(translate("dialog_rename_title", currentLanguage)) },
            text = {
                OutlinedTextField(
                    value = newSessionTitle,
                    onValueChange = { newSessionTitle = it },
                    label = { Text(translate("dialog_rename_label", currentLanguage)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalBlueMain,
                        cursorColor = RoyalBlueMain
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameSession(sessionToRename.id, newSessionTitle)
                        showRenameDialog = null
                    }
                ) {
                    Text(translate("dialog_rename_save", currentLanguage), color = RoyalBlueMain)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text(translate("dialog_rename_cancel", currentLanguage), color = Color.Gray)
                }
            }
        )
    }

    // Settings panel for Hex personalities, API key and Models
    if (showSettingsDialog) {
        SettingsDialog(
            currentPersonalityId = currentPersonalityId,
            currentModelName = currentModelName,
            customApiKey = customApiKey,
            currentLanguage = currentLanguage,
            onSavePersonality = { viewModel.setPersonality(it) },
            onSaveModel = { viewModel.setModelName(it) },
            onSaveApiKey = { viewModel.setCustomApiKey(it) },
            onSaveLanguage = { viewModel.setAppLanguage(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (isLiveModeActive) {
        LiveVoiceOverlay(
            viewModel = viewModel,
            currentLanguage = currentLanguage,
            isResponding = isResponding,
            isSpeaking = isSpeaking,
            onClose = { viewModel.setLiveMode(false) },
            onRetryListen = { startLiveSpeechToText() }
        )
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    personality: Personality
) {
    val isUser = message.sender == "user"
    val isError = message.sender == "error"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!isUser) {
                // Hex Icon Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(RoyalBlueMain)
                        .padding(bottom = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isError) "⚠️" else personality.emoji,
                        fontSize = 15.sp,
                        color = PureWhite
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 16.dp
                        )
                    )
                    .background(
                        when {
                            isUser -> RoyalBlueMain
                            isError -> Color(0xFFFDE8E8)
                            else -> PureWhite
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = when {
                            isUser -> Color.Transparent
                            isError -> Color(0xFFF8B4B4)
                            else -> BorderSoftBlue
                        },
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 16.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    color = when {
                        isUser -> PureWhite
                        isError -> Color(0xFF9B1C1C)
                        else -> DeepNavyText
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                // User symbol
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AccentSkyBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Вы",
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator(personality: Personality, currentLanguage: String) {
    Row(
        modifier = Modifier.fillMaxWidth(0.85f),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(RoyalBlueMain),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = personality.emoji,
                fontSize = 15.sp,
                color = PureWhite
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 2.dp))
                .background(PureWhite)
                .border(width = 1.dp, color = BorderSoftBlue, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 2.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = translate("typing_indicator", currentLanguage),
                color = Color.Gray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun WelcomeStateView(
    onSuggestionClick: (String) -> Unit,
    currentPersonality: Personality,
    currentLanguage: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = translate("welcome_header", currentLanguage),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DeepNavyText
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Info card about GBT & Shamkhal
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .border(1.dp, BorderSoftBlue, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = translate("about_title", currentLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = RoyalBlueMain
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = translate("about_desc", currentLanguage),
                    fontSize = 13.sp,
                    color = DeepNavyText,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = translate("select_prompt", currentLanguage),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Suggestions
        val suggestionsList = listOf(
            translate("prompt_tech_idea", currentLanguage),
            translate("prompt_inspiring_story", currentLanguage),
            translate("prompt_plan_week", currentLanguage)
        )

        suggestionsList.forEach { prompt ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { onSuggestionClick(prompt) },
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AccentSkyBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = prompt,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = DeepNavyText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun InputPanel(
    modifier: Modifier = Modifier,
    text: String,
    currentLanguage: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    isResponding: Boolean,
    onVoiceClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.5.dp, color = Color(0xFF424242), shape = RoundedCornerShape(24.dp))
            .testTag("input_panel_surface"),
        shape = RoundedCornerShape(24.dp),
        color = PureWhite,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChanged,
                placeholder = { Text(translate("input_placeholder", currentLanguage), fontSize = 14.sp, color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = DeepNavyText,
                    unfocusedTextColor = DeepNavyText,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = RoyalBlueMain
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        onSend()
                        keyboardController?.hide()
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("message_input_field")
            )

            IconButton(
                onClick = onVoiceClick,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(40.dp)
                    .testTag("voice_input_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Voice Input",
                    tint = RoyalBlueMain,
                    modifier = Modifier.size(24.dp)
                )
            }

            FloatingActionButton(
                onClick = {
                    onSend()
                    keyboardController?.hide()
                },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("send_button"),
                containerColor = RoyalBlueMain,
                contentColor = PureWhite,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                if (isResponding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = PureWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    currentPersonalityId: String,
    currentModelName: String,
    customApiKey: String,
    currentLanguage: String,
    onSavePersonality: (String) -> Unit,
    onSaveModel: (String) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onSaveLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPersonalityId by remember { mutableStateOf(currentPersonalityId) }
    var selectedModelName by remember { mutableStateOf(currentModelName) }
    var keyText by remember { mutableStateOf(customApiKey) }
    var keyVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            color = PureWhite,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = translate("settings_title", currentLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DeepNavyText
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = translate("close_btn", currentLanguage))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Language preference settings selector row
                Text(
                    text = translate("app_language_title", currentLanguage),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = RoyalBlueMain
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ru" to "Русский",
                        "en" to "English",
                        "az" to "Azərbaycanca"
                    ).forEach { (langId, displayTitle) ->
                        val isSelected = currentLanguage == langId
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onSaveLanguage(langId)
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) RoyalBlueMain else PureWhite,
                            border = BorderStroke(1.dp, if (isSelected) RoyalBlueMain else BorderSoftBlue)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayTitle,
                                    color = if (isSelected) PureWhite else DeepNavyText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Personality settings
                Text(
                    text = translate("select_personality", currentLanguage),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = RoyalBlueMain
                )

                Spacer(modifier = Modifier.height(8.dp))

                Personality.ALL_PERSONALITIES.forEach { p ->
                    val isSelected = selectedPersonalityId == p.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedPersonalityId = p.id
                                onSavePersonality(p.id)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) SoftBgBlue else PureWhite
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) RoyalBlueMain else BorderSoftBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = p.emoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = p.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = DeepNavyText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = p.description,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Choice of Model
                Text(
                    text = translate("select_model", currentLanguage),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = RoyalBlueMain
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "gemini-3.5-flash" to "3.5 Flash",
                        "gemini-3.1-pro-preview" to "3.1 Pro"
                    ).forEach { (modelId, displayTitle) ->
                        val isSelected = selectedModelName == modelId
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedModelName = modelId
                                    onSaveModel(modelId)
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) RoyalBlueMain else PureWhite,
                            border = BorderStroke(1.dp, if (isSelected) RoyalBlueMain else BorderSoftBlue)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayTitle,
                                    color = if (isSelected) PureWhite else DeepNavyText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. User Custom API Key overriding
                Text(
                    text = translate("api_key_title", currentLanguage),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = RoyalBlueMain
                )
                Text(
                    text = translate("api_key_desc", currentLanguage),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = keyText,
                    onValueChange = {
                        keyText = it
                        onSaveApiKey(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("AIzaSy...", fontSize = 12.sp) },
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalBlueMain,
                        cursorColor = RoyalBlueMain
                    ),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                imageVector = if (keyVisible) Icons.Default.Refresh else Icons.Default.Lock,
                                contentDescription = "Show/Hide"
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueMain),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(translate("btn_ready", currentLanguage), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private val TRANSLATIONS = mapOf(
    "ru" to mapOf(
        "app_title" to "Hexagon AI 🤔",
        "gbt_projects" to "Проекты GBT Entertainment",
        "new_chat" to "Новый диалог",
        "chat_history" to "История бесед",
        "history_empty" to "История пока пуста",
        "creator_shamkhal" to "Создатель: Шамхал",
        "default_chat_title" to "Диалог с Hexagon",
        "mode_prefix" to "Режим:",
        "welcome_header" to "Hexagon приветствует!",
        "about_title" to "О боте Hexagon AI 🌟",
        "about_desc" to "Hexagon AI — универсальная языковая модель искусственного интеллекта, разработанная для ответов на любые вопросы, написания текстов, генерации кода и помощи в повседневных задачах.",
        "select_prompt" to "Выберите вопрос для разгона беседы:",
        "prompt_tech_idea" to "Расскажи интересную технологическую идею 💡",
        "prompt_inspiring_story" to "Напиши короткую вдохновляющую историю 📖",
        "prompt_plan_week" to "Помоги составить план на неделю 📅",
        "input_placeholder" to "Напишите Hexagon...",
        "settings_title" to "Настройки Hexagon AI",
        "select_personality" to "Выберите личность бота:",
        "select_model" to "Версия модели ИИ:",
        "app_language_title" to "Язык приложения / App Language / Söhbət dili:",
        "api_key_title" to "Персональный API-ключ Gemini:",
        "api_key_placeholder" to "Введите свой собственный ключ (начиная с AIzaSy)",
        "api_key_desc" to "Если ключ не настроен или не работает, введите свой собственный ключ:",
        "btn_ready" to "Готово",
        "dialog_rename_title" to "Изменить название диалога",
        "dialog_rename_label" to "Название",
        "dialog_rename_save" to "Сохранить",
        "dialog_rename_cancel" to "Отмена",
        "delete_session_desc" to "Удалить беседу",
        "rename_session_desc" to "Переименовать",
        "typing_indicator" to "Hexagon печатает...",
        "close_btn" to "Закрыть",
        "live_mode" to "Live режим",
        "live_mode_desc" to "Голосовое общение с Hex",
        "live_mode_talking" to "Гекс говорит...",
        "live_mode_listening" to "Слушаю вас...",
        "live_mode_processing" to "Гекс думает...",
        "live_mode_tap_to_talk" to "Нажмите для записи"
    ),
    "en" to mapOf(
        "app_title" to "Hexagon AI 🤔",
        "gbt_projects" to "GBT Entertainment Projects",
        "new_chat" to "New Chat",
        "chat_history" to "Conversation History",
        "history_empty" to "No history yet",
        "creator_shamkhal" to "Creator: Shamkhal",
        "default_chat_title" to "Chat with Hexagon",
        "mode_prefix" to "Mode:",
        "welcome_header" to "Hexagon Welcomes You!",
        "about_title" to "About Hexagon AI 🌟",
        "about_desc" to "Hexagon AI is a general-purpose artificial intelligence language model built to answer questions, write creative copy, generate code, and assist with daily requests.",
        "select_prompt" to "Select a prompt to begin:",
        "prompt_tech_idea" to "Tell me an interesting tech idea 💡",
        "prompt_inspiring_story" to "Write a short inspiring story 📖",
        "prompt_plan_week" to "Help plan my week ahead 📅",
        "input_placeholder" to "Write to Hexagon...",
        "settings_title" to "Hexagon AI Settings",
        "select_personality" to "Select Bot Personality:",
        "select_model" to "AI Model Version:",
        "app_language_title" to "App Language / Язык приложения / Söhbət dili:",
        "api_key_title" to "Personal Gemini API Key:",
        "api_key_placeholder" to "Enter your custom key (starting with AIzaSy)",
        "api_key_desc" to "If the default key is not working, enter your own API key:",
        "btn_ready" to "Ready",
        "dialog_rename_title" to "Rename Conversation",
        "dialog_rename_label" to "Title",
        "dialog_rename_save" to "Save",
        "dialog_rename_cancel" to "Cancel",
        "delete_session_desc" to "Delete chat",
        "rename_session_desc" to "Rename",
        "typing_indicator" to "Hexagon is typing...",
        "close_btn" to "Close",
        "live_mode" to "Live Mode",
        "live_mode_desc" to "Voice conversation with Hex",
        "live_mode_talking" to "Hex is speaking...",
        "live_mode_listening" to "Listening...",
        "live_mode_processing" to "Hex is thinking...",
        "live_mode_tap_to_talk" to "Tap to Speak"
    ),
    "az" to mapOf(
        "app_title" to "Hexagon AI 🤔",
        "gbt_projects" to "GBT Entertainment Layihələri",
        "new_chat" to "Yeni söhbət",
        "chat_history" to "Söhbət tarixçəsi",
        "history_empty" to "Tarixçə hələ boşdur",
        "creator_shamkhal" to "Qurucu: Şamxal",
        "default_chat_title" to "Hexagon ilə söhbət",
        "mode_prefix" to "Rejim:",
        "welcome_header" to "Hexagon sizi salamlayır!",
        "about_title" to "Hexagon AI haqqında 🌟",
        "about_desc" to "Hexagon AI, suallara cavab vermək, mətn yazmaq, kod yaratmaq və gündəlik işlərdə kömək etmək üçün hazırlanmış universal süni intellekt dil modelidir.",
        "select_prompt" to "Söhbətə başlamaq üçün sual seçin:",
        "prompt_tech_idea" to "Maraqlı bir texnoloji ideya danış 💡",
        "prompt_inspiring_story" to "Qısa ruhlandırıcı hekayə yaz 📖",
        "prompt_plan_week" to "Həftəlik plan hazırlamağa kömək et 📅",
        "input_placeholder" to "Hexagon-a yazın...",
        "settings_title" to "Hexagon AI Tənzimləmələri",
        "select_personality" to "Botun şəxsiyyətini seçin:",
        "select_model" to "Süni İntellekt Model Versiyası:",
        "app_language_title" to "Tətbiq dili / App Language / Язык приложения:",
        "api_key_title" to "Fərdi Gemini API Açarı:",
        "api_key_placeholder" to "Öz xüsusi açarınızı daxil edin (AIzaSy ilə başlayır)",
        "api_key_desc" to "Əgər standart açar işləmirsə, öz fərdi API açarınızı daxil edin:",
        "btn_ready" to "Hazırdır",
        "dialog_rename_title" to "Söhbətin adını dəyiş",
        "dialog_rename_label" to "Başlıq",
        "dialog_rename_save" to "Yadda saxla",
        "dialog_rename_cancel" to "Ləğv et",
        "delete_session_desc" to "Söhbəti sil",
        "rename_session_desc" to "Adını dəyiş",
        "typing_indicator" to "Hexagon yazır...",
        "close_btn" to "Bağla",
        "live_mode" to "Canlı rejim",
        "live_mode_desc" to "Hex ilə səsli danışıq",
        "live_mode_talking" to "Hex danışır...",
        "live_mode_listening" to "Sizi dinləyirəm...",
        "live_mode_processing" to "Hex düşünür...",
        "live_mode_tap_to_talk" to "Danışmaq üçün toxunun"
    )
)

private fun translate(key: String, lang: String): String {
    val langMap = TRANSLATIONS[lang] ?: TRANSLATIONS["ru"]!!
    return langMap[key] ?: key
}

val Icons.Filled.Mic: ImageVector
    get() {
        if (_mic != null) {
            return _mic!!
        }
        _mic = ImageVector.Builder(
            name = "Filled.Mic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 14f)
                curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.66f, -1.34f, -3f, -3f, -3f)
                reflectiveCurveTo(9f, 3.34f, 9f, 5f)
                verticalLineToRelative(6f)
                curveToRelative(0f, 1.66f, 1.34f, 3f, 3f, 3f)
                close()
                moveTo(17.3f, 11f)
                curveToRelative(0f, 3f, -2.54f, 5.1f, -5.3f, 5.1f)
                reflectiveCurveTo(6.7f, 14f, 6.7f, 11f)
                horizontalLineTo(5f)
                curveToRelative(0f, 3.41f, 2.72f, 6.23f, 6f, 6.72f)
                verticalLineTo(21f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-3.28f)
                curveToRelative(3.28f, -0.48f, 6f, -3.3f, 6f, -6.72f)
                horizontalLineToRelative(-1.7f)
                close()
            }
        }.build()
        return _mic!!
    }

private var _mic: ImageVector? = null

@Composable
fun LiveVoiceOverlay(
    viewModel: ChatViewModel,
    currentLanguage: String,
    isResponding: Boolean,
    isSpeaking: Boolean,
    onClose: () -> Unit,
    onRetryListen: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val lastMessage = messages.lastOrNull()

    // Smooth pulsing scales for Radar Rings
    val infiniteTransition = rememberInfiniteTransition(label = "RadarAnimation")
    
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 3.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2"
    )

    val centerScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "center"
    )

    // Main layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E1B4B), // Indigo 950
                        Color(0xFF020617)  // Slate 950
                    )
                )
            )
            .testTag("live_voice_overlay_container"),
        contentAlignment = Alignment.Center
    ) {
        // Top controls
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = translate("live_mode", currentLanguage),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = translate("live_mode_desc", currentLanguage),
                        color = Color.LightGray.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .testTag("close_live_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Live Mode",
                        tint = Color.White
                    )
                }
            }

            // Center Dynamic Sphere and Animated Waves
            Box(
                modifier = Modifier
                    .size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Pulse Ring 2 (Only active when speaking or listening)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(if (isSpeaking || (!isResponding && !isSpeaking)) pulseScale2 else 1.0f)
                        .alpha(if (isSpeaking || (!isResponding && !isSpeaking)) pulseAlpha2 else 0.0f)
                        .background(
                            color = if (isSpeaking) RoyalBlueMain.copy(alpha = 0.35f) else AccentSkyBlue.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                )

                // Outer Pulse Ring 1
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(if (isSpeaking || (!isResponding && !isSpeaking)) pulseScale1 else 1.0f)
                        .alpha(if (isSpeaking || (!isResponding && !isSpeaking)) pulseAlpha1 else 0.0f)
                        .background(
                            color = if (isSpeaking) RoyalBlueMain.copy(alpha = 0.45f) else AccentSkyBlue.copy(alpha = 0.45f),
                            shape = CircleShape
                        )
                )

                // Main Central Button
                val glowColor = if (isResponding) Color(0xFFF59E0B) // Amber for thinking
                else if (isSpeaking) RoyalBlueMain // Royal Blue for speaking
                else AccentSkyBlue // Sky Blue for listening

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(if (isSpeaking || (!isResponding && !isSpeaking)) centerScale else 1.0f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    glowColor,
                                    glowColor.copy(alpha = 0.7f),
                                    Color.Transparent
                                )
                            )
                        )
                        .clickable {
                            if (!isSpeaking && !isResponding) {
                                onRetryListen()
                            }
                        }
                        .testTag("live_sphere_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            // Bottom Status and last text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                // Big Status Text
                val statusText = if (isResponding) {
                    translate("live_mode_processing", currentLanguage)
                } else if (isSpeaking) {
                    translate("live_mode_talking", currentLanguage)
                } else {
                    translate("live_mode_listening", currentLanguage)
                }

                Text(
                    text = statusText,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("live_status_text")
                )

                // Subtitle/Helper action
                if (!isResponding && !isSpeaking) {
                    Text(
                        text = "(${translate("live_mode_tap_to_talk", currentLanguage)})",
                        color = Color.LightGray.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Transcript Window (Show last chat transcription)
                if (lastMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp, max = 110.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.08f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (lastMessage.sender == "user") "Вы:" else "Hex:",
                                fontWeight = FontWeight.Bold,
                                color = if (lastMessage.sender == "user") AccentSkyBlue else Color(0xFF10B981),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = lastMessage.text,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
