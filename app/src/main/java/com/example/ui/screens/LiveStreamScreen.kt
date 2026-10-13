package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.data.viewmodel.NavigationTab
import com.example.ui.theme.LocalAppLanguage

data class StreamChannelPreset(
    val id: String,
    val title: String,
    val category: String,
    val streamUrl: String,
    val resolution: String = "1080p 60fps"
)

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStreamScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val isFr = language == com.example.ui.theme.AppLanguage.FRENCH

    val currentStreamUrl by viewModel.currentStreamUrl.collectAsState()
    val currentStreamTitle by viewModel.currentStreamTitle.collectAsState()

    var isFullscreen by remember { mutableStateOf(false) }
    var useNativePlayer by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var nativeMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlayerLoading by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var showCustomUrlDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf("") }
    var customTitleInput by remember { mutableStateOf("") }
    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                nativeMediaPlayer?.stop()
                nativeMediaPlayer?.reset()
                nativeMediaPlayer?.release()
                nativeMediaPlayer = null
                webViewInstance?.stopLoading()
                webViewInstance?.onPause()
                webViewInstance?.pauseTimers()
                webViewInstance?.destroy()
                webViewInstance = null
            } catch (e: Exception) {
                Log.w("LiveStreamScreen", "Player cleanup error: ${e.message}")
            }
        }
    }

    val presets = remember {
        listOf(
            StreamChannelPreset(
                id = "zcore_logic_cover",
                title = "Z-CORE Logic Cover (979V11)",
                category = "Tunnel Cloudflare (Ghost)",
                streamUrl = "https://retailers-mitchell-customs-rotation.trycloudflare.com/logic_cover.html",
                resolution = "Web UI / Gatekeeper 979V11"
            ),
            StreamChannelPreset(
                id = "zcore_usb_live",
                title = "Z-CORE USB Live (Montreal)",
                category = "Tunnel Cloudflare (Ghost)",
                streamUrl = "https://retailers-mitchell-customs-rotation.trycloudflare.com/index_usb.html",
                resolution = "Web UI / Montreal QC"
            ),
            StreamChannelPreset(
                id = "zcore_workspace",
                title = "Z-CORE Workspace Live",
                category = "Tunnel Cloudflare (Ghost)",
                streamUrl = "https://retailers-mitchell-customs-rotation.trycloudflare.com/",
                resolution = "Cloudflare Root Listing"
            ),
            StreamChannelPreset(
                id = "mux_test_hls",
                title = "Mux Live HLS Test",
                category = "Flux HLS Certifié",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                resolution = "1080p HLS Validé"
            ),
            StreamChannelPreset(
                id = "bein_1_hf",
                title = "BEIN 1 HF",
                category = "Sport & Direct",
                streamUrl = "https://tvnow.best/api/stream/amishpatel81@gmail.com/790899/livetv.epg/tf1.fr.m3u8",
                resolution = "1080p High Frequency"
            ),
            StreamChannelPreset(
                id = "tf1_direct",
                title = "TF1 HD Direct",
                category = "Généraliste",
                streamUrl = "https://tvnow.best/api/stream/amishpatel81@gmail.com/790899/livetv.epg/tf1.fr.m3u8",
                resolution = "1080p Direct"
            ),
            StreamChannelPreset(
                id = "france_24",
                title = "France 24 Info",
                category = "Actualités 24/7",
                streamUrl = "https://static.france24.com/live/F24_FR_LO_HLS/live_tv.m3u8",
                resolution = "720p HLS"
            ),
            StreamChannelPreset(
                id = "euronews_fr",
                title = "Euronews Français",
                category = "Actualités Internationales",
                streamUrl = "https://rakuten-euronews-1-eu.rakuten.wurl.tv/playlist.m3u8",
                resolution = "720p HLS"
            )
        )
    }

    // Handles hardware back button when in fullscreen
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    val htmlData = remember(currentStreamUrl, currentStreamTitle) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <title>$currentStreamTitle</title>
            <style>
                body, html {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background-color: #0b0f19;
                    overflow: hidden;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                }
                video {
                    width: 100%;
                    height: 100%;
                    object-fit: contain;
                    background-color: #000;
                }
                .badge-live {
                    position: absolute;
                    top: 12px;
                    left: 12px;
                    z-index: 100;
                    background: #dc2626;
                    color: white;
                    padding: 4px 10px;
                    border-radius: 6px;
                    font-size: 11px;
                    font-weight: 700;
                    letter-spacing: 0.5px;
                    box-shadow: 0 2px 8px rgba(220, 38, 38, 0.4);
                    pointer-events: none;
                }
            </style>
        </head>
        <body>
            <div class="badge-live">● EN DIRECT</div>
            <video id="my_video_1" controls autoplay playsinline webkit-playsinline style="width:100%; height:100%;">
                <source src="$currentStreamUrl" type="application/x-mpegURL">
                Votre navigateur ne supporte pas la lecture directe de ce flux.
            </video>
        </body>
        </html>
        """.trimIndent()
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("live_stream_screen"),
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = currentStreamTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDC2626)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (useNativePlayer) 
                                    (if (isFr) "Lecteur Natif Android (Stagefright/HLS)" else "Native Android Player (HLS)") 
                                else 
                                    (if (isFr) "Lecteur Web Compatible" else "Web Player Mode"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                playbackError = null
                                isPlayerLoading = true
                                if (useNativePlayer) {
                                    try {
                                        nativeMediaPlayer?.apply {
                                            reset()
                                            setDataSource(context, Uri.parse(currentStreamUrl))
                                            prepareAsync()
                                        }
                                    } catch (e: Exception) {
                                        Log.w("LiveStreamScreen", "Reload error: ${e.message}")
                                        playbackError = "Erreur rechargement: ${e.message}"
                                        isPlayerLoading = false
                                    }
                                } else {
                                    webViewInstance?.reload()
                                }
                                Toast.makeText(context, if (isFr) "Flux rechargé !" else "Stream reloaded!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("reload_stream_btn")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Recharger le flux", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = {
                                isFullscreen = !isFullscreen
                            },
                            modifier = Modifier.testTag("fullscreen_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = "Plein écran",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(currentStreamUrl), "video/*")
                                    }
                                    context.startActivity(Intent.createChooser(intent, if (isFr) "Lire le flux avec" else "Play stream with"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, if (isFr) "Aucun lecteur vidéo externe trouvé" else "No external player found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("open_external_player_btn")
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = "Lecteur externe", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = {
                                customUrlInput = currentStreamUrl
                                customTitleInput = currentStreamTitle
                                showCustomUrlDialog = true
                            },
                            modifier = Modifier.testTag("custom_stream_url_btn")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Éditer l'URL", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else innerPadding)
                .background(if (isFullscreen) Color.Black else MaterialTheme.colorScheme.background)
        ) {
            // === Video Player Viewport ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isFullscreen) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier
                                .height(250.dp)
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        }
                    )
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (useNativePlayer) {
                    // 1. Native TextureView with MediaPlayer (zero DRM SurfaceView rendernode MESA errors)
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                        try {
                                            isPlayerLoading = true
                                            playbackError = null
                                            val surface = Surface(surfaceTexture)
                                            val player = MediaPlayer().apply {
                                                setSurface(surface)
                                                setOnPreparedListener { mp ->
                                                    isPlayerLoading = false
                                                    playbackError = null
                                                    mp.isLooping = true
                                                    mp.start()
                                                }
                                                setOnErrorListener { _, what, extra ->
                                                    isPlayerLoading = false
                                                    playbackError = if (extra == -1004) {
                                                        if (isFr) "Flux inaccessible ou hors ligne (Erreur IO -1004)" else "Stream offline or unreachable (IO -1004)"
                                                    } else {
                                                        if (isFr) "Erreur de flux ($what, $extra)" else "Stream error ($what, $extra)"
                                                    }
                                                    Log.w("LiveStreamScreen", "Native MediaPlayer handled notice: what=$what, extra=$extra")
                                                    true // Handled gracefully, prevents framework crash
                                                }
                                                setDataSource(ctx, Uri.parse(currentStreamUrl))
                                                prepareAsync()
                                            }
                                            nativeMediaPlayer = player
                                        } catch (e: Exception) {
                                            Log.e("LiveStreamScreen", "Failed to start native TextureView MediaPlayer", e)
                                            playbackError = if (isFr) "Erreur d'initialisation du flux" else "Failed to initialize stream"
                                            isPlayerLoading = false
                                        }
                                        lastLoadedUrl = currentStreamUrl
                                    }

                                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                        try {
                                            nativeMediaPlayer?.stop()
                                            nativeMediaPlayer?.reset()
                                            nativeMediaPlayer?.release()
                                            nativeMediaPlayer = null
                                        } catch (e: Exception) {
                                            Log.w("LiveStreamScreen", "Player destroy error: ${e.message}")
                                        }
                                        return true
                                    }

                                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                                }
                            }
                        },
                        update = { _ ->
                            if (lastLoadedUrl != currentStreamUrl) {
                                lastLoadedUrl = currentStreamUrl
                                playbackError = null
                                isPlayerLoading = true
                                try {
                                    nativeMediaPlayer?.apply {
                                        reset()
                                        setDataSource(context, Uri.parse(currentStreamUrl))
                                        prepareAsync()
                                    }
                                } catch (e: Exception) {
                                    Log.e("LiveStreamScreen", "Failed to update native MediaPlayer", e)
                                    playbackError = if (isFr) "Flux inaccessible" else "Stream unreachable"
                                    isPlayerLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize().testTag("native_texture_player")
                    )
                } else {
                    // 2. Safe WebView Player with onRenderProcessGone crash protection
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                // Force software rendering on virtualized emulators to prevent MESA rendernode failure
                                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    loadsImagesAutomatically = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    allowFileAccess = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                }
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                        Log.w("LiveStreamScreen", "WebView notice: $description")
                                    }

                                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                        Log.e("LiveStreamScreen", "Chromium render process crashed safely caught. Switching to native player.")
                                        try {
                                            view?.destroy()
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                        // Auto-switch to Native VideoView on Chromium crash
                                        useNativePlayer = true
                                        return true // MANDATORY: Returns true to prevent Android terminating host app
                                    }
                                }
                                if (currentStreamUrl.contains(".m3u8")) {
                                    loadDataWithBaseURL("https://tvnow.best", htmlData, "text/html", "UTF-8", null)
                                } else {
                                    loadUrl(currentStreamUrl)
                                }
                                lastLoadedUrl = currentStreamUrl
                                webViewInstance = this
                            }
                        },
                        update = { view ->
                            if (lastLoadedUrl != currentStreamUrl) {
                                lastLoadedUrl = currentStreamUrl
                                if (currentStreamUrl.contains(".m3u8")) {
                                    view.loadDataWithBaseURL("https://tvnow.best", htmlData, "text/html", "UTF-8", null)
                                } else {
                                    view.loadUrl(currentStreamUrl)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize().testTag("webview_video_player")
                    )
                }

                // Loading Spinner
                if (isPlayerLoading && playbackError == null) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Friendly Error / Offline Overlay
                if (playbackError != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.94f),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444).copy(alpha = 0.8f))
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WifiOff,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (isFr) "Flux Temporairement Indisponible" else "Stream Currently Unavailable",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = playbackError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        playbackError = null
                                        isPlayerLoading = true
                                        try {
                                            nativeMediaPlayer?.apply {
                                                reset()
                                                setDataSource(context, Uri.parse(currentStreamUrl))
                                                prepareAsync()
                                            }
                                        } catch (e: Exception) {
                                            playbackError = "Flux inaccessible"
                                            isPlayerLoading = false
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isFr) "Réessayer" else "Retry")
                                }

                                FilledTonalButton(
                                    onClick = {
                                        useNativePlayer = false
                                        viewModel.setStream(
                                            "https://retailers-mitchell-customs-rotation.trycloudflare.com/logic_cover.html",
                                            "Z-CORE Logic Cover (979V11)"
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isFr) "Ouvrir Z-CORE" else "Open Z-CORE")
                                }
                            }
                        }
                    }
                }

                // Exit Fullscreen Floating Button when in fullscreen mode
                if (isFullscreen) {
                    IconButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Filled.FullscreenExit, contentDescription = "Quitter plein écran", tint = Color.White)
                    }
                }
            }

            // === Controls & Channel List (Hidden in Fullscreen) ===
            if (!isFullscreen) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                ) {
                    // Player Status Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                        Text(
                                            text = if (isFr) "Flux Connecté & Synchronisé" else "Stream Connected & Synchronized",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = if (useNativePlayer) "Natif Android" else "Web HLS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = currentStreamUrl,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 11.sp,
                                    maxLines = 2
                                )

                                // Player engine selector toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (useNativePlayer) Icons.Filled.Shield else Icons.Outlined.Web,
                                            contentDescription = null,
                                            tint = if (useNativePlayer) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isFr) "Moteur Natif (Sans Crash Chromium)" else "Native Engine (Zero Crash)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Switch(
                                        checked = useNativePlayer,
                                        onCheckedChange = { checked ->
                                            useNativePlayer = checked
                                            Toast.makeText(
                                                context,
                                                if (checked) 
                                                    (if (isFr) "Moteur Natif Android actif" else "Native Android engine active") 
                                                else 
                                                    (if (isFr) "Moteur Web actif" else "Web engine active"),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.setTab(NavigationTab.RECORDER_VAULT)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (isFr) "Enregistrer Audio" else "Record Audio", style = MaterialTheme.typography.labelMedium)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Stream URL", currentStreamUrl)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, if (isFr) "URL copiée !" else "URL copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (isFr) "Copier URL" else "Copy URL", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    // Available Stream Presets
                    item {
                        Text(
                            text = if (isFr) "Chaînes & Flux Disponibles" else "Available Streams & Channels",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(presets.size) { index ->
                        val preset = presets[index]
                        val isSelected = preset.streamUrl == currentStreamUrl

                        Card(
                            onClick = {
                                if (preset.streamUrl.contains(".m3u8")) {
                                    useNativePlayer = true
                                } else {
                                    useNativePlayer = false
                                }
                                viewModel.setStream(preset.streamUrl, preset.title)
                                Toast.makeText(context, if (isFr) "Chargement de ${preset.title}..." else "Loading ${preset.title}...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stream_preset_${preset.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected)
                                CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
                            else
                                null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Tv,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = preset.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${preset.category} • ${preset.resolution}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF10B981)
                                    ) {
                                        Text(
                                            text = "ACTIF",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Lire",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog to enter custom stream URL
    if (showCustomUrlDialog) {
        AlertDialog(
            onDismissRequest = { showCustomUrlDialog = false },
            title = {
                Text(
                    text = if (isFr) "Flux IPTV / HLS Personnalisé" else "Custom IPTV / HLS Stream",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = customTitleInput,
                        onValueChange = { customTitleInput = it },
                        label = { Text(if (isFr) "Nom de la Chaîne" else "Channel Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        label = { Text("URL du flux (m3u8 / HLS)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customUrlInput.isNotBlank()) {
                            val title = if (customTitleInput.isNotBlank()) customTitleInput else "Flux Personnalisé"
                            viewModel.setStream(customUrlInput.trim(), title)
                            showCustomUrlDialog = false
                            Toast.makeText(context, if (isFr) "Flux configuré !" else "Stream updated!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(if (isFr) "Appliquer" else "Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomUrlDialog = false }) {
                    Text(if (isFr) "Annuler" else "Cancel")
                }
            }
        )
    }
}
