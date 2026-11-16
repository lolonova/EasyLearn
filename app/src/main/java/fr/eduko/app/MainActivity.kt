package fr.eduko.app

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF18243A)
private val Blue = Color(0xFF315F91)
private val BlueSoft = Color(0xFFEAF2F9)
private val Paper = Color(0xFFF7F8FA)
private val Muted = Color(0xFF68778A)
private val Line = Color(0xFFE5EAF0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("eduko_profile", MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Blue, onPrimary = Color.White, background = Paper,
                surface = Color.White, onSurface = Ink
            )) {
                EdukoApp(
                    initialName = prefs.getString("name", "") ?: "",
                    initialLevel = prefs.getString("level", "4e") ?: "4e",
                    saveProfile = { name, level ->
                        prefs.edit().putString("name", name).putString("level", level).apply()
                    }
                )
            }
        }
    }
}

private data class ChatMessage(val text: String, val fromUser: Boolean, val isNotice: Boolean = false)

@Composable
private fun EdukoApp(initialName: String, initialLevel: String, saveProfile: (String, String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var level by remember { mutableStateOf(initialLevel) }
    var draft by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var photo by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val messages = remember { mutableStateListOf<ChatMessage>() }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) draft = listOf(draft, spoken).filter { it.isNotBlank() }.joinToString(" ")
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) photo = bitmap
    }

    fun sendMessage() {
        val text = draft.trim()
        if (text.isNotEmpty() || photo != null) {
            messages.add(ChatMessage(text.ifBlank { "J'ai ajouté une photo." }, true))
            messages.add(ChatMessage(
                "Je n'ai pas encore accès au moteur d'intelligence artificielle. Cette version met en place l'interface de discussion ; la connexion sécurisée à un service IA et l'analyse des photos restent à développer.",
                false, true
            ))
            draft = ""
            photo = null
        }
    }

    Surface(Modifier.fillMaxSize(), color = Paper) {
        Column(Modifier.fillMaxSize()) {
            TopBar(name.ifBlank { "Élève" }, level, { showSettings = true }, { messages.clear() })
            if (messages.isEmpty()) {
                WelcomePanel(name)
                SuggestedPrompts(
                    onChoose = { draft = it },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(messages) { message -> MessageBubble(message) }
                }
            }
            if (photo != null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = photo!!.asImageBitmap(), contentDescription = "Photo du devoir",
                        modifier = Modifier.size(62.dp).background(Color.White, RoundedCornerShape(10.dp))
                    )
                    Text("Photo prête à joindre", Modifier.weight(1f).padding(start = 10.dp), color = Muted, fontSize = 13.sp)
                    Text("Retirer", Modifier.clickable { photo = null }.padding(8.dp), color = Blue, fontSize = 13.sp)
                }
            }
            Composer(
                value = draft,
                onValueChange = { draft = it },
                onSend = { sendMessage() },
                onCamera = { cameraLauncher.launch(null) },
                onVoice = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Dis-moi ce que tu veux comprendre")
                    }
                    runCatching { voiceLauncher.launch(intent) }
                }
            )
            Text(
                "Eduko peut se tromper : vérifie les réponses avec ton cours ou ton enseignant.",
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 8.dp),
                color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center
            )
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Mon profil", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Prénom", color = Muted, fontSize = 12.sp)
                    OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, placeholder = { Text("Ton prénom") })
                    Text("Classe", color = Muted, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("6e", "5e", "4e", "3e", "2nde", "1re", "Tle").forEach {
                            FilterChip(selected = level == it, onClick = { level = it }, label = { Text(it) })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { saveProfile(name, level); showSettings = false }) { Text("Enregistrer") } },
            dismissButton = { TextButton(onClick = { showSettings = false }) { Text("Fermer") } }
        )
    }
}

@Composable
private fun TopBar(name: String, level: String, settings: () -> Unit, newChat: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).background(BlueSoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.School, contentDescription = null, tint = Blue, modifier = Modifier.size(23.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text("EDUKO", color = Blue, fontSize = 17.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("$name · $level", color = Muted, fontSize = 11.sp)
        }
        IconButton(onClick = newChat) { Icon(Icons.Default.Add, "Nouvelle discussion", tint = Ink) }
        IconButton(onClick = settings) { Icon(Icons.Default.Settings, "Paramètres", tint = Ink) }
    }
    HorizontalDivider(color = Line)
}

@Composable
private fun WelcomePanel(name: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 28.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(86.dp).background(BlueSoft, CircleShape), contentAlignment = Alignment.Center) {
            Text("E", color = Blue, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(18.dp))
        Text(if (name.isBlank()) "Bonjour ! Je suis Eduko." else "Bonjour $name !", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Ton professeur à la maison. On peut reprendre un cours, comprendre un exercice ou préparer un contrôle, tranquillement et étape par étape.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SuggestedPrompts(onChoose: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("COMMENT PUIS-JE T'AIDER ?", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        PromptCard("📘", "M'expliquer une leçon", "Avec des mots simples et des exemples", onChoose)
        PromptCard("✏️", "M'aider sur un exercice", "Un indice à la fois, sans donner directement la réponse", onChoose)
        PromptCard("📷", "Comprendre une photo de cours", "Ajouter une photo avec le bouton appareil", onChoose)
        PromptCard("🧠", "Me préparer à un contrôle", "Réviser les notions importantes", onChoose)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PromptCard(icon: String, title: String, subtitle: String, onChoose: (String) -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable { onChoose("$title : ") },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 25.sp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
            Text("›", color = Blue, fontSize = 25.sp)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 310.dp)
                .background(
                    when {
                        message.fromUser -> Blue
                        message.isNotice -> Color(0xFFFFF5E6)
                        else -> Color.White
                    },
                    RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 15.dp, vertical = 12.dp)
        ) {
            if (!message.fromUser) Text("EDUKO", color = if (message.isNotice) Color(0xFF8B5B1D) else Blue, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(message.text, color = if (message.fromUser) Color.White else Ink, fontSize = 14.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onCamera: () -> Unit,
    onVoice: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().background(Paper, RoundedCornerShape(22.dp)).padding(horizontal = 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCamera) { Icon(Icons.Default.CameraAlt, "Ajouter une photo", tint = Blue) }
            BasicTextField(
                value = value, onValueChange = onValueChange,
                modifier = Modifier.weight(1f).padding(horizontal = 5.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 14.sp),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) Text("Écris ton message…", color = Muted, fontSize = 14.sp)
                        inner()
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines = 4
            )
            IconButton(onClick = onVoice) { Icon(Icons.Default.Mic, "Dicter un message", tint = Blue) }
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank(),
                modifier = Modifier.size(42.dp).background(if (value.isNotBlank()) Blue else Line, CircleShape)
            ) { Icon(Icons.Default.ArrowUpward, "Envoyer", tint = Color.White) }
        }
    }
}
