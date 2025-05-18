package com.jh9kwm.Questionnaire

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.jh9kwm.Questionnaire.ui.theme.QuestionnaireTheme
import kotlinx.coroutines.delay
import android.Manifest

private val DefaultShapes = Shapes()
private val DefaultTypography = Typography()

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            QuestionnaireTheme {
                val context = this
                var locationStatus by remember { mutableStateOf("Requesting location...") }
                var micStatus by remember { mutableStateOf("Requesting microphone permission...") }

                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        fusedLocationClient.lastLocation.addOnSuccessListener {
                            locationStatus = it?.let { loc ->
                                "Location: ${loc.latitude}, ${loc.longitude}"
                            } ?: "No location data."
                        }
                    } else {
                        locationStatus = "Location denied"
                    }
                }

                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    micStatus = if (granted) "Microphone granted" else "Microphone denied"
                }

                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener {
                            locationStatus = it?.let { loc ->
                                "Location: ${loc.latitude}, ${loc.longitude}"
                            } ?: "No location data."
                        }
                    }

                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        micStatus = "Microphone granted"
                    }
                }
                AuthWrapper {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Location: $locationStatus", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Microphone: $micStatus", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        QuestionnaireApp()
                    }
                }
            }
        }
    }
}

@Composable
fun AuthWrapper(content: @Composable () -> Unit) {
    var user by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    var showSuccess by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val currentUser = FirebaseAuth.getInstance().currentUser

    if (user == null) {
        AuthScreen(
            onLogin = { email, password, onResult ->
                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        user = FirebaseAuth.getInstance().currentUser
                        showSuccess = true
                    }
                    .addOnFailureListener {
                        onResult(it.localizedMessage ?: "Unknown error while logging in.")
                    }
            },
            onRegister = { email, password, onResult ->
                FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        user = FirebaseAuth.getInstance().currentUser
                        showSuccess = true
                    }
                    .addOnFailureListener {
                        onResult(it.localizedMessage ?: "Unknown error while registering.")
                    }
            }
        )
    } else {
        LaunchedEffect(showSuccess) {
            if (showSuccess) {
                delay(150)
                visible = true
                delay(3000)
                visible = false
            }
        }

        Column {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically { -150 } + fadeIn(tween(600)),
                exit = slideOutVertically { -150 } + fadeOut(tween(400))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2E7D32))
                        .padding(16.dp)
                ) {
                    Text("✅ Successful login!", color = Color.White)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = {
                    FirebaseAuth.getInstance().signOut()
                    user = null
                }) {
                    Text("Log out")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showEmailDialog = true }) {
                    Text("Change Email")
                }
                Button(onClick = { showPasswordDialog = true }) {
                    Text("Change Password")
                }
                Button(onClick = { showDeleteDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Delete Account", color = Color.White)
                }
            }

            updateMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            content()

            // Email módosítási párbeszéd
            if (showEmailDialog) {
                var newEmail by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showEmailDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            currentUser?.updateEmail(newEmail)
                                ?.addOnSuccessListener {
                                    updateMessage = "✅ Email updated!"
                                    showEmailDialog = false
                                }
                                ?.addOnFailureListener {
                                    updateMessage = "❌ Error: ${it.localizedMessage}"
                                }
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEmailDialog = false }) {
                            Text("Cancel")
                        }
                    },
                    title = { Text("Change Email") },
                    text = {
                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = { newEmail = it },
                            label = { Text("New email") }
                        )
                    }
                )
            }

            // Jelszó módosítási párbeszéd
            if (showPasswordDialog) {
                var newPassword by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showPasswordDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            currentUser?.updatePassword(newPassword)
                                ?.addOnSuccessListener {
                                    updateMessage = "✅ Password updated!"
                                    showPasswordDialog = false
                                }
                                ?.addOnFailureListener {
                                    updateMessage = "❌ Error: ${it.localizedMessage}"
                                }
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPasswordDialog = false }) {
                            Text("Cancel")
                        }
                    },
                    title = { Text("Change Password") },
                    text = {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New password") }
                        )
                    }
                )
            }

            // Fiók törlésének megerősítése
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            currentUser?.delete()
                                ?.addOnSuccessListener {
                                    user = null
                                    updateMessage = "❌ Account deleted"
                                    showDeleteDialog = false
                                }
                                ?.addOnFailureListener {
                                    updateMessage = "❌ Error: ${it.localizedMessage}"
                                }
                        }) {
                            Text("Delete", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    },
                    title = { Text("Delete Account") },
                    text = { Text("Are you sure you want to delete your account? This cannot be undone.") }
                )
            }
        }
    }
}


