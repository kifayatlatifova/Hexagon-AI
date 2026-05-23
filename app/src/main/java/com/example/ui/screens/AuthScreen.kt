package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val currentLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Active simulated Social OAuth Dialog
    var activeSocialProvider by remember { mutableStateOf<String?>(null) } // "google", "github", "facebook", "instagram"
    var socialEmail by remember { mutableStateOf("") }
    var socialName by remember { mutableStateOf("") }

    val translations = AUTH_TRANSLATIONS[currentLanguage] ?: AUTH_TRANSLATIONS["ru"]!!

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SoftBgBlue)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hexagon AI",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = DeepNavyText
            )

            Text(
                text = translations["subtitle"] ?: "",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Tabs for Sign In vs Registration
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BorderSoftBlue)
                    .padding(4.dp)
            ) {
                val loginLabel = translations["tab_login"] ?: ""
                val registerLabel = translations["tab_register"] ?: ""

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLoginMode) PureWhite else Color.Transparent)
                        .clickable {
                            isLoginMode = true
                            errorMessage = null
                            successMessage = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = loginLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isLoginMode) RoyalBlueMain else DeepNavyText
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isLoginMode) PureWhite else Color.Transparent)
                        .clickable {
                            isLoginMode = false
                            errorMessage = null
                            successMessage = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = registerLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (!isLoginMode) RoyalBlueMain else DeepNavyText
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Authentication Form Cards
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    successMessage?.let {
                        Text(
                            text = it,
                            color = Color(0xFF2E7D32),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    if (!isLoginMode) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text(translations["label_name"] ?: "") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalBlueMain,
                                unfocusedBorderColor = BorderSoftBlue,
                                focusedLabelColor = RoyalBlueMain
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(translations["label_email"] ?: "") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_field"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlueMain,
                            unfocusedBorderColor = BorderSoftBlue,
                            focusedLabelColor = RoyalBlueMain
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(translations["label_password"] ?: "") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_field"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlueMain,
                            unfocusedBorderColor = BorderSoftBlue,
                            focusedLabelColor = RoyalBlueMain
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Refresh else Icons.Default.Lock,
                                    contentDescription = "Toggle password"
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            errorMessage = null
                            successMessage = null
                            if (email.isBlank() || password.isBlank() || (!isLoginMode && displayName.isBlank())) {
                                errorMessage = translations["error_empty_fields"] ?: "Заполните все поля"
                                return@Button
                            }
                            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                errorMessage = translations["error_invalid_email"] ?: "Некорректный email"
                                return@Button
                            }
                            if (password.length < 6) {
                                errorMessage = translations["error_short_password"] ?: "Мин. 6 символов"
                                return@Button
                            }

                            if (isLoginMode) {
                                viewModel.loginWithEmail(email, password) { success, msg ->
                                    if (success) {
                                        successMessage = translations["success_login"]
                                    } else {
                                        errorMessage = when(msg) {
                                            "User not found" -> translations["error_user_not_found"]
                                            "Incorrect password" -> translations["error_wrong_pwd"]
                                            else -> msg
                                        }
                                    }
                                }
                            } else {
                                viewModel.registerWithEmail(email, password, displayName) { success, msg ->
                                    if (success) {
                                        successMessage = translations["success_registered"]
                                    } else {
                                        errorMessage = when(msg) {
                                            "User already exists" -> translations["error_exists"]
                                            else -> msg
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueMain),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isLoginMode) (translations["btn_login"] ?: "") else (translations["btn_register"] ?: ""),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PureWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Social Logins Label Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1.5f), color = BorderSoftBlue)
                Text(
                    text = translations["label_or_social"] ?: "или войдите с помощью",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1.5f), color = BorderSoftBlue)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Social login buttons grid supporting: Google, GitHub, Facebook, Instagram
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Google
                SocialLoginButton(
                    icon = Icons.Default.Share, // Modern symbol fallback for Google circular G
                    color = Color(0xFFDB4437),
                    name = "Google",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        socialEmail = "user.google@gmail.com"
                        socialName = "Google User"
                        activeSocialProvider = "google"
                    }
                )

                // GitHub
                SocialLoginButton(
                    icon = Icons.Default.Build, // Github repo symbol fallback
                    color = Color(0xFF181717),
                    name = "GitHub",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        socialEmail = "octocat.github@gmail.com"
                        socialName = "GitHub Developer"
                        activeSocialProvider = "github"
                    }
                )

                // Facebook
                SocialLoginButton(
                    icon = Icons.Default.Send, // Facebook messaging signal symbol
                    color = Color(0xFF1877F2),
                    name = "Facebook",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        socialEmail = "user.facebook@fb.com"
                        socialName = "Facebook Member"
                        activeSocialProvider = "facebook"
                    }
                )

                // Instagram
                SocialLoginButton(
                    icon = Icons.Default.Favorite, // Instagram creative camera passion fallback
                    color = Color(0xFFE1306C),
                    name = "Instagram",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        socialEmail = "creator.instagram@insta.com"
                        socialName = "Instagram Creator"
                        activeSocialProvider = "instagram"
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom App Language selector links
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "ru" to "RU",
                    "en" to "EN",
                    "az" to "AZ"
                ).forEach { (langId, label) ->
                    val isSelected = currentLanguage == langId
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) RoyalBlueMain else Color.Gray,
                        modifier = Modifier
                            .clickable {
                                viewModel.setAppLanguage(langId)
                            }
                            .padding(4.dp)
                    )
                }
            }
        }
    }

    // High fidelity Interactive OAuth popups
    activeSocialProvider?.let { provider ->
        val providerColor = when(provider) {
            "google" -> Color(0xFFDB4437)
            "github" -> Color(0xFF181717)
            "facebook" -> Color(0xFF1877F2)
            else -> Color(0xFFE1306C)
        }

        val providerUpper = provider.replaceFirstChar { it.uppercase() }

        Dialog(onDismissRequest = { activeSocialProvider = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PureWhite,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$providerUpper OAuth Portal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = providerColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = translations["oauth_desc"]?.replace("{p}", providerUpper) ?: "",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = socialName,
                        onValueChange = { socialName = it },
                        label = { Text(translations["label_oauth_name"] ?: "Имя") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = providerColor,
                            focusedLabelColor = providerColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = socialEmail,
                        onValueChange = { socialEmail = it },
                        label = { Text(translations["label_oauth_email"] ?: "Email") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = providerColor,
                            focusedLabelColor = providerColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (socialEmail.isNotBlank() && socialName.isNotBlank()) {
                                viewModel.loginWithSocial(socialEmail, provider, socialName)
                                activeSocialProvider = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = providerColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = translations["oauth_authorize"] ?: "Авторизовать",
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { activeSocialProvider = null }) {
                        Text(
                            translations["oauth_cancel"] ?: "Отмена",
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SocialLoginButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.2.dp, BorderSoftBlue),
        color = PureWhite,
        modifier = modifier
            .height(50.dp)
            .testTag("oauth_btn_${name.lowercase()}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Sign in with $name",
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private val AUTH_TRANSLATIONS = mapOf(
    "ru" to mapOf(
        "subtitle" to "Вход в учетную запись Hexagon AI",
        "tab_login" to "Вход",
        "tab_register" to "Регистрация",
        "label_name" to "Ваше Имя",
        "label_email" to "Электронная почта",
        "label_password" to "Пароль",
        "btn_login" to "Войти",
        "btn_register" to "Создать аккаунт",
        "label_or_social" to "или продолжите с помощью",
        "success_login" to "Вход успешно выполнен!",
        "success_registered" to "Регистрация завершена!",
        "error_empty_fields" to "Заполните все необходимые поля",
        "error_invalid_email" to "Введите корректный почтовый адрес",
        "error_short_password" to "Пароль должен быть не менее 6 символов",
        "error_user_not_found" to "Пользователь с такой почтой не найден",
        "error_wrong_pwd" to "Неверный пароль. Попробуйте ещё раз",
        "error_exists" to "Пользователь с таким email уже зарегистрирован",
        "oauth_desc" to "Вы собираетесь подключить свой профиль {p} к Hexagon AI. Пожалуйста, введите данные OAuth для симуляции:",
        "label_oauth_name" to "Полное имя",
        "label_oauth_email" to "Электронная почта профиля",
        "oauth_authorize" to "Авторизовать профиль",
        "oauth_cancel" to "Отмена"
    ),
    "en" to mapOf(
        "subtitle" to "Access your Hexagon AI Account",
        "tab_login" to "Sign In",
        "tab_register" to "Sign Up",
        "label_name" to "Full Name",
        "label_email" to "Email Address",
        "label_password" to "Password",
        "btn_login" to "Log In",
        "btn_register" to "Create Account",
        "label_or_social" to "or continue with",
        "success_login" to "Login completed successfully!",
        "success_registered" to "Account registered successfully!",
        "error_empty_fields" to "Please fill in all information fields",
        "error_invalid_email" to "Please provide a valid email format",
        "error_short_password" to "Password must contain at least 6 characters",
        "error_user_not_found" to "No account found matching this email",
        "error_wrong_pwd" to "Incorrect password. Please try again",
        "error_exists" to "An account with this email is already registered",
        "oauth_desc" to "You are connecting your {p} identity context to Hexagon AI. Provide simulated credentials to authorize:",
        "label_oauth_name" to "Display Name",
        "label_oauth_email" to "Profile Email",
        "oauth_authorize" to "Confirm Connections",
        "oauth_cancel" to "Cancel"
    ),
    "az" to mapOf(
        "subtitle" to "Hexagon AI hesabınıza giriş",
        "tab_login" to "Giriş",
        "tab_register" to "Qeydiyyat",
        "label_name" to "Adınız",
        "label_email" to "E-poçt ünvanı",
        "label_password" to "Şifrə",
        "btn_login" to "Daxil ol",
        "btn_register" to "Hesab yarat",
        "label_or_social" to "və ya social şəbəkə ilə",
        "success_login" to "Giriş uğurla tamamlandı!",
        "success_registered" to "Qeydiyyat tamamlandı!",
        "error_empty_fields" to "Zəhmət olmasa bütün xanaları doldurun",
        "error_invalid_email" to "Düzgün e-poçt formatı daxil edin",
        "error_short_password" to "Şifrə uzunluğu ən azı 6 simvol olmalıdır",
        "error_user_not_found" to "Bu e-poçt ilə daxil edilmiş profil tapılmadı",
        "error_wrong_pwd" to "Yalnış şifrə. Yenidən cəhd edin",
        "error_exists" to "Bu e-poçt ünvanı ilə artıq qeydiyyat mövcuddur",
        "oauth_desc" to "{p} hesabınızı Hexagon AI tətbiqinə bağlamaq üzrəsiniz. Simulyasiya üçün məlumatları daxil edin:",
        "label_oauth_name" to "Tam adınız",
        "label_oauth_email" to "Hesab E-poçtu",
        "oauth_authorize" to "Hesabı bağla",
        "oauth_cancel" to "Ləğv et"
    )
)
