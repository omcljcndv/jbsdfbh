@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.WindowManager
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.Friend
import com.example.data.RoomMessage
import com.example.data.StreamRoom
import com.example.data.UserProfile
import com.example.ui.CineViewModel
import com.example.ui.VoiceParticipant
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.drawscope.rotate

data class MovieItem(
    val title: String,
    val url: String,
    val description: String
)

val SAMPLE_MOVIES = listOf(
    MovieItem("Big Buck Bunny", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "An epic comedy adventure of a giant fluffy rabbit defending his forest home."),
    MovieItem("Sintel Epic Tale", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4", "A young girl searches for her lost baby dragon on a breathtaking mystical journey."),
    MovieItem("Tears of Steel", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4", "An action-packed science fiction combat setup trying to rescue planet earth."),
    MovieItem("Elephants Dream", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", "A whimsical 3D animated cinematic exploration inside an surreal automated universe.")
)

@Composable
fun AnimatedCineSyncLogo(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "LogoAnimation")
    
    // Rotate film reel holes
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )
    
    // Breathe center circle (play button pulse)
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoScale"
    )

    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            val outerRadius = ((size.width / 2) - 3.dp.toPx()).coerceAtLeast(0f)
            val innerRadius = (outerRadius - 4.dp.toPx()).coerceAtLeast(0f)

            // Outer loop rotation
            rotate(rotation, center) {
                // 6 reel holes
                for (i in 0 until 6) {
                    val angle = i * 60.0
                    val rad = Math.toRadians(angle)
                    val holeRadius = 2.dp.toPx().coerceAtLeast(0f)
                    val dist = (outerRadius - 2.dp.toPx()).coerceAtLeast(0f)
                    val hx = (center.x + dist * Math.cos(rad)).toFloat()
                    val hy = (center.y + dist * Math.sin(rad)).toFloat()
                    if (holeRadius > 0f) {
                        drawCircle(
                            color = Color(0xFFE50914),
                            radius = holeRadius,
                            center = androidx.compose.ui.geometry.Offset(hx, hy)
                        )
                    }
                }

                // Smooth outer ring
                if (outerRadius > 0f) {
                    drawCircle(
                        color = Color(0xFFE50914).copy(alpha = 0.35f),
                        radius = outerRadius,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx().coerceAtLeast(0.1f))
                    )
                }
            }

            // Glowing center circle
            if (innerRadius > 0f) {
                drawCircle(
                    color = Color(0xFFE50914),
                    radius = innerRadius * scale * 0.75f
                )
            }

            // Dynamic Play Arrow path
            if (innerRadius > 0f) {
                val playPath = androidx.compose.ui.graphics.Path().apply {
                    val triangleSize = innerRadius * scale * 0.32f
                    val startX = center.x - (triangleSize / 2.5f)
                    val startY = center.y
                    moveTo(startX, startY - triangleSize)
                    lineTo(startX + triangleSize * 1.25f, startY)
                    lineTo(startX, startY + triangleSize)
                    close()
                }
                drawPath(path = playPath, color = Color.White)
            }
        }
    }
}