@Composable
fun AuthScreen(
    onLogin: (String, String, (String?) -> Unit) -> Unit,
    onRegister: (String, String, (String?) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Welcome", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text("Log in or create an account", style = MaterialTheme.typography.bodyLarge)

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = null)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = { onLogin(email, password) { error -> errorMessage = error } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Log In", color = MaterialTheme.colorScheme.onPrimary)
                }

                OutlinedButton(
                    onClick = { onRegister(email, password) { error -> errorMessage = error } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Register")
                }
            }
        }
    }
}

@Composable
fun QuestionnaireApp(viewModel: QuestionnaireViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (uiState.submitted) {
            SubmissionScreen(
                answers = uiState.answers,
                onRestart = viewModel::restart
            )
        } else {
            QuestionnaireScreen(
                questions = uiState.questions,
                answers = uiState.answers,
                onAnswer = viewModel::updateAnswer,
                onSubmit = viewModel::submit
            )
        }
    }
}

@Composable
fun QuestionnaireScreen(
    questions: List<Question>,
    answers: Map<Int, String>,
    onAnswer: (Int, String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(questions) { question ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = question.text,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        when (question.type) {
                            QuestionType.TEXT -> {
                                OutlinedTextField(
                                    value = answers[question.id] ?: "",
                                    onValueChange = { input ->
                                        if (question.expectNumeric.not() || input.all { it.isDigit() }) {
                                            onAnswer(question.id, input)
                                        }
                                    },
                                    keyboardOptions = if (question.expectNumeric)
                                        KeyboardOptions(keyboardType = KeyboardType.Number)
                                    else
                                        KeyboardOptions.Default,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            QuestionType.SINGLE_CHOICE -> {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    question.options?.forEach { option ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = answers[question.id] == option,
                                                onClick = { onAnswer(question.id, option) }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(option)
                                        }
                                    }
                                }
                            }

                            QuestionType.MULTI_CHOICE -> {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val selectedOptions = answers[question.id]?.split(", ")?.toMutableSet() ?: mutableSetOf()
                                    question.options?.forEach { option ->
                                        val isSelected = option in selectedOptions
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    if (checked) selectedOptions.add(option) else selectedOptions.remove(option)
                                                    onAnswer(question.id, selectedOptions.joinToString(", "))
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(option)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .align(Alignment.End)
                .padding(8.dp)
        ) {
            Text("Submit")
        }
    }
}

@Composable
fun SubmissionScreen(answers: Map<Int, String>, onRestart: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing), label = "fade"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutBounce), label = "scale"
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "✅ Questionnaire sent successfully!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF2E7D32),
            modifier = Modifier
                .alpha(alphaAnim)
                .scale(scaleAnim)
                .padding(bottom = 24.dp)
        )

        answers.forEach { (id, answer) ->
            Text("${id + 1}. Answer: $answer")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onRestart) {
            Text("Answer again")
        }
    }
}

enum class QuestionType { TEXT, SINGLE_CHOICE, MULTI_CHOICE }

data class Question(
    val id: Int,
    val text: String,
    val type: QuestionType,
    val options: List<String>? = null,
    val expectNumeric: Boolean = false
)

data class QuestionnaireUiState(
    val questions: List<Question> = listOf(
        Question(0, "What's your favourite place you've ever visited?", QuestionType.TEXT),
        Question(1, "How old are you?", QuestionType.TEXT, expectNumeric = true),
        Question(2, "What's your favourite food?", QuestionType.TEXT),
        Question(3, "What's your favourite car brand?", QuestionType.TEXT),
        Question(4, "What's your favourite subject?", QuestionType.TEXT),
        Question(5, "Which animal do you like?", QuestionType.SINGLE_CHOICE, listOf("Cat", "Bunny", "Dog", "Horse")),
        Question(6, "Which sport do you like?", QuestionType.SINGLE_CHOICE, listOf("Soccer", "Swimming", "Running", "Golf")),
        Question(7, "What are your hobbies?", QuestionType.MULTI_CHOICE, listOf("Sports", "Cooking", "Music", "Reading")),
        Question(8, "What's your favourite season?", QuestionType.MULTI_CHOICE, listOf("Winter", "Spring", "Summer", "Autumn")),
        Question(9, "What kind of music do you listen to?", QuestionType.MULTI_CHOICE, listOf("Pop", "Rock", "Classic", "Jazz"))
    ),
    val answers: Map<Int, String> = emptyMap(),
    val submitted: Boolean = false
)

class QuestionnaireViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(QuestionnaireUiState())
    val uiState: StateFlow<QuestionnaireUiState> = _uiState

    fun updateAnswer(id: Int, answer: String) {
        _uiState.update { state ->
            state.copy(answers = state.answers + (id to answer))
        }
    }

    fun submit() {
        _uiState.update { state ->
            state.copy(submitted = true)
        }
    }

    fun restart() {
        _uiState.value = QuestionnaireUiState()
    }
}
