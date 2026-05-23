package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
                    isResponding = isResponding
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
    isResponding: Boolean
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
        "close_btn" to "Закрыть"
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
        "close_btn" to "Close"
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
        "close_btn" to "Bağla"
    )
)

private fun translate(key: String, lang: String): String {
    val langMap = TRANSLATIONS[lang] ?: TRANSLATIONS["ru"]!!
    return langMap[key] ?: key
}
