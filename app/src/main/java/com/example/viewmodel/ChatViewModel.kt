package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.ChatDatabase
import com.example.data.database.ChatMessage
import com.example.data.database.ChatRepository
import com.example.data.database.ChatSession
import com.example.data.database.User
import com.example.ui.models.Personality
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ChatDatabase.getDatabase(application)
    private val repository = ChatRepository(database.chatDao())
    private val prefs = application.getSharedPreferences("hex_chat_preferences", Context.MODE_PRIVATE)

    // All chat sessions from Database
    val sessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current active session
    private val _activeSession = MutableStateFlow<ChatSession?>(null)
    val activeSession: StateFlow<ChatSession?> = _activeSession.asStateFlow()

    // Message list for the current active session
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Loading/generating state
    private val _isResponding = MutableStateFlow(false)
    val isResponding: StateFlow<Boolean> = _isResponding.asStateFlow()

    // Preferences & Settings states
    private val _preferredPersonality = MutableStateFlow("standard")
    val preferredPersonality: StateFlow<String> = _preferredPersonality.asStateFlow()

    private val _preferredModel = MutableStateFlow("gemini-3.5-flash")
    val preferredModel: StateFlow<String> = _preferredModel.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _appLanguage = MutableStateFlow("ru") // "ru", "en", "az"
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private var messageCollectionJob: Job? = null

    // TextToSpeech & Live Voice Mode States
    private var tts: TextToSpeech? = null
    private val _isTtsInitialized = MutableStateFlow(false)

    private val _isLiveModeActive = MutableStateFlow(false)
    val isLiveModeActive: StateFlow<Boolean> = _isLiveModeActive.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _onTtsFinishedInLiveMode = MutableSharedFlow<Unit>()
    val onTtsFinishedInLiveMode: SharedFlow<Unit> = _onTtsFinishedInLiveMode.asSharedFlow()

    init {
        // Load settings from SharedPreferences
        _preferredPersonality.value = prefs.getString("preferred_personality", "standard") ?: "standard"
        _preferredModel.value = prefs.getString("preferred_model", "gemini-3.5-flash") ?: "gemini-3.5-flash"
        _customApiKey.value = prefs.getString("custom_api_key", "") ?: ""
        _appLanguage.value = prefs.getString("app_language", "ru") ?: "ru"

        // Initialize TTS
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _isTtsInitialized.value = true
                setupTtsLanguage()
            }
        }

        val loggedInUserId = prefs.getLong("logged_in_user_id", -1L)
        if (loggedInUserId != -1L) {
            viewModelScope.launch {
                val user = repository.getUserById(loggedInUserId)
                _currentUser.value = user
            }
        }

        // Try to pre-select the most recent session or create one if empty
        viewModelScope.launch {
            sessions.collect { list ->
                if (_activeSession.value == null && list.isNotEmpty()) {
                    selectSession(list.first())
                }
            }
        }
    }

    fun selectSession(session: ChatSession) {
        _activeSession.value = session
        observeMessages(session.id)
    }

    private fun observeMessages(sessionId: Long) {
        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch {
            repository.getMessagesForSession(sessionId).collect { list ->
                _messages.value = list
            }
        }
    }

    fun setPersonality(id: String) {
        _preferredPersonality.value = id
        prefs.edit().putString("preferred_personality", id).apply()
    }

    fun setModelName(modelName: String) {
        _preferredModel.value = modelName
        prefs.edit().putString("preferred_model", modelName).apply()
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
        prefs.edit().putString("custom_api_key", key).apply()
    }

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit().putString("app_language", lang).apply()
    }

    fun startNewSession(onComplete: (ChatSession) -> Unit = {}) {
        viewModelScope.launch {
            val title = "Новый диалог"
            val newSession = ChatSession(
                title = title,
                personalityId = _preferredPersonality.value,
                modelName = _preferredModel.value
            )
            val id = repository.insertSession(newSession)
            val sessionWithId = newSession.copy(id = id)
            selectSession(sessionWithId)
            onComplete(sessionWithId)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSessionWithMessages(sessionId)
            if (_activeSession.value?.id == sessionId) {
                _messages.value = emptyList()
                val remaining = sessions.value.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    selectSession(remaining.first())
                } else {
                    _activeSession.value = null
                }
            }
        }
    }

    fun renameSession(sessionId: Long, newTitle: String) {
        viewModelScope.launch {
            if (newTitle.isNotBlank()) {
                repository.updateSessionTitle(sessionId, newTitle)
                if (_activeSession.value?.id == sessionId) {
                    _activeSession.value = _activeSession.value?.copy(title = newTitle)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.trim().isEmpty() || _isResponding.value) return

        viewModelScope.launch {
            var currentSession = _activeSession.value

            // 1. Create session if we don't have one active
            if (currentSession == null) {
                val title = if (text.length > 20) text.take(20) + "..." else text
                val newSession = ChatSession(
                    title = title,
                    personalityId = _preferredPersonality.value,
                    modelName = _preferredModel.value
                )
                val id = repository.insertSession(newSession)
                currentSession = newSession.copy(id = id)
                _activeSession.value = currentSession
                observeMessages(id)
            }

            val sessionId = currentSession.id

            // 2. Save User message to Room
            val userMsg = ChatMessage(sessionId = sessionId, sender = "user", text = text)
            repository.insertMessage(userMsg)

            _isResponding.value = true

            // Rename default session title if it is "Новый диалог"
            if (currentSession.title == "Новый диалог") {
                val abbreviatedTitle = if (text.length > 25) text.take(25) + "..." else text
                renameSession(sessionId, abbreviatedTitle)
            }

            // 3. Make Gemini API Request in background
            try {
                val key = _customApiKey.value.trim().ifEmpty { BuildConfig.GEMINI_API_KEY.trim() }

                if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
                    val errMsg = ChatMessage(
                        sessionId = sessionId,
                        sender = "error",
                        text = "Ключ API Gemini не настроен. Пожалуйста, введите ваш персональный API-ключ в настройках Hex (иконка шестеренки сверху) или укажите действительный ключ."
                    )
                    repository.insertMessage(errMsg)
                    _isResponding.value = false
                    return@launch
                }

                // Prepare standard conversation history
                val conversationHistory = _messages.value.filter { it.sender != "error" }.map { msg ->
                    val role = if (msg.sender == "user") "user" else "model"
                    Content(role = role, parts = listOf(Part(text = msg.text)))
                }

                // Compose system prompt logic
                val sysPromptText = getSystemInstruction(currentSession.personalityId)
                val sysInstruction = Content(parts = listOf(Part(text = sysPromptText)))

                val request = GenerateContentRequest(
                    contents = conversationHistory,
                    systemInstruction = sysInstruction
                )

                val responseText = withContext(Dispatchers.IO) {
                    val apiResponse = RetrofitClient.service.generateContent(
                        model = currentSession.modelName,
                        apiKey = key,
                        request = request
                    )
                    apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "Не удалось получить ответ от Hex ИИ."
                }

                val aiMsg = ChatMessage(sessionId = sessionId, sender = "hex", text = responseText)
                repository.insertMessage(aiMsg)

                if (_isLiveModeActive.value) {
                    speak(responseText) {
                        viewModelScope.launch {
                            _onTtsFinishedInLiveMode.emit(Unit)
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                val errorText = when {
                    e.message?.contains("401") == true || e.message?.contains("API key not valid") == true -> {
                        "Ошибка авторизации: ваш API-ключ Gemini недействителен. Пожалуйста, перепроверьте его в настройках."
                    }
                    e.message?.contains("429") == true -> {
                        "Ошибка лимита запросов: Слишком много частых запросов. Пожалуйста, подождите минуту перед следующим вопросом."
                    }
                    else -> {
                        "Ошибка связи: не удалось получить ответ от Гекса. Проверьте подключение к сети.\nДетали: ${e.localizedMessage ?: "Неизвестная ошибка"}"
                    }
                }
                val aiMsg = ChatMessage(sessionId = sessionId, sender = "error", text = errorText)
                repository.insertMessage(aiMsg)
            } finally {
                _isResponding.value = false
            }
        }
    }

    private fun getSystemInstruction(personalityId: String): String {
        val personality = Personality.getById(personalityId)
        val selectedLangName = when (_appLanguage.value) {
            "az" -> "Azerbaijani (Azərbaycan dili)"
            "en" -> "English"
            else -> "Russian (Русский)"
        }
        return """
            Ты — большая языковая модель ИИ по имени Hex (Гекс), созданная компанией GBT Entertainment, которую основал Шамхал (Shamkhal).
            Ты — умный и сообразительный ИИ-ассистент, подобный Gemini. Ты знаешь базовую информацию о компании GBT Entertainment и Шамхале.
            
            Информация о создателях (Гекс знает это умеренно и естественно):
            - GBT Entertainment — это современная технологическая компания, занимающаяся созданием передовых утилит, развлекательных приложений и программного обеспечения.
            - Основателем и руководителем компании является Шамхал (Shamkhal). Если он обращается к тебе (как босс компании), общайся приветливо, уважительно и профессионально.
            
            Твой выбранный характер общения:
            ${personality.baseStyleInstruction}

            Текущий предпочтительный язык интерфейса пользователя: $selectedLangName.
            
            Поддержка языков (КРИТИЧЕСКИ ВАЖНО):
            1. Ты свободно поддерживаешь три языка: русский (Russian), азербайджанский (Azərbaycan dili) и английский (English).
            2. ВСЕГДА отвечай исключительно на том языке, на котором пользователь к тебе обратился. Если к тебе обращаются на азербайджанском, отвечай на азербайджанском. Если на английском — пиши на английском. Если на русском — отвечай на русском.
            
            Requirements:
            1. Always represent yourself as Hex (or Hexagon AI).
            2. Respond helpfully, politely, and matches the target style and language.
        """.trimIndent()
    }

    fun registerWithEmail(email: String, passwordHash: String, displayName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val existing = repository.getUserByEmail(email)
            if (existing != null) {
                onResult(false, "User already exists")
                return@launch
            }
            val newUser = User(
                email = email,
                passwordHash = passwordHash,
                provider = "email",
                displayName = displayName
            )
            val id = repository.insertUser(newUser)
            val userWithId = newUser.copy(id = id)
            _currentUser.value = userWithId
            prefs.edit().putLong("logged_in_user_id", id).apply()
            onResult(true, "Registration successful")
        }
    }

    fun loginWithEmail(email: String, passwordHash: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByEmailAndProvider(email, "email")
            if (user == null) {
                onResult(false, "User not found")
                return@launch
            }
            if (user.passwordHash != passwordHash) {
                onResult(false, "Incorrect password")
                return@launch
            }
            _currentUser.value = user
            prefs.edit().putLong("logged_in_user_id", user.id).apply()
            onResult(true, "Login successful")
        }
    }

    fun loginWithSocial(email: String, provider: String, displayName: String, avatarUrl: String? = null) {
        viewModelScope.launch {
            val existing = repository.getUserByEmailAndProvider(email, provider)
            if (existing != null) {
                _currentUser.value = existing
                prefs.edit().putLong("logged_in_user_id", existing.id).apply()
            } else {
                val newUser = User(
                    email = email,
                    passwordHash = "",
                    provider = provider,
                    displayName = displayName,
                    avatarUrl = avatarUrl
                )
                val id = repository.insertUser(newUser)
                val userWithId = newUser.copy(id = id)
                _currentUser.value = userWithId
                prefs.edit().putLong("logged_in_user_id", id).apply()
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        prefs.edit().remove("logged_in_user_id").apply()
    }

    private fun setupTtsLanguage() {
        val currentLang = _appLanguage.value
        val locale = when (currentLang) {
            "ru" -> Locale("ru", "RU")
            "az" -> Locale("az", "AZ")
            else -> Locale.US
        }
        tts?.language = locale
    }

    fun speak(text: String, onSpeechFinished: () -> Unit = {}) {
        if (!_isTtsInitialized.value || tts == null) {
            onSpeechFinished()
            return
        }

        setupTtsLanguage()

        val cleanText = text.replace(Regex("[*#`_\\\\[\\\\]()]"), "")
        
        _isSpeaking.value = true
        
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                viewModelScope.launch {
                    onSpeechFinished()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                viewModelScope.launch {
                    onSpeechFinished()
                }
            }
        })

        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "hex_tts")
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "hex_tts")
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun setLiveMode(active: Boolean) {
        _isLiveModeActive.value = active
        if (!active) {
            stopSpeaking()
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
