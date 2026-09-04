package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.example.util.QRCodeUtils
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONArray
import org.json.JSONObject

enum class QRPresetType(val title: String) {
  URL("URL"),
  WIFI("Wi-Fi"),
  VCARD("Contact"),
  EMAIL("Email"),
  SMS("SMS"),
  TEXT("Text")
}

data class ColorThemeOption(
  val name: String,
  val fg: Color,
  val bg: Color,
  val fgHex: String,
  val bgHex: String
)

data class QRHistoryItem(
  val id: Long,
  val title: String,
  val type: String,
  val content: String,
  val fgHex: String,
  val emblem: String?,
  val timestamp: Long
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          QRCodeGeneratorScreen(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          )
        }
      }
    }
  }
}

@Composable
fun QRCodeGeneratorScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val prefs = remember { context.getSharedPreferences("qr_history_prefs_v3", Context.MODE_PRIVATE) }

  // Presets State
  var activePreset by rememberSaveable { mutableStateOf(QRPresetType.URL) }

  // Form Fields
  var urlInput by rememberSaveable { mutableStateOf("https://ai.studio") }
  var wifiSsid by rememberSaveable { mutableStateOf("") }
  var wifiPass by rememberSaveable { mutableStateOf("") }
  var wifiType by rememberSaveable { mutableStateOf("WPA") }

  var vcardFirst by rememberSaveable { mutableStateOf("") }
  var vcardLast by rememberSaveable { mutableStateOf("") }
  var vcardPhone by rememberSaveable { mutableStateOf("") }
  var vcardEmail by rememberSaveable { mutableStateOf("") }
  var vcardOrg by rememberSaveable { mutableStateOf("") }

  var emailTo by rememberSaveable { mutableStateOf("") }
  var emailSubject by rememberSaveable { mutableStateOf("") }
  var emailBody by rememberSaveable { mutableStateOf("") }

  var smsPhone by rememberSaveable { mutableStateOf("") }
  var smsMsg by rememberSaveable { mutableStateOf("") }

  var textRaw by rememberSaveable { mutableStateOf("") }

  // General textInput used by tests and raw input
  var textInput by rememberSaveable { mutableStateOf("https://ai.studio") }
  var generatedText by rememberSaveable { mutableStateOf("") }
  var generatedTitle by rememberSaveable { mutableStateOf("Website") }
  var selectedSize by rememberSaveable { mutableIntStateOf(512) }
  var selectedEcc by rememberSaveable { mutableStateOf("H") }
  var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

  // Toggles: Live Auto-Update and Frame Mode
  var livePreviewEnabled by rememberSaveable { mutableStateOf(true) }
  var frameModeEnabled by rememberSaveable { mutableStateOf(false) }

  // Center Emblem Badges
  val emblemList = remember { listOf("None", "🔗", "📶", "👤", "⭐", "⚡", "❤️") }
  var selectedEmblemIndex by rememberSaveable { mutableIntStateOf(0) }
  val activeEmblem = if (selectedEmblemIndex > 0) emblemList[selectedEmblemIndex] else null

  // Themes
  val colorThemes = remember {
    listOf(
      ColorThemeOption("Obsidian", Color(0xFF0F172A), Color(0xFFFFFFFF), "#0f172a", "#ffffff"),
      ColorThemeOption("Indigo", Color(0xFF4338CA), Color(0xFFFFFFFF), "#4338ca", "#ffffff"),
      ColorThemeOption("Emerald", Color(0xFF047857), Color(0xFFFFFFFF), "#047857", "#ffffff"),
      ColorThemeOption("Sunset", Color(0xFFC2410C), Color(0xFFFFFFFF), "#c2410c", "#ffffff"),
      ColorThemeOption("Violet", Color(0xFF6D28D9), Color(0xFFFFFFFF), "#6d28d9", "#ffffff"),
      ColorThemeOption("Inverted", Color(0xFFFFFFFF), Color(0xFF0F172A), "#ffffff", "#0f172a")
    )
  }
  var selectedThemeIndex by rememberSaveable { mutableIntStateOf(0) }
  val activeTheme = colorThemes[selectedThemeIndex]

  // Local Generation History
  val historyItems = remember { mutableStateListOf<QRHistoryItem>() }
  var historyFilterType by rememberSaveable { mutableStateOf("All") }

  // Load history from SharedPreferences on start
  LaunchedEffect(Unit) {
    try {
      val raw = prefs.getString("history_json", null)
      if (raw != null) {
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          historyItems.add(
            QRHistoryItem(
              id = obj.getLong("id"),
              title = obj.getString("title"),
              type = obj.getString("type"),
              content = obj.getString("content"),
              fgHex = obj.optString("fgHex", "#0f172a"),
              emblem = if (obj.has("emblem")) obj.getString("emblem") else null,
              timestamp = obj.getLong("timestamp")
            )
          )
        }
      }
    } catch (_: Exception) {}
  }

  fun saveHistoryToPrefs() {
    try {
      val arr = JSONArray()
      for (item in historyItems.take(20)) {
        val obj = JSONObject().apply {
          put("id", item.id)
          put("title", item.title)
          put("type", item.type)
          put("content", item.content)
          put("fgHex", item.fgHex)
          if (item.emblem != null) put("emblem", item.emblem)
          put("timestamp", item.timestamp)
        }
        arr.put(obj)
      }
      prefs.edit().putString("history_json", arr.toString()).apply()
    } catch (_: Exception) {}
  }

  // Construct payload from active preset
  fun getActiveContent(): Pair<String, String> {
    return when (activePreset) {
      QRPresetType.URL -> {
        val trimmed = if (textInput.isNotBlank()) textInput else urlInput.trim()
        val title = trimmed.replace(Regex("^https?://", RegexOption.IGNORE_CASE), "").take(20).ifBlank { "URL" }
        Pair(trimmed, title)
      }
      QRPresetType.WIFI -> {
        val encoded = "WIFI:S:$wifiSsid;T:$wifiType;P:$wifiPass;H:false;;"
        Pair(encoded, if (wifiSsid.isNotBlank()) "Wi-Fi: $wifiSsid" else "Wi-Fi")
      }
      QRPresetType.VCARD -> {
        val fn = "$vcardFirst $vcardLast".trim().ifBlank { vcardPhone }.ifBlank { vcardEmail }
        val vcard = listOfNotNull(
          "BEGIN:VCARD",
          "VERSION:3.0",
          "N:$vcardLast;$vcardFirst;;;",
          "FN:$fn",
          if (vcardOrg.isNotBlank()) "ORG:$vcardOrg" else null,
          if (vcardPhone.isNotBlank()) "TEL;TYPE=CELL:$vcardPhone" else null,
          if (vcardEmail.isNotBlank()) "EMAIL:$vcardEmail" else null,
          "END:VCARD"
        ).joinToString("\n")
        Pair(vcard, if (fn.isNotBlank()) fn else "Contact")
      }
      QRPresetType.EMAIL -> {
        val mailto = "mailto:$emailTo?subject=$emailSubject&body=$emailBody"
        Pair(mailto, if (emailTo.isNotBlank()) "Email: $emailTo" else "Email")
      }
      QRPresetType.SMS -> {
        val sms = "smsto:$smsPhone${if (smsMsg.isNotBlank()) ":$smsMsg" else ""}"
        Pair(sms, if (smsPhone.isNotBlank()) "SMS: $smsPhone" else "SMS")
      }
      QRPresetType.TEXT -> {
        val raw = if (textInput.isNotBlank()) textInput else textRaw.trim()
        Pair(raw, raw.take(20).ifBlank { "Text" })
      }
    }
  }

  fun generateCurrentQR(pushToHistory: Boolean = true) {
    val (payload, title) = getActiveContent()
    if (payload.isBlank()) {
      if (pushToHistory) Toast.makeText(context, "Please enter content", Toast.LENGTH_SHORT).show()
      return
    }
    val ecc = when (selectedEcc) {
      "L" -> ErrorCorrectionLevel.L
      "M" -> ErrorCorrectionLevel.M
      "Q" -> ErrorCorrectionLevel.Q
      else -> ErrorCorrectionLevel.H
    }
    qrBitmap = QRCodeUtils.generateQRCodeBitmap(
      content = payload,
      size = selectedSize,
      ecc = ecc,
      fgColor = activeTheme.fg.toArgb(),
      bgColor = activeTheme.bg.toArgb(),
      centerBadge = activeEmblem
    )
    generatedText = payload
    generatedTitle = title

    if (pushToHistory) {
      historyItems.removeAll { it.content == payload }
      historyItems.add(
        0,
        QRHistoryItem(
          id = System.currentTimeMillis(),
          title = title,
          type = activePreset.title,
          content = payload,
          fgHex = activeTheme.fgHex,
          emblem = activeEmblem,
          timestamp = System.currentTimeMillis()
        )
      )
      saveHistoryToPrefs()
    }
  }

  // Live Auto-Update Reactive Effect
  val (currentPayload, _) = getActiveContent()
  LaunchedEffect(currentPayload, activeTheme, selectedEcc, activeEmblem, livePreviewEnabled) {
    if (livePreviewEnabled && currentPayload.isNotBlank()) {
      generateCurrentQR(pushToHistory = false)
    }
  }

  val scrollState = rememberScrollState()

  Box(
    modifier = modifier
      .background(MaterialTheme.colorScheme.background)
      .fillMaxSize(),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 600.dp)
        .fillMaxWidth()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // 1. Sleek Header Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp),
            shadowElevation = 4.dp
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "App Icon",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
              )
            }
          }
          Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
              )
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
              ) {
                Text(
                  text = "PRO ULTRA",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = stringResource(R.string.header_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Live Auto-Update Pill
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = if (livePreviewEnabled) Color(0xFFECFDF5) else MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, if (livePreviewEnabled) Color(0xFFA7F3D0) else Color.Transparent),
          modifier = Modifier.clickable {
            livePreviewEnabled = !livePreviewEnabled
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .background(if (livePreviewEnabled) Color(0xFF10B981) else Color.Gray, CircleShape)
            )
            Text(
              text = if (livePreviewEnabled) "Live" else "Manual",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
              color = if (livePreviewEnabled) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // 2. Smart Preset Filter Chips Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        QRPresetType.values().forEach { preset ->
          val isSelected = activePreset == preset
          FilterChip(
            selected = isSelected,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              activePreset = preset
              if (preset == QRPresetType.URL) textInput = urlInput
            },
            label = {
              Text(
                preset.title,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
              )
            },
            leadingIcon = {
              val icon = when (preset) {
                QRPresetType.URL -> Icons.Default.Link
                QRPresetType.WIFI -> Icons.Default.Wifi
                QRPresetType.VCARD -> Icons.Default.Person
                QRPresetType.EMAIL -> Icons.Default.Email
                QRPresetType.SMS -> Icons.Default.Sms
                QRPresetType.TEXT -> Icons.Default.Description
              }
              Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
            )
          )
        }
      }

      // 3. Input Card Container with Animated Content
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Dynamic Fields with Demo Fill Action
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = activePreset.title + " Settings",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            // One-tap Demo Data Fill
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
              modifier = Modifier.clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                when (activePreset) {
                  QRPresetType.URL -> {
                    urlInput = "https://ai.studio/build"
                    textInput = "https://ai.studio/build"
                  }
                  QRPresetType.WIFI -> {
                    wifiSsid = "Studio_5GHz_HighSpeed"
                    wifiPass = "SuperSecret2026!"
                    wifiType = "WPA"
                  }
                  QRPresetType.VCARD -> {
                    vcardFirst = "Jordan"
                    vcardLast = "Hayes"
                    vcardPhone = "+1 (415) 555-0182"
                    vcardEmail = "jordan.hayes@company.io"
                    vcardOrg = "Head of Product Design"
                  }
                  QRPresetType.EMAIL -> {
                    emailTo = "hello@example.org"
                    emailSubject = "Project Inquiry"
                    emailBody = "Excited to connect with you!"
                  }
                  QRPresetType.SMS -> {
                    smsPhone = "+1 202 555 0143"
                    smsMsg = "Reservation confirmed for 7:30 PM."
                  }
                  QRPresetType.TEXT -> {
                    textRaw = "CONFIDENTIAL-KEY-8837-XJ92"
                    textInput = "CONFIDENTIAL-KEY-8837-XJ92"
                  }
                }
                generateCurrentQR(pushToHistory = false)
                Toast.makeText(context, "Filled demo sample", Toast.LENGTH_SHORT).show()
              }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.demo_button), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              }
            }
          }

          // Animated Form View
          AnimatedContent(
            targetState = activePreset,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "PresetFields"
          ) { preset ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              when (preset) {
                QRPresetType.URL -> {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                  ) {
                    OutlinedButton(
                      onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                          val text = clip.getItemAt(0).text?.toString() ?: ""
                          if (text.isNotBlank()) {
                            textInput = text
                            urlInput = text
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          }
                        }
                      },
                      contentPadding = ButtonDefaults.TextButtonContentPadding,
                      modifier = Modifier.height(28.dp),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(12.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Paste", fontSize = 11.sp)
                    }
                  }

                  OutlinedTextField(
                    value = textInput,
                    onValueChange = {
                      textInput = it
                      urlInput = it
                    },
                    placeholder = { Text("https://example.com", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("qr_text_input")
                  )
                }

                QRPresetType.WIFI -> {
                  OutlinedTextField(
                    value = wifiSsid,
                    onValueChange = { wifiSsid = it },
                    label = { Text("Network Name (SSID)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                  )
                  OutlinedTextField(
                    value = wifiPass,
                    onValueChange = { wifiPass = it },
                    label = { Text("Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                  )
                }

                QRPresetType.VCARD -> {
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                      value = vcardFirst,
                      onValueChange = { vcardFirst = it },
                      label = { Text("First Name") },
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                      value = vcardLast,
                      onValueChange = { vcardLast = it },
                      label = { Text("Last Name") },
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(12.dp)
                    )
                  }
                  OutlinedTextField(
                    value = vcardPhone,
                    onValueChange = { vcardPhone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                  )
                  OutlinedTextField(
                    value = vcardEmail,
                    onValueChange = { vcardEmail = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                  )
                }

                QRPresetType.EMAIL -> {
                  OutlinedTextField(
                    value = emailTo,
                    onValueChange = { emailTo = it },
                    label = { Text("Recipient Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                  )
                  OutlinedTextField(
                    value = emailSubject,
                    onValueChange = { emailSubject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                  )
                }

                QRPresetType.SMS -> {
                  OutlinedTextField(
                    value = smsPhone,
                    onValueChange = { smsPhone = it },
                    label = { Text("Recipient Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                  )
                  OutlinedTextField(
                    value = smsMsg,
                    onValueChange = { smsMsg = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                  )
                }

                QRPresetType.TEXT -> {
                  OutlinedTextField(
                    value = textInput,
                    onValueChange = {
                      textInput = it
                      textRaw = it
                    },
                    placeholder = { Text(stringResource(R.string.input_placeholder), fontSize = 13.sp) },
                    minLines = 3,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("qr_text_input")
                  )
                }
              }
            }
          }

          // Live Density & Metric Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            val len = currentPayload.length
            val densityText = if (len < 80) "Optimal Density" else if (len < 250) "Balanced" else "Dense"
            val densityColor = if (len < 80) Color(0xFF047857) else if (len < 250) Color(0xFF2563EB) else Color(0xFFD97706)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Box(modifier = Modifier.size(6.dp).background(densityColor, CircleShape))
              Text(
                text = "$len chars • $densityText",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = densityColor)
              )
            }

            Text(
              text = "Level H (30% ECC)",
              style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }

          // 4. Center Emblem / Badge Selector
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = stringResource(R.string.center_emblem_title),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              emblemList.forEachIndexed { idx, emblem ->
                val isSelected = selectedEmblemIndex == idx
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  border = BorderStroke(if (isSelected) 1.5.dp else 0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray),
                  modifier = Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedEmblemIndex = idx
                    generateCurrentQR(pushToHistory = false)
                  }
                ) {
                  Text(
                    text = emblem,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
              }
            }
          }

          // 5. Color Theme Selector
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = stringResource(R.string.color_themes_title),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              colorThemes.forEachIndexed { idx, theme ->
                val isSelected = selectedThemeIndex == idx
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  border = BorderStroke(if (isSelected) 1.5.dp else 0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray),
                  modifier = Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedThemeIndex = idx
                    generateCurrentQR(pushToHistory = false)
                  }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(14.dp)
                        .background(theme.fg, CircleShape)
                        .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                    )
                    Text(theme.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                  }
                }
              }
            }
          }

          // Frame Poster Switch
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = stringResource(R.string.scan_frame_title),
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Switch(
              checked = frameModeEnabled,
              onCheckedChange = {
                frameModeEnabled = it
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
              )
            )
          }

          // Manual Generate Button
          Button(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              generateCurrentQR(pushToHistory = true)
              Toast.makeText(context, "Generated QR Code", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("generate_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.generate_button),
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 6. QR Code Output Card with Smooth Animation
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("qr_code_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          if (qrBitmap != null) {
            // Framed or Pure Presentation
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = activeTheme.bg,
              border = BorderStroke(if (frameModeEnabled) 2.dp else 1.dp, if (frameModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier
                .size(if (frameModeEnabled) 270.dp else 240.dp)
                .padding(4.dp)
            ) {
              Column(
                modifier = Modifier.fillMaxSize().padding(if (frameModeEnabled) 10.dp else 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                if (frameModeEnabled) {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.padding(bottom = 6.dp)
                  ) {
                    Text(
                      text = "SCAN WITH CAMERA",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                  }
                }

                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                  Image(
                    bitmap = qrBitmap!!.asImageBitmap(),
                    contentDescription = "Generated QR Code",
                    modifier = Modifier
                      .fillMaxSize()
                      .clip(RoundedCornerShape(8.dp))
                  )
                }

                if (frameModeEnabled) {
                  Text(
                    text = generatedTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeTheme.fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                  )
                }
              }
            }

            // Dual Export: PNG and Vector SVG Buttons
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // PNG Download
              Button(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  qrBitmap?.let { bmp ->
                    val success = QRCodeUtils.saveBitmapToGallery(context, bmp, generatedText)
                    if (success) {
                      Toast.makeText(context, context.getString(R.string.saved_success), Toast.LENGTH_SHORT).show()
                    } else {
                      Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
                    }
                  }
                },
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
                  .testTag("download_button"),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.Download, contentDescription = "Download PNG", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.download_button), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
              }

              // Vector SVG Export Button
              OutlinedButton(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  val ecc = when (selectedEcc) {
                    "L" -> ErrorCorrectionLevel.L
                    "M" -> ErrorCorrectionLevel.M
                    "Q" -> ErrorCorrectionLevel.Q
                    else -> ErrorCorrectionLevel.H
                  }
                  val svg = QRCodeUtils.generateQRCodeSvg(
                    content = generatedText,
                    ecc = ecc,
                    fgColorHex = activeTheme.fgHex,
                    bgColorHex = activeTheme.bgHex,
                    centerBadge = activeEmblem
                  )
                  if (svg != null) {
                    val success = QRCodeUtils.saveSvgToDocuments(context, svg, generatedText)
                    if (success) {
                      Toast.makeText(context, context.getString(R.string.saved_svg_success), Toast.LENGTH_SHORT).show()
                    } else {
                      QRCodeUtils.shareSvg(context, svg, generatedText)
                    }
                  }
                },
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Vector SVG", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.download_svg_button), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
              }
            }

            // Secondary Action Row: Share and Copy
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  qrBitmap?.let { bmp ->
                    QRCodeUtils.shareBitmap(context, bmp, generatedText)
                  }
                },
                modifier = Modifier
                  .weight(1f)
                  .height(38.dp)
                  .testTag("share_button"),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.share_button), fontSize = 12.sp)
              }

              OutlinedButton(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("QR Text", generatedText)
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                  .weight(1f)
                  .height(38.dp)
                  .testTag("copy_button"),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.copy_button), fontSize = 12.sp)
              }
            }
          } else {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(24.dp)
            ) {
              Icon(
                Icons.Default.QrCode,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                stringResource(R.string.qr_placeholder_title),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
              )
              Text(
                stringResource(R.string.qr_placeholder_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // 7. Local Generation History Section
      Spacer(modifier = Modifier.height(16.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
              Text(
                stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
              ) {
                Text(
                  text = "${historyItems.size}",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            if (historyItems.isNotEmpty()) {
              Text(
                text = "Clear",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  historyItems.clear()
                  saveHistoryToPrefs()
                }
              )
            }
          }

          if (historyItems.isEmpty()) {
            Text(
              stringResource(R.string.history_empty),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.padding(vertical = 8.dp)
            )
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              historyItems.take(5).forEach { item ->
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                  border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Row(
                      modifier = Modifier.weight(1f),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                      ) {
                        Text(
                          item.type,
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                      }
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          item.title,
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 12.sp,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Text(
                          item.content,
                          fontSize = 10.sp,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                      IconButton(
                        onClick = {
                          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          textInput = item.content
                          generateCurrentQR(pushToHistory = false)
                          Toast.makeText(context, "Loaded \"${item.title}\"", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                      ) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore", modifier = Modifier.size(16.dp))
                      }
                      IconButton(
                        onClick = {
                          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          historyItems.remove(item)
                          saveHistoryToPrefs()
                        },
                        modifier = Modifier.size(28.dp)
                      ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Footer
      Row(
        modifier = Modifier.padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          modifier = Modifier.size(14.dp),
          tint = Color(0xFF10B981)
        )
        Text(
          text = "Client-side generation • Lossless Vector SVG • Zero telemetry",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme {
    QRCodeGeneratorScreen()
  }
}
