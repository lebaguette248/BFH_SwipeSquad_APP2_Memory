package com.swipesquad.memory

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.swipesquad.memory.ui.theme.MemoryTheme
import org.json.JSONObject

class MainActivity : ComponentActivity() {
//    TODO:
//    - Make pair cards in a scrollable view
//    - Implement sendLogbookIntent function to the send button with correct json structure
//    - Make the current highlighted qr code visible with color or some other ui feature

    override fun onCreate(savedInstanceState: Bundle?) {
        val allScans = mutableStateListOf<QRItem>()
        val pairs = mutableStateListOf<List<QRItem>>()
        val selected = mutableStateListOf<QRItem>()

        val qrLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    data?.getParcelableExtra("image_uri", Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    data?.getParcelableExtra("image_uri")
                }
                val qrCodeValue = data?.getStringExtra("qr_code_value")

                if (imageUri != null && qrCodeValue != null) {
                    val item = QRItem(imageUri, qrCodeValue)
                    allScans.add(item)
                }

                Toast.makeText(this, "New scan added", Toast.LENGTH_SHORT).show()
            }
        }

        val cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // Launch the QR camera activity
                qrLauncher.launch(Intent(this, CameraActivity::class.java))
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MemoryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        requestCameraPermission = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        allScans = allScans,
                        pairs = pairs,
                        onSelect = { item ->
                            selected.add(item)
                            if (selected.size == 2) {
                                pairs.add(selected.toList())
                                allScans.removeAll(selected)
                                selected.clear()
                            }
                        },
                        onDeletePair = { pair ->
                            pairs.remove(pair)
                            allScans.addAll(pair)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }
}

fun sendLogbookIntent(context: Context, value: String) {
    val log = JSONObject()
    log.put("task", "Metalldetektor")
    log.put("solution", value)

    val intent = Intent("ch.apprun.intent.LOG").apply {
        putExtra("ch.apprun.logmessage", log.toString())
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Log.e("Logger", "LogBook application is not installed on this device.")
    }
}


@Composable
fun MainScreen(
    requestCameraPermission: () -> Unit,
    allScans: List<QRItem>,
    pairs: List<List<QRItem>>,
    onSelect: (QRItem) -> Unit,
    onDeletePair: (List<QRItem>) -> Unit
) {
    Scaffold(
        topBar = { Header() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { requestCameraPermission() },
                modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            ScanGallery(
                allScans = allScans,
                pairs = pairs,
                onSelect = onSelect,
                onDeletePair = onDeletePair,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header() {
    TopAppBar(
        title = { Text("Memory") },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary
        ),
        actions = {
            IconButton(onClick = { /* TODO: handle action */ }) {
                Icon(
                    tint = MaterialTheme.colorScheme.primary,
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send to logbook"
                )
            }
        }
    )
}

@Composable
fun ScanGallery(
    allScans: List<QRItem>,
    pairs: List<List<QRItem>>,
    onSelect: (QRItem) -> Unit,
    onDeletePair: (List<QRItem>) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Pairs:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        pairs.forEachIndexed { index, pair ->
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pair ${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding( 12.dp).padding(bottom = 4.dp)
                            )
                            IconButton(onClick = { onDeletePair(pair) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete pair",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            pair.forEach { item ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = item.value,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 180.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = item.value,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Unpaired QR-Codes:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(allScans) { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(160.dp)
                        .clickable { onSelect(item) }
                ) {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.value,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoPreviewScreen(imageUri: Uri?, qrValue: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Captured photo",
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Text("No photo taken yet")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (qrValue != null) {
            Text("QR Code Value: $qrValue")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MemoryTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreen(
                requestCameraPermission = TODO(),
                allScans = TODO(),
                pairs = TODO(),
                onSelect = TODO(),
                onDeletePair = TODO()
            )
        }
    }
}