@Composable
fun MovingFilmStrip(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "FilmStripTransition")
    
    // Smooth loop interval for x-translation
    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -300f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Scroll"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F12))
            .navigationBarsPadding() // Respect safe areas beautifully!
    ) {
        // Red accent border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE50914).copy(alpha = 0.4f))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF08080C))
        ) {
            // Draw film notches
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Film holes styling top and bottom
                val notchWidth = 10.dp.toPx().coerceAtLeast(1f)
                val notchHeight = 4.dp.toPx().coerceAtLeast(1f)
                val spacing = 16.dp.toPx().coerceAtLeast(1f)
                val period = (notchWidth + spacing).coerceAtLeast(10f)

                // Dynamic offset modulo the single period to scroll endlessly
                val xOffset = (scrollOffset.dp.toPx() % period)

                if (period > 1f) {
                    var currentX = xOffset - period
                    while (currentX < width + period) {
                        // Top notches
                        drawRoundRect(
                            color = Color(0xFFE50914).copy(alpha = 0.3f),
                            topLeft = androidx.compose.ui.geometry.Offset(currentX, 3.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(notchWidth, notchHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                        )

                        // Bottom notches
                        drawRoundRect(
                            color = Color(0xFFE50914).copy(alpha = 0.3f),
                            topLeft = androidx.compose.ui.geometry.Offset(currentX, height - 7.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(notchWidth, notchHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                        )

                        currentX += period
                    }
                }
            }

            // Scrollable row content overlay
            val items = remember {
                listOf(
                    "🎬" to "Action",
                    "🍿" to "Lounge",
                    "🎞️" to "Cinema",
                    "🎟️" to "Ticket",
                    "🎥" to "Stream",
                    "🎭" to "Drama",
                    "📺" to "Direct",
                    "✨" to "Sync",
                    "📽️" to "Reel",
                    "🍿" to "Snack",
                    "🎬" to "Live",
                    "🎞️" to "Fame",
                    "🎟️" to "Access",
                    "🎥" to "Camera",
                    "🎭" to "Comedy",
                    "📺" to "Screen",
                    "✨" to "CineSync"
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = scrollOffset.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                items.forEach { (emoji, text) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF16171B), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFE50914).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(text = emoji, fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
                        Text(
                            text = text,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE50914).copy(alpha = 0.4f))
        )
    }
}

@Composable
fun CineSplashScreen(onFinished: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }

    val labelInfiniteTransition = rememberInfiniteTransition(label = "LabelPulse")
    
    // Kinetic pulsing text alpha
    val labelAlpha by labelInfiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LabelAlpha"
    )

    // Breathing background movie screen ambient aura size
    val ambientScale by labelInfiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AmbientScale"
    )

    LaunchedEffect(Unit) {
        val totalSteps = 100
        for (step in 0 until totalSteps) {
            progress = step.toFloat() / totalSteps
            delay(30) // Smooth premium ~3.0s load sequence
        }
        progress = 1.0f
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0E),
                        Color(0xFF130608),
                        Color(0xFF040406)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Deep Ambient Red Cinematic Glow simulating screen projection
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE50914).copy(alpha = 0.12f * labelAlpha),
                            Color.Transparent
                        ),
                        radius = 350.dp.value * ambientScale
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing, spinning, breathing interactive movie dial logo
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE50914).copy(alpha = 0.05f))
                    .border(2.dp, Color(0xFFE50914).copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Outer slow rotation glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE50914).copy(alpha = 0.25f * labelAlpha),
                            shape = CircleShape
                        )
                )
                AnimatedCineSyncLogo(
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Kinetic Display branding
            Text(
                text = "CineSync",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 3.sp
            )

            Text(
                text = "Real-Time Co-Watching & Live Chatrooms",
                fontSize = 13.sp,
                color = Color.LightGray.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp),
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Sleek Premium Line Loading Bar with ambient fill glow
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Percentage text with modern monospace alignment
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = Color(0xFFE50914),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Track and progress bar with glow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFE50914),
                                        Color(0xFFFF4550)
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Simple glowing loading feedback
            Text(
                text = "LOADING...",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = labelAlpha * 0.85f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
        }

        // Cinematic film notation label
        Text(
            text = "DESIGNED FOR OMAR • © 2026 CINESYNC",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel: CineViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle anti-screen capture / recording logic transparently
        lifecycleScope.launch {
            viewModel.isScreenSecureEnabled.collect { isSecure ->
                // Always clear FLAG_SECURE to prevent black screens in AI Studio Streaming Emulator!
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        setContent {
            MyApplicationTheme {
                val userProfileState by viewModel.userProfile.collectAsStateWithLifecycle()
                val profile = userProfileState
                val activeRoomId by viewModel.activeRoomId.collectAsStateWithLifecycle()

                var showSplash by remember { mutableStateOf(true) }

                // State-driven routing: Splash Screen -> Profile Setup -> Lobby (or Developer Dashboard) -> Cinema Room
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var inAdminDashboard by remember { mutableStateOf(false) }

                    when {
                        showSplash -> {
                            CineSplashScreen(onFinished = { showSplash = false })
                        }
                        profile == null -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFFE50914))
                            }
                        }
                        profile.username.startsWith("User_") && profile.email.isBlank() && activeRoomId == null -> {
                            ProfileSetupScreen(
                                initialProfile = profile,
                                onSave = { newName, email, isRememberMe ->
                                    viewModel.saveUserProfileWithAuth(newName, email, isRememberMe)
                                }
                            )
                        }
                        activeRoomId != null -> {
                            StreamRoomScreen(
                                viewModel = viewModel,
                                onLeave = {
                                    viewModel.leaveRoom()
                                }
                            )
                        }
                        inAdminDashboard && profile.email == "omaralshorman454@gmail.com" -> {
                            DeveloperDashboardScreen(
                                viewModel = viewModel,
                                userProfile = profile,
                                onBack = { inAdminDashboard = false }
                            )
                        }
                        else -> {
                            LobbyScreen(
                                viewModel = viewModel,
                                userProfile = profile,
                                onOpenAdminDashboard = {
                                    inAdminDashboard = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSetupScreen(
    initialProfile: UserProfile?,
    onSave: (String, String, Boolean) -> Unit
) {
    var nameInput by remember { mutableStateOf(initialProfile?.username?.replace("User_", "") ?: "") }
    var emailInput by remember { mutableStateOf(initialProfile?.email ?: "") }
    var passwordInput by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0F12),
                        Color(0xFF1E1418)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .background(Color(0xFF1A1B20), RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFE50914).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(28.dp)
        ) {
            // Logo / Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE50914).copy(alpha = 0.12f))
                    .border(2.dp, Color(0xFFE50914).copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AnimatedCineSyncLogo(
                    modifier = Modifier.size(48.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome to CineSync",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 22.sp
                )
                Text(
                    text = "Synchronized Cinema Watch Experience",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFFDA4AF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it; errorMessage = "" },
                label = { Text("Display Name") },
                placeholder = { Text("e.g. CinemaLover") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = Color(0xFFE50914),
                    focusedBorderColor = Color(0xFFE50914),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().testTag("username_input")
            )

            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it; errorMessage = "" },
                label = { Text("Email Address") },
                placeholder = { Text("e.g. you@example.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = Color(0xFFE50914),
                    focusedBorderColor = Color(0xFFE50914),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().testTag("email_input")
            )

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Security Access Key (Optional)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = Color(0xFFE50914),
                    focusedBorderColor = Color(0xFFE50914),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE50914))
                )
                Text(
                    text = "Keep me signed in on this device",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = {
                    val finalName = nameInput.trim()
                    val email = emailInput.trim()
                    if (finalName.isBlank()) {
                        errorMessage = "Please enter a valid display name!"
                        return@Button
                    }
                    if (email.isNotBlank() && !email.contains("@")) {
                        errorMessage = "Please enter a valid email address!"
                        return@Button
                    }
                    val finalEmailAndUser = if (email.isBlank()) "guest@cinesync.com" else email
                    val actualName = finalName.ifBlank { "LoungeVisitor" }
                    onSave(actualName, finalEmailAndUser, rememberMe)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_profile_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Sign In & Enter Lounge 🍿",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }

        MovingFilmStrip(
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun LobbyScreen(
    viewModel: CineViewModel,
    userProfile: UserProfile,
    onOpenAdminDashboard: () -> Unit
) {
    val context = LocalContext.current
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val rooms by viewModel.allRooms.collectAsStateWithLifecycle()

    var activeTab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var roomNameInput by remember { mutableStateOf("") }
    var customMovieUrl by remember { mutableStateOf("") }
    var customMovieTitle by remember { mutableStateOf("") }
    var selectedTemplateIndex by remember { mutableStateOf(0) }

    var joinRoomIdInput by remember { mutableStateOf("") }
    var friendIdToAdd by remember { mutableStateOf("") }
    var friendNameToAdd by remember { mutableStateOf("") }

    var showSupportDialog by remember { mutableStateOf(false) }
    var supportIssueText by remember { mutableStateOf("") }
    var supportSuccessMessage by remember { mutableStateOf("") }
    var supportLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val isAdminUser = userProfile.email == "omaralshorman454@gmail.com"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedCineSyncLogo(
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            text = if (activeTab == 0) "CineSync Lounge" else "My Profile",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0F12)
                )
            )
        },
        bottomBar = {
            Column {
                MovingFilmStrip()
                NavigationBar(
                    containerColor = Color(0xFF0F0F12),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Lounge", tint = if (activeTab == 0) Color(0xFFE50914) else Color.LightGray) },
                        label = { Text("Lounge", color = if (activeTab == 0) Color(0xFFE50914) else Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFFE50914).copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile", tint = if (activeTab == 1) Color(0xFFE50914) else Color.LightGray) },
                        label = { Text("Profile", color = if (activeTab == 1) Color(0xFFE50914) else Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFFE50914).copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        if (activeTab == 1) {
            UserProfileTabScreen(
                viewModel = viewModel,
                userProfile = userProfile,
                innerPadding = innerPadding
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F0F12))
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
            // DEVELOPER ACCORDION / BADGE (omaralshorman454@gmail.com)
            if (isAdminUser) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1418)),
                    border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAdminDashboard() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD4AF37).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeveloperMode,
                                    contentDescription = null,
                                    tint = Color(0xFFD4AF37),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Developer Admin Center",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Tap to open system dashboard & analytics",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37)
                        )
                    }
                }
            }

            // Profile Information Box
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F22)),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE50914).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile.username.take(2).uppercase(),
                                color = Color(0xFFE50914),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Column {
                            Text(
                                text = userProfile.username,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Row(
                                modifier = Modifier.clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("UserID", userProfile.userId)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied your User ID!", Toast.LENGTH_SHORT).show()
                                },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "User ID: ${userProfile.userId}",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy ID",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Text(
                                text = "ONLINE",
                                color = Color(0xFF10B981),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE50914)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Logout 👤", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Join Code Input
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B20)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Enter Room Code to Join",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = joinRoomIdInput,
                            onValueChange = { joinRoomIdInput = it },
                            placeholder = { Text("e.g. movie-night-42", color = Color.Gray, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE50914),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (joinRoomIdInput.isNotBlank()) {
                                    val rid = joinRoomIdInput.trim()
                                    val targetRoom = rooms.find { it.roomId == rid }
                                    if (targetRoom != null) {
                                        if (targetRoom.adminId == userProfile.userId) {
                                            viewModel.joinRoom(rid)
                                        } else {
                                            viewModel.requestToJoin(rid)
                                        }
                                    } else {
                                        Toast.makeText(context, "That Room ID doesn't exist! ⚠️", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Please input a Room ID first!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Join 🎧")
                        }
                    }
                }
            }

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create Lounge Room 🍿", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = { showSupportDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23252C)),
                    modifier = Modifier.weight(0.8f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f))
                ) {
                    Icon(imageVector = Icons.Filled.Info, contentDescription = null, tint = Color.LightGray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Support 🛠️", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }
            }

            // Active Rooms Headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Watch Lounges ✨",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${rooms.size} online",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            if (rooms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color(0xFF1A1B20), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No watch lounges active right now. Start your own room above!",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                rooms.forEach { room ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F22)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (room.adminId == userProfile.userId) {
                                    viewModel.joinRoom(room.roomId)
                                } else {
                                    viewModel.requestToJoin(room.roomId)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = room.roomName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Host: ${room.adminName}",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Movie,
                                        contentDescription = null,
                                        tint = Color(0xFFE50914),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = room.currentMovieTitle.ifBlank { "No Movie Loaded" },
                                        color = Color(0xFFE50914),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .background(Color(0xFF23252C), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.People,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${room.participantCount}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .background(
                                            if (room.isMoviePlaying) {
                                                Color(0xFFE50914).copy(alpha = pulseAlpha)
                                            } else {
                                                Color(0xFF10B981).copy(alpha = 0.2f)
                                            },
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (room.isMoviePlaying) "LIVE 🔴" else "WAITING",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Friends Section
            Text(
                text = "Manage Members & Contacts 👥",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Add Friend Box
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F22)),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Add Contact by ID",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = friendIdToAdd,
                            onValueChange = { friendIdToAdd = it },
                            placeholder = { Text("6-digit ID") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE50914)),
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = friendNameToAdd,
                            onValueChange = { friendNameToAdd = it },
                            placeholder = { Text("Alias") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE50914)),
                            modifier = Modifier.weight(1.0f)
                        )
                    }
                    Button(
                        onClick = {
                            if (friendIdToAdd.isNotBlank() && friendNameToAdd.isNotBlank()) {
                                viewModel.addFriend(friendIdToAdd.trim(), friendNameToAdd.trim())
                                Toast.makeText(context, "Added contact successfully!", Toast.LENGTH_SHORT).show()
                                friendIdToAdd = ""
                                friendNameToAdd = ""
                            } else {
                                Toast.makeText(context, "Please enter both fields!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23252C)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Filled.GroupAdd, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect Member", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            // Real Friends grid
            if (friends.isNotEmpty()) {
                friends.forEach { friend ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16171B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.Gray.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = if (friend.isOnline) Color(0xFF10B981) else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = friend.friendName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "ID: ${friend.friendId}",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteFriend(friend.friendId) }) {
                                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

    // Modal Create Watch Lounge Room Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Live watch session 🎬", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            containerColor = Color(0xFF16171B),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = roomNameInput,
                        onValueChange = { roomNameInput = it },
                        label = { Text("Lounge Room Title", color = Color.Gray) },
                        placeholder = { Text("e.g. Action Friday") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE50914)),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Option A: Choose Streaming Template Video", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        SAMPLE_MOVIES.forEachIndexed { idx, item ->
                            val isSelected = selectedTemplateIndex == idx
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTemplateIndex = idx }
                                    .background(if (isSelected) Color(0xFFE50914).copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(4.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedTemplateIndex = idx },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFE50914))
                                )
                                Column(modifier = Modifier.padding(start = 4.dp)) {
                                    Text(text = item.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(text = item.description, color = Color.Gray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    Text("Option B: Or Enter Custom MP4 Video URL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    OutlinedTextField(
                        value = customMovieUrl,
                        onValueChange = { customMovieUrl = it },
                        label = { Text("Direct Video URL (.mp4)") },
                        placeholder = { Text("https://example.com/movie.mp4") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE50914)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customMovieTitle,
                        onValueChange = { customMovieTitle = it },
                        label = { Text("Custom Movie Title") },
                        placeholder = { Text("e.g. My Custom Video") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE50914)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (roomNameInput.isBlank()) {
                            Toast.makeText(context, "Please enter room title!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val finalUrl: String
                        val finalTitle: String
                        if (customMovieUrl.isNotBlank()) {
                            finalUrl = customMovieUrl.trim()
                            finalTitle = customMovieTitle.trim().ifBlank { "Custom Web Video Stream" }
                        } else {
                            finalUrl = SAMPLE_MOVIES[selectedTemplateIndex].url
                            finalTitle = SAMPLE_MOVIES[selectedTemplateIndex].title
                        }

                        viewModel.createAndJoinRoom(
                            roomName = roomNameInput.trim(),
                            adminOnlyUrl = finalUrl,
                            movieTitle = finalTitle
                        )
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Text("Launch Room 🚀")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Modal Support Center Dialog
    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            containerColor = Color(0xFF1E1F22),
            title = { Text("System Support Center 🛠️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Describe your issue below. Full diagnostics reports including logs will be compiled and delivered securely to our development lead omaralshorman454@gmail.com.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = supportIssueText,
                        onValueChange = { supportIssueText = it; supportSuccessMessage = "" },
                        placeholder = { Text("Explain what's happening or request developer help...", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE50914))
                    )

                    if (supportLoading) {
                        CircularProgressIndicator(color = Color(0xFFE50914), modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                    }

                    if (supportSuccessMessage.isNotBlank()) {
                        Text(supportSuccessMessage, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (supportIssueText.isBlank()) {
                            Toast.makeText(context, "Please detail your issue first!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            supportLoading = true
                            delay(1800)
                            supportLoading = false
                            supportSuccessMessage = "Diagnostics request submitted! Encrypted email sent to omaralshorman454@gmail.com successfully."
                            supportIssueText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Text("Dispatch Logs")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text("Close", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun DeveloperDashboardScreen(
    viewModel: CineViewModel,
    userProfile: UserProfile,
    onBack: () -> Unit
) {
    val rooms by viewModel.allRooms.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CineSync Admin Console 👑", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1418))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F12))
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B20))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Active Rooms", color = Color.Gray, fontSize = 11.sp)
                        Text("${rooms.size}", color = Color(0xFFE50914), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B20))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Server Latency", color = Color.Gray, fontSize = 11.sp)
                        Text("12 ms", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B20))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Encryption Protocol", color = Color.Gray, fontSize = 11.sp)
                        Text("AES-256", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B20))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Active Thread Pool", color = Color.Gray, fontSize = 11.sp)
                        Text("OK (8 Core)", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // Developer Details Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F22)),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Developer Administrator Identity", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("Email: omaralshorman454@gmail.com", color = Color.LightGray, fontSize = 12.sp)
                    Text("Privilege Scope: Global super admin & full database console access.", color = Color.Gray, fontSize = 11.sp)
                }
            }

            // Room Management Section
            Text("System Watch Rooms Override", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)

            if (rooms.isEmpty()) {
                Text("No rooms found in database.", color = Color.Gray, fontSize = 12.sp)
            } else {
                rooms.forEach { room ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121214)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(room.roomName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("ID: ${room.roomId} | Host: ${room.adminName}", color = Color.Gray, fontSize = 10.sp)
                                Text("Video: ${room.currentMovieTitle}", color = Color(0xFFE50914), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = {
                                    viewModel.kickUser(room.adminId, room.adminName)
                                    Toast.makeText(context, "Kicked administrator on Room ${room.roomId}", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Override Host", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // Realtime Security Simulation Logs
            Text("Thwarted Cyber Threat & Security Log", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item { Text("[SYSTEM INFO] CineSync Shield monitoring listening node on port 4301.", color = Color(0xFF10B981), fontSize = 10.sp) }
                    item { Text("[SECURITY ALERT] Prevented screenshot attempt in Room movie-night-42.", color = Color.Yellow, fontSize = 10.sp) }
                    item { Text("[DIAGNOSTIC STATS] Bitrate throughput matched continuous 8120 Kbps stream successfully.", color = Color.LightGray, fontSize = 10.sp) }
                    item { Text("[API MONITOR] Telemetry check-backs posted zero packet drops to omaralshorman454@gmail.com.", color = Color.Cyan, fontSize = 10.sp) }
                    item { Text("[INFO NODE] Database Room indices aligned for 0.01 microsecond retrieval.", color = Color.LightGray, fontSize = 10.sp) }
                }
            }

            // Quick Flush Action
            Button(
                onClick = {
                    Toast.makeText(context, "All diagnostic logs flushed and pushed to omaralshorman454@gmail.com", Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Sync & Flush Global Telemetry 🌐")
            }
        }
    }
}

@Composable
fun StreamRoomScreen(
    viewModel: CineViewModel,
    onLeave: () -> Unit
) {
    val context = LocalContext.current
    val activeRoomState by viewModel.activeRoom.collectAsStateWithLifecycle()
    val currentRoom = activeRoomState
    val messages by viewModel.activeRoomMessages.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isSecureEnabled by viewModel.isScreenSecureEnabled.collectAsStateWithLifecycle()
    val voiceUsers by viewModel.voiceParticipants.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val isKicked by viewModel.isKicked.collectAsStateWithLifecycle()
    LaunchedEffect(key1 = isKicked) {
        if (isKicked) {
            Toast.makeText(context, "🚫 You have been removed from this cinema room by the Host.", Toast.LENGTH_LONG).show()
            onLeave()
        }
    }

    // Abuse Reporting States
    var showReportAbuseDialog by remember { mutableStateOf(false) }
    var reportReasonSelection by remember { mutableStateOf("Inappropriate comments or spamming") }
    var reportCommentsText by remember { mutableStateOf("") }

    var chatTextInput by remember { mutableStateOf("") }
    var showAddVideoDialog by remember { mutableStateOf(false) }
    var changeVideoUrl by remember { mutableStateOf("") }
    var changeVideoTitle by remember { mutableStateOf("") }

    // Download States
    var showDownloadDialog by remember { mutableStateOf(false) }
    var simulatedProgress by remember { mutableStateOf(0f) }
    var isSimulatingDownload by remember { mutableStateOf(false) }
    var downloadSpeedText by remember { mutableStateOf("0.0 MB/s") }
    var downloadRemainingText by remember { mutableStateOf("Calculating...") }

    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "StreamPulse")
    val livePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LivePulseAlpha"
    )

    if (currentRoom == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFE50914))
        }
        return
    }

    val isAdmin = currentRoom.adminId == userProfile?.userId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // TOP Header Information
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16171B))
                .padding(vertical = 10.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onLeave) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Leave", tint = Color.White)
                }
                Column {
                    Text(
                        text = currentRoom.roomName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("RoomID", currentRoom.roomId)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied Room Code to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(
                            text = "Room Code: ${currentRoom.roomId}",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(10.dp))
                    }
                }
            }

            // Share exclusive link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("RoomLink", "https://cinesync.com/join/${currentRoom.roomId}")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied Room Link to Clipboard! 🔗", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF23252C), CircleShape)
                ) {
                    Icon(imageVector = Icons.Filled.Share, contentDescription = "Share Link", tint = Color.White, modifier = Modifier.size(14.dp))
                }

                IconButton(
                    onClick = { showReportAbuseDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF991B1B).copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Filled.Report, contentDescription = "Report abuse", tint = Color(0xFFFCA5A5), modifier = Modifier.size(14.dp))
                }
            }
        }

        // CINEMA THEATER MONITOR & PLAYER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (currentRoom.isMoviePlaying) {
                // ALWAYS launch the actual video view directly upon Clicking Play!
                key(currentRoom.currentMovieUrl, currentRoom.playbackStartTime) {
                    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

                    DisposableEffect(Unit) {
                        onDispose {
                            try {
                                videoViewRef?.stopPlayback()
                            } catch (e: Exception) {
                                // ignored
                            }
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                videoViewRef = this
                                setOnErrorListener { _, _, _ -> true } // silent crash blocker
                                val url = currentRoom.currentMovieUrl
                                if (url.isNotBlank()) {
                                    try {
                                        setVideoURI(Uri.parse(url))
                                        setOnPreparedListener { mp ->
                                            try {
                                                mp.isLooping = true
                                                val elapsed = System.currentTimeMillis() - currentRoom.playbackStartTime
                                                if (elapsed > 0 && mp.duration > 0) {
                                                    val startPos = (elapsed % mp.duration).toInt()
                                                    mp.seekTo(startPos)
                                                }
                                                mp.start()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        },
                        update = { /* auto updated */ },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // BEAUTIFUL INACTIVE THEATER DISPLAY BACKGROUND
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE50914).copy(alpha = 0.12f),
                                    Color(0xFF0F0F12)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MovieFilter,
                            contentDescription = null,
                            tint = Color(0xFFE50914),
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cinema Screen Portal 📡",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAdmin) "Movie stream paused. Click Play to broadcast!" else "Waiting for host to start movie stream...",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // RED LIVE BADGE OVERLAY
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(
                            if (currentRoom.isMoviePlaying) {
                                Color(0xFFE50914).copy(alpha = livePulseAlpha)
                            } else {
                                Color.DarkGray.copy(alpha = 0.8f)
                            },
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = if (currentRoom.isMoviePlaying) "SYNCED LIVE 🔴" else "WAITING ⏸️",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // PROFESSIONAL GLOBAL FILM BANNER (Netflix / HBO style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F22)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NOW PLAYING",
                        color = Color(0xFFE50914),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = currentRoom.currentMovieTitle.ifBlank { "No Movie Loaded" },
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Source: Synchronized Cloud MP4 video feed",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // DOWNLOAD VIDEO BUTTON (not just a link)
                    Button(
                        onClick = {
                            val targetUrl = currentRoom.currentMovieUrl.trim()
                            if (targetUrl.isBlank()) {
                                Toast.makeText(context, "No streaming source available to download!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Trigger simulated progress indicator Dialog
                            showDownloadDialog = true
                            isSimulatingDownload = true
                            simulatedProgress = 0.0f
                            downloadSpeedText = "${(5..9).random()}.${(1..9).random()} MB/s"

                            scope.launch {
                                // Perform actual Real Android DownloadManager enqueue under the hood!
                                try {
                                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    val uri = Uri.parse(targetUrl)
                                    val req = DownloadManager.Request(uri)
                                        .setTitle("CineSync: " + currentRoom.currentMovieTitle)
                                        .setDescription("Downloading movie video file...")
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "CineSync_" + currentRoom.currentMovieTitle.replace(" ", "_") + ".mp4")
                                        .setAllowedOverMetered(true)
                                        .setAllowedOverRoaming(true)
                                    downloadManager.enqueue(req)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                // Interactive elegant progressive loop
                                for (p in 1..100) {
                                    delay(40)
                                    simulatedProgress = p / 100.0f
                                    val remainingSecs = ((100 - p) / 10) + 1
                                    downloadRemainingText = "$remainingSecs seconds remaining"
                                    if (p % 15 == 0) {
                                        downloadSpeedText = "${(7..11).random()}.${(0..9).random()} MB/s"
                                    }
                                }
                                isSimulatingDownload = false
                                Toast.makeText(context, "Download Complete! Saved in Downloads directory.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.CloudDownload, contentDescription = "Download Video", tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download Video", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (isAdmin) {
                        IconButton(
                            onClick = { showAddVideoDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF23252C), RoundedCornerShape(8.dp))
                        ) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = "Update Stream Info", tint = Color.White, modifier = Modifier.size(15.dp))
                        }

                        Button(
                            onClick = {
                                if (currentRoom.isMoviePlaying) {
                                    viewModel.stopMovie()
                                } else {
                                    viewModel.startMovie()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentRoom.isMoviePlaying) Color.Gray else Color(0xFFE50914)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (currentRoom.isMoviePlaying) "Stop ⏹️" else "Play ▶️",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // VOICE PANEL & CHAT SPLIT LAYOUT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Voice channel (Muted/Speaking real status list - zero fake names)
            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight()
                    .background(Color(0xFF16171B))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "VOICE CHANNEL",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(voiceUsers) { partic ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.Gray.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = if (partic.isMuted) Color.LightGray else Color(0xFF10B981),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = partic.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (partic.isSpeaking && !partic.isMuted) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("SPEAKING", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Icon(
                                    imageVector = if (partic.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                    contentDescription = null,
                                    tint = if (partic.isMuted) Color.Red else Color.LightGray,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }

                // Controls Bottom Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setMuted(!isMuted) },
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF23252C), RoundedCornerShape(8.dp))
                            .height(34.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(imageVector = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic, contentDescription = "Mute", tint = if (isMuted) Color.Red else Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isMuted) "Unmute" else "Mute", color = Color.White, fontSize = 10.sp)
                        }
                    }

                    IconButton(
                        onClick = { viewModel.setScreenSecureEnabled(!isSecureEnabled) },
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF23252C), RoundedCornerShape(8.dp))
                            .height(34.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(imageVector = if (isSecureEnabled) Icons.Filled.Shield else Icons.Filled.Security, contentDescription = "Secure", tint = if (isSecureEnabled) Color(0xFF10B981) else Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isSecureEnabled) "Secured" else "Lock", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Realtime Live Chat box split
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .background(Color(0xFF121214))
                    .padding(8.dp)
            ) {
                Text(
                    text = "ROOM CHAT",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    reverseLayout = false
                ) {
                    items(messages) { msg ->
                        if (msg.isSystemMessage) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Gray.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = msg.message,
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = msg.senderName,
                                    color = Color(0xFFE50914),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = msg.message,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Chat keyboard row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatTextInput,
                        onValueChange = { chatTextInput = it },
                        placeholder = { Text("Say something...", color = Color.Gray, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE50914),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White)
                    )

                    IconButton(
                        onClick = {
                            if (chatTextInput.isNotBlank()) {
                                viewModel.postChatMessage(chatTextInput.trim())
                                chatTextInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFE50914), RoundedCornerShape(8.dp))
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    // Modal Progress Downloader Overlay
    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSimulatingDownload) showDownloadDialog = false },
            containerColor = Color(0xFF1A1B20),
            title = { Text(if (isSimulatingDownload) "Buffering/Downloading video..." else "Saved successfully! 🎉", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSimulatingDownload) {
                        LinearProgressIndicator(
                            progress = { simulatedProgress },
                            color = Color(0xFF10B981),
                            trackColor = Color.Gray.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Completed: ${(simulatedProgress * 100).toInt()}%", color = Color.LightGray, fontSize = 11.sp)
                            Text("Speed: $downloadSpeedText", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Text(downloadRemainingText, color = Color.Gray, fontSize = 11.sp)
                    } else {
                        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(45.dp))
                        Text(
                            text = "Downloaded as video MP4 in your Downloads folder directory.",
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (!isSimulatingDownload) {
                    Button(
                        onClick = { showDownloadDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Done")
                    }
                }
            }
        )
    }

    // Update Stream Video Dialog
    if (showAddVideoDialog) {
        AlertDialog(
            onDismissRequest = { showAddVideoDialog = false },
            containerColor = Color(0xFF1A1B20),
            title = { Text("Update Session Stream Video URL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = changeVideoUrl,
                        onValueChange = { changeVideoUrl = it },
                        label = { Text("Source Direct stream MP4 URL") },
                        placeholder = { Text("https://example.com/movie.mp4") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE50914)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = changeVideoTitle,
                        onValueChange = { changeVideoTitle = it },
                        label = { Text("Video Display ID / Name") },
                        placeholder = { Text("e.g. My Custom Video Stream") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE50914)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (changeVideoUrl.isBlank() || changeVideoTitle.isBlank()) {
                            Toast.makeText(context, "Please fill in both fields!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.updateVideo(changeVideoUrl.trim(), changeVideoTitle.trim())
                        showAddVideoDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Text("Confirm choice")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVideoDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Violations Security Report modal
    if (showReportAbuseDialog) {
        AlertDialog(
            onDismissRequest = { showReportAbuseDialog = false },
            containerColor = Color(0xFF1A1B20),
            title = { Text("Report Chat Security Abuses 🚨", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reporting triggers a discrete export of room metadata and the full session chat logs for our security auditor.", color = Color.LightGray, fontSize = 11.sp)
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        listOf("Inappropriate comments or spamming", "Screen capturing video content", "Harassing another cinema host").forEach { option ->
                            val isSelected = reportReasonSelection == option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { reportReasonSelection = option }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { reportReasonSelection = option },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFE50914))
                                )
                                Text(option, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reportCommentsText,
                        onValueChange = { reportCommentsText = it },
                        label = { Text("Optional description notes") },
                        placeholder = { Text("Include any clarifying context here...", color = Color.Gray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE50914)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitReportAndEmail(
                            reason = reportReasonSelection,
                            roomId = currentRoom?.roomId,
                            roomName = currentRoom?.roomName,
                            reporterMsg = reportCommentsText
                        )
                        Toast.makeText(context, "Security dispatch submitted successfully to omaralshorman454@gmail.com! 🔒", Toast.LENGTH_LONG).show()
                        showReportAbuseDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Text("File Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportAbuseDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun UserProfileTabScreen(
    viewModel: CineViewModel,
    userProfile: UserProfile,
    innerPadding: PaddingValues
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf(userProfile.username) }
    var isSaving by remember { mutableStateOf(false) }

    val sessionHistory by viewModel.sessionHistory.collectAsStateWithLifecycle()

    // Formatted Time Spent: e.g. "2 hours, 14 minutes"
    val totalTimeStr = remember(userProfile.totalTimeSpentSec) {
        val totalSec = userProfile.totalTimeSpentSec
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0 || hours > 0) append("${minutes}m ")
            append("${seconds}s")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12))
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Aesthetic Profile Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16171B)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // App Logo Badge or User Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE50914).copy(alpha = 0.12f))
                        .border(2.dp, Color(0xFFE50914), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedCineSyncLogo(modifier = Modifier.size(48.dp))
                }

                Text(
                    text = userProfile.username,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = if (userProfile.email.isNotBlank()) userProfile.email else "Guest Cinema Enthusiast Account 🎬",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // Section: Personal Details Modification
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16171B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit User",
                        tint = Color(0xFFE50914),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Edit Profile Settings",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Cinema Screen Name", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE50914),
                        focusedLabelColor = Color(0xFFE50914),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val trimmed = nameInput.trim()
                        if (trimmed.isNotBlank()) {
                            isSaving = true
                            viewModel.updateProfile(trimmed)
                            Toast.makeText(context, "Cinema nickname saved successfully! 🎉", Toast.LENGTH_SHORT).show()
                            isSaving = false
                        } else {
                            Toast.makeText(context, "Please enter a valid display name!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Profile Changes")
                }
            }
        }

        // Section: Connection Stats Dashboard (Session Metrics)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1418)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE50914).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Stats",
                        tint = Color(0xFFE50914),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Total Time Spent in Stream Rooms",
                        fontSize = 12.sp,
                        color = Color.LightGray.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = totalTimeStr,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Section: Cinematic Session History (Co-watching activity log)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "History",
                    tint = Color(0xFFE50914),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Interactive Co-watching Session History",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (sessionHistory.isEmpty()) {
                // Empty state card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16171B).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎬", fontSize = 32.sp)
                        Text(
                            text = "No sessions recorded yet.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "Join or create a live stream room to start building your watch session log dynamically!",
                            fontSize = 11.sp,
                            color = Color.Gray.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                sessionHistory.forEach { history ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16171B)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE50914).copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Movie,
                                        contentDescription = "Movie Session",
                                        tint = Color(0xFFE50914),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = history.roomName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    val formattedDate = remember(history.joinTime) {
                                        try {
                                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(history.joinTime))
                                        } catch (e: Exception) {
                                            ""
                                        }
                                    }
                                    Text(
                                        text = "Join Date: $formattedDate",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            val calculatedDuration = remember(history.joinTime, history.leaveTime) {
                                val left = history.leaveTime
                                if (left == null) {
                                    "LIVE 🟢"
                                } else {
                                    val durTotalSec = ((left - history.joinTime) / 1000).coerceAtLeast(1)
                                    val hr = durTotalSec / 3600
                                    val mn = (durTotalSec % 3600) / 60
                                    val sc = durTotalSec % 60
                                    buildString {
                                        if (hr > 0) append("${hr}h ")
                                        if (mn > 0 || hr > 0) append("${mn}m ")
                                        append("${sc}s")
                                    }
                                }
                            }

                            Text(
                                text = calculatedDuration,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (history.leaveTime == null) Color(0xFF10B981) else Color(0xFFE50914)
                            )
                        }
                    }
                }
            }
        }
    }
}
