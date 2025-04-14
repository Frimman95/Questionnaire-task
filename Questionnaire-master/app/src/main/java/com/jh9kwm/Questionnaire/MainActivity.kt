package com.jh9kwm.Questionnaire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.google.firebase.auth.FirebaseAuth
import com.jh9kwm.Questionnaire.ui.theme.QuestionnaireTheme

private val DefaultShapes = Shapes()
private val DefaultTypography = Typography()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuestionnaireTheme {
                AuthWrapper {
                    QuestionnaireApp()
                }
            }
        }
    }
}

@Composable
fun AuthWrapper(content: @Composable () -> Unit) {
    var user by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    if (user == null) {
        AuthScreen(
            onLogin = { email, password, onResult ->
                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        user = FirebaseAuth.getInstance().currentUser
                        onResult(null)
                    }
                    .addOnFailureListener {
                        onResult(it.localizedMessage ?: "Unkown error while logging in.")
                    }
            },
            onRegister = { email, password, onResult ->
                FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        user = FirebaseAuth.getInstance().currentUser
                        onResult(null)
                    }
                    .addOnFailureListener {
                        onResult(it.localizedMessage ?: "Unknown error while registering.")
                    }
            }
        )
    } else {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = {
                    FirebaseAuth.getInstance().signOut()
                    user = null
                }) {
                    Text("Log out")
                }
            }
            content()
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
