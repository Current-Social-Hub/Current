package com.current.hub

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.Path
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.util.Log
import android.widget.Toast
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.current.hub.ui.theme.CurrentTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import androidx.credentials.*
import com.google.android.libraries.identity.googleid.*
import androidx.graphics.shapes.*
import androidx.compose.ui.graphics.asComposePath
import kotlin.random.Random

// Composition Locals
val LocalThemeMode = compositionLocalOf { "System" }
val LocalManualFontScale = compositionLocalOf { 3f }
val LocalAccentColorIndex = compositionLocalOf { 0 }
val LocalPureBlack = compositionLocalOf { false }
val LocalHighContrast = compositionLocalOf { false }
val LocalDynamicColor = compositionLocalOf { false }
val LocalMergeDuplicates = compositionLocalOf { false }
val LocalShowDuration = compositionLocalOf { true }
val LocalShowUnknown = compositionLocalOf { true }
val LocalHistoryLimit = compositionLocalOf { 100 }
val LocalCompactView = compositionLocalOf { false }
val LocalSwipeActionsEnabled = compositionLocalOf { true }
val LocalShowFrequentHub = compositionLocalOf { true }
val LocalStrictGrouping = compositionLocalOf { false }
val LocalShowIncoming = compositionLocalOf { true }
val LocalShowOutgoing = compositionLocalOf { true }
val LocalShowMissed = compositionLocalOf { true }
val LocalShowBlocked = compositionLocalOf { true }
val LocalFavoriteNumbers = compositionLocalOf { emptySet<String>() }
val LocalIncomingAlerts = compositionLocalOf { true }
val LocalMissedAlerts = compositionLocalOf { true }
val LocalVibrationIntensity = compositionLocalOf { 1 }
val LocalPrivacyMode = compositionLocalOf { false }
val LocalShowDesignGrid = compositionLocalOf { false }
val LocalAnimationMultiplier = compositionLocalOf { 1f }
val LocalDeletedCallIds = compositionLocalOf { emptySet<Long>() }
val LocalIsDemoMode = compositionLocalOf { false }

val GoogleSansFlex = FontFamily.Default
private const val WEB_CLIENT_ID = "340753714127-fm8i8ab477bq0ktb86v35mu7a7tm5muo.apps.googleusercontent.com"

data class UserSession(val name: String, val email: String, val photoUrl: String?, val phoneNumber: String?)
data class Contact(val id: String, val name: String, val number: String, val photoUri: String?)
data class ActiveCall(val name: String, val number: String, val photoUri: String?)
data class CallRecord(val id: Long, val name: String?, val number: String, val type: Int, val date: Long, val duration: Long, val photoUri: String?)

object VibrantTheme {
    val IconColors = listOf(
        Color(0xFF7ABAF2) to Color(0xFF003355), // Blue
        Color(0xFF78D9EC) to Color(0xFF00363D), // Cyan
        Color(0xFF90A4FF) to Color(0xFF001452), // Indigo
        Color(0xFFFFB2BC) to Color(0xFF3F0015), // Pink
        Color(0xFFE5B8F4) to Color(0xFF35004F), // Magenta
        Color(0xFFFFB870) to Color(0xFF4B2800)  // Orange
    )
    val Blue = IconColors[0]
    val Cyan = IconColors[1]
    val Indigo = IconColors[2]
    val Pink = IconColors[3]
    val Magenta = IconColors[4]
    val Orange = IconColors[5]
    val SystemGrey = Color(0xFFDDE3EA) to Color(0xFF444746)
    val Red = Color(0xFFFFB4AB)
    val RedOn = Color(0xFF690005)

    @Composable fun background(): Color {
        val dark = isSystemInDarkThemeCustom(); val pure = LocalPureBlack.current; val dyn = LocalDynamicColor.current
        return when { 
            dyn && Build.VERSION.SDK_INT >= 31 -> MaterialTheme.colorScheme.background
            dark && pure -> Color.Black
            dark -> Color(0xFF001F3F)
            else -> Color.White 
        }
    }
    @Composable fun cardBackground(): Color {
        if (LocalDynamicColor.current && Build.VERSION.SDK_INT >= 31) return MaterialTheme.colorScheme.surfaceContainer
        return if (isSystemInDarkThemeCustom()) Color(0xFF083061) else Color(0xFFF0F4F9)
    }
    @Composable fun surfaceHigh(): Color {
        if (LocalDynamicColor.current && Build.VERSION.SDK_INT >= 31) return MaterialTheme.colorScheme.surfaceContainerHigh
        return if (isSystemInDarkThemeCustom()) Color(0xFF104482) else Color(0xFFE1E8F0)
    }
    @Composable fun textPrimary(): Color {
        if (LocalDynamicColor.current && Build.VERSION.SDK_INT >= 31) return MaterialTheme.colorScheme.onSurface
        return if (isSystemInDarkThemeCustom()) Color.White else Color(0xFF1B1B1F)
    }
    @Composable fun textSecondary(): Color {
        val dark = isSystemInDarkThemeCustom(); val high = LocalHighContrast.current; val dyn = LocalDynamicColor.current
        if (dyn && Build.VERSION.SDK_INT >= 31 && !high) return MaterialTheme.colorScheme.onSurfaceVariant
        return when { high && dark -> Color.White.copy(0.9f); high && !dark -> Color.Black.copy(0.9f); dark -> Color(0xFFC7C5D0); else -> Color(0xFF44474E) }
    }
    @Composable fun accent(): Color { if (LocalDynamicColor.current && Build.VERSION.SDK_INT >= 31) return MaterialTheme.colorScheme.primary; return IconColors[LocalAccentColorIndex.current % IconColors.size].first }
    @Composable fun onAccent(): Color { if (LocalDynamicColor.current && Build.VERSION.SDK_INT >= 31) return MaterialTheme.colorScheme.onPrimary; return IconColors[LocalAccentColorIndex.current % IconColors.size].second }
}

@Composable fun isSystemInDarkThemeCustom() = when (LocalThemeMode.current) { "Dark" -> true; "Light" -> false; else -> isSystemInDarkTheme() }

fun shapeFor(idx: Int, total: Int) = when { total == 1 -> RoundedCornerShape(28.dp); idx == 0 -> RoundedCornerShape(28.dp, 28.dp, 4.dp, 4.dp); idx == total - 1 -> RoundedCornerShape(4.dp, 4.dp, 28.dp, 28.dp); else -> RoundedCornerShape(4.dp) }

@Composable
fun rememberRandomPolygonShape(): Shape {
    return remember {
        val allowedPolygons = listOf(
            RoundedPolygon(numVertices = 3, rounding = CornerRounding(0.2f)), // Triangle
            RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.15f)), // Square
            RoundedPolygon(numVertices = 5, rounding = CornerRounding(0.2f)), // Pentagon
            RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.2f)), // Hexagon
            RoundedPolygon(numVertices = 8, rounding = CornerRounding(0.15f)), // Octagon
            RoundedPolygon(numVertices = 12, rounding = CornerRounding(0.1f)), // Dodecagon
            RoundedPolygon.star(numVerticesPerRadius = 6, innerRadius = 0.7f, rounding = CornerRounding(0.1f)), // Soft star
            RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.8f, rounding = CornerRounding(0.15f)) // Gem-like
        )
        allowedPolygons.random().toShape()
    }
}

fun RoundedPolygon.toShape(): Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val bounds = calculateBounds()
        val width = bounds[2] - bounds[0]
        val height = bounds[3] - bounds[1]
        val scaleX = size.width / width
        val scaleY = size.height / height
        val scale = min(scaleX, scaleY)
        
        val matrix = Matrix().apply {
            postTranslate(-bounds[0], -bounds[1])
            postScale(scale, scale)
            postTranslate((size.width - width * scale) / 2f, (size.height - height * scale) / 2f)
        }
        
        val path = Path()
        this@toShape.toPath(path)
        path.transform(matrix)
        return Outline.Generic(path.asComposePath())
    }
}

val ClamshellShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = androidx.compose.ui.graphics.Path().apply {
            val w = size.width
            val h = size.height
            val r = h / 2
            moveTo(r, 0f)
            lineTo(w - r, 0f)
            quadraticTo(w, 0f, w, r)
            quadraticTo(w, h, w - r, h)
            lineTo(r, h)
            quadraticTo(0f, h, 0f, r)
            quadraticTo(0f, 0f, r, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable fun HubSwitchItem(title: String, subtitle: String?, checked: Boolean, onToggle: (Boolean) -> Unit, icon: ImageVector, shape: Shape, enabled: Boolean = true) {
    Surface(onClick = { if (enabled) onToggle(!checked) }, shape = shape, color = VibrantTheme.cardBackground(), modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f)) { Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(VibrantTheme.surfaceHigh()), contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, tint = VibrantTheme.textPrimary(), modifier = Modifier.size(20.dp)) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VibrantTheme.textPrimary()); if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = VibrantTheme.textSecondary()) }; Switch(checked, onToggle, enabled = enabled, thumbContent = if (checked) { { Icon(Icons.Filled.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } } else null, colors = SwitchDefaults.colors(checkedThumbColor = VibrantTheme.onAccent(), checkedTrackColor = VibrantTheme.accent(), uncheckedThumbColor = VibrantTheme.textSecondary(), uncheckedTrackColor = VibrantTheme.surfaceHigh())) } }
}

@Composable fun ExpressiveSettingItem(title: String, subtitle: String?, icon: ImageVector, photoUrl: String? = null, photoResId: Int? = null, colors: Pair<Color, Color>, onClick: () -> Unit, shape: Shape = RoundedCornerShape(24.dp), trailingContent: @Composable (RowScope.() -> Unit)? = null) {
    Surface(onClick = onClick, shape = shape, color = VibrantTheme.cardBackground(), modifier = Modifier.fillMaxWidth()) { Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(colors.first), contentAlignment = Alignment.Center) { if (photoUrl != null) AsyncImage(model = photoUrl, contentDescription = "Profile", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop) else if (photoResId != null) Image(painterResource(photoResId), "Profile", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop) else Icon(imageVector = icon, contentDescription = null, tint = colors.second, modifier = Modifier.size(28.dp)) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = VibrantTheme.textPrimary()); if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = VibrantTheme.textSecondary()) }; if (trailingContent != null) Row(content = trailingContent) } }
}

@Composable fun ExpressiveSettingsGroup(content: @Composable () -> Unit) { Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.Transparent) ) { content() } }

@Composable fun PixelSearchBar(placeholder: String, modifier: Modifier = Modifier) { Surface(modifier = modifier.height(64.dp), shape = CircleShape, color = VibrantTheme.surfaceHigh(), tonalElevation = 2.dp) { Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = VibrantTheme.textSecondary()); Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = VibrantTheme.textSecondary()) } } }

@Composable fun CollapsibleTopBar(title: String, collapse: Float, h: Dp, profilePhotoUrl: String? = null, onSettingsClick: () -> Unit = {}) {
    Surface(modifier = Modifier.fillMaxWidth().height(h), color = VibrantTheme.background(), tonalElevation = if (collapse > 0.9f) 2.dp else 0.dp) { Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) { val size = (32 - (12 * collapse)).sp; val hPad = (24 - (8 * collapse)).dp; val bPad = (16 * (1f - collapse)).dp; Text(title, fontSize = size, fontWeight = if (collapse > 0.5f) FontWeight.Medium else FontWeight.Bold, fontFamily = GoogleSansFlex, color = VibrantTheme.textPrimary(), modifier = Modifier.align(if (collapse > 0.5f) Alignment.Center else Alignment.BottomStart).padding(start = hPad, bottom = bPad)); Box(modifier = Modifier.align(if (collapse > 0.5f) Alignment.CenterEnd else Alignment.BottomEnd).padding(end = 24.dp, bottom = if (collapse > 0.5f) 0.dp else bPad).size(48.dp).clip(CircleShape).clickable(onClick = onSettingsClick), contentAlignment = Alignment.Center) { if (profilePhotoUrl != null) AsyncImage(model = profilePhotoUrl, contentDescription = "Profile", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop) else Surface(modifier = Modifier.fillMaxSize(), color = VibrantTheme.surfaceHigh(), shape = CircleShape) { Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = VibrantTheme.textPrimary()) } } } } }
}

@Composable fun MorphingIconButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, containerColor: Color = VibrantTheme.surfaceHigh(), contentColor: Color = VibrantTheme.textPrimary()) {
    val interactionSource = remember { MutableInteractionSource() }; val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(if (isPressed) 8.dp else 28.dp, if (isPressed) spring(stiffness = 100_000f) else spring(Spring.StiffnessMedium, Spring.DampingRatioHighBouncy), label = "CR")
    Surface(onClick = onClick, interactionSource = interactionSource, shape = RoundedCornerShape(cornerRadius), color = containerColor, contentColor = contentColor, modifier = modifier.size(56.dp)) { Box(contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp)) } }
}

@Composable fun CallTypeIcon(t: Int, modifier: Modifier = Modifier) {
    val (ic, c) = when (t) { CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived to Color(0xFF4CAF50); CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade to VibrantTheme.Blue.first; CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> Icons.AutoMirrored.Filled.CallMissed to Color(0xFFF44336); CallLog.Calls.BLOCKED_TYPE -> Icons.Default.Block to Color.Gray; else -> Icons.Default.Call to VibrantTheme.textSecondary() }
    Icon(imageVector = ic, contentDescription = null, tint = c, modifier = modifier)
}

@Composable fun CallHistoryDetailRow(r: CallRecord, d: Boolean) {
    val locale = LocalConfiguration.current.locales[0]; val timeSdf = remember(locale) { SimpleDateFormat("HH:mm", locale) }
    val ts = timeSdf.format(Date(r.date)); val ds = if (d && r.duration > 0) { val m = r.duration / 60; val s = r.duration % 60; if (m > 0) " • ${m}m ${s}s" else " • ${s}s" } else ""
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Row(verticalAlignment = Alignment.CenterVertically) { CallTypeIcon(r.type, Modifier.size(16.dp)); Spacer(modifier = Modifier.width(12.dp)); Text(text = "$ts$ds", style = MaterialTheme.typography.bodyMedium, color = VibrantTheme.textSecondary()) } }
}

@Composable fun DialPad(modifier: Modifier = Modifier, onKey: (String) -> Unit) {
    val keys = listOf(listOf("1", ""), listOf("2", "ABC"), listOf("3", "DEF"), listOf("4", "GHI"), listOf("5", "JKL"), listOf("6", "MNO"), listOf("7", "PQRS"), listOf("8", "TUV"), listOf("9", "WXYZ"), listOf("*", ""), listOf("0", "+"), listOf("#", ""))
    Column(modifier, verticalArrangement = Arrangement.Center) { for (i in 0 until 4) { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) { for (j in 0 until 3) { val k = keys[i * 3 + j]; Surface(onClick = { onKey(k[0]) }, modifier = Modifier.weight(1f).aspectRatio(1.2f), shape = RoundedCornerShape(24.dp), color = VibrantTheme.surfaceHigh()) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(k[0], style = MaterialTheme.typography.headlineMedium, color = VibrantTheme.textPrimary()); if (k[1].isNotEmpty()) Text(k[1], style = MaterialTheme.typography.labelSmall, color = VibrantTheme.textSecondary()) } } } } } }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable fun CategoryItemContent(onClick: () -> Unit, shape: Shape, compact: Boolean, photoUri: String?, colors: Pair<Color, Color>, icon: ImageVector, title: String, subtitle: String, trailingContent: @Composable (RowScope.() -> Unit)?, isExpanded: Boolean, expandedContent: @Composable (ColumnScope.() -> Unit)?, onCallClick: () -> Unit, onMessageClick: () -> Unit, onEditClick: () -> Unit, onDeleteClick: (Long) -> Unit, recordId: Long) {
    val mul = LocalAnimationMultiplier.current; val padding = if (compact) 12.dp else 16.dp; val iconSize = if (compact) 40.dp else 56.dp; val iconInnerSize = if (compact) 18.dp else 24.dp
    Surface(onClick = onClick, shape = shape, color = VibrantTheme.cardBackground(), modifier = Modifier.fillMaxWidth().animateContentSize(spring(stiffness = 400f / mul, dampingRatio = 0.85f))) { Column(modifier = Modifier.padding(padding)) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Box(modifier = Modifier.size(iconSize).clip(CircleShape).background(colors.first), contentAlignment = Alignment.Center) { if (photoUri != null) AsyncImage(model = photoUri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop) else Icon(imageVector = icon, contentDescription = null, tint = colors.second, modifier = Modifier.size(iconInnerSize)) }; Spacer(modifier = Modifier.width(if (compact) 12.dp else 16.dp)); Column(modifier = Modifier.weight(1f)) { Text(title, style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VibrantTheme.textPrimary(), maxLines = 1, overflow = TextOverflow.Ellipsis); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = VibrantTheme.textSecondary(), maxLines = 1, overflow = TextOverflow.Ellipsis) }; if (trailingContent != null) Row(content = trailingContent) }
    AnimatedVisibility(visible = isExpanded, enter = expandVertically(spring(stiffness = 400f / mul, dampingRatio = 0.85f)) + fadeIn(tween((250 * mul).toInt())), exit = shrinkVertically(spring(stiffness = 400f / mul, dampingRatio = 0.85f)) + fadeOut(tween((250 * mul).toInt()))) { Column { if (expandedContent != null) expandedContent(); Spacer(modifier = Modifier.height(16.dp)); val opts = listOf(Triple(Icons.Default.Call, onCallClick, "Call"), Triple(Icons.AutoMirrored.Default.Message, onMessageClick, "Message"), Triple(Icons.Default.Edit, onEditClick, "Edit"), Triple(Icons.Default.Delete, { onDeleteClick(recordId) }, "Delete")); val ints = remember { List(4) { MutableInteractionSource() } }; ButtonGroup(overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) }, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) { opts.forEachIndexed { idx, (ic, cl, l) -> customItem(buttonGroupContent = { ToggleButton(checked = false, onCheckedChange = { cl() }, modifier = Modifier.weight(1f).animateWidth(ints[idx]), shapes = when(idx) { 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes(); 3 -> ButtonGroupDefaults.connectedTrailingButtonShapes(); else -> ButtonGroupDefaults.connectedMiddleButtonShapes() }, colors = ToggleButtonDefaults.colors(containerColor = VibrantTheme.surfaceHigh(), contentColor = if (l == "Delete") VibrantTheme.Red else VibrantTheme.textPrimary()), interactionSource = ints[idx]) { Icon(imageVector = ic, contentDescription = null, modifier = Modifier.size(20.dp)) } }, menuContent = { DropdownMenuItem(text = { Text(l) }, onClick = cl, leadingIcon = { Icon(imageVector = ic, contentDescription = null) }) }) } } } } } }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable fun ExpressiveCategoryItem(title: String, subtitle: String, icon: ImageVector, photoUri: String?, colors: Pair<Color, Color>, isExpanded: Boolean, onClick: () -> Unit, onCallClick: () -> Unit, onMessageClick: () -> Unit, onEditClick: () -> Unit, onDeleteClick: (Long) -> Unit, recordId: Long, shape: Shape = RoundedCornerShape(24.dp), compact: Boolean = false, swipeEnabled: Boolean = true, trailingContent: @Composable (RowScope.() -> Unit)? = null, expandedContent: @Composable (ColumnScope.() -> Unit)? = null) {
    val ds = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> { onCallClick(); false }
                SwipeToDismissBoxValue.EndToStart -> { onMessageClick(); false }
                else -> false
            }
        }
    )
    if (!swipeEnabled) CategoryItemContent(onClick, shape, compact, photoUri, colors, icon, title, subtitle, trailingContent, isExpanded, expandedContent, onCallClick, onMessageClick, onEditClick, onDeleteClick, recordId)
    else SwipeToDismissBox(state = ds, backgroundContent = { val dir = ds.dismissDirection; val col = when (dir) { SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50); SwipeToDismissBoxValue.EndToStart -> VibrantTheme.accent(); else -> Color.Transparent }; val ali = when (dir) { SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart; SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd; else -> Alignment.Center }; val icd = when (dir) { SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Call; SwipeToDismissBoxValue.EndToStart -> Icons.AutoMirrored.Default.Message; else -> Icons.Default.Call }; Box(modifier = Modifier.fillMaxSize().clip(shape).background(col).padding(horizontal = 24.dp), contentAlignment = ali) { Icon(imageVector = icd, contentDescription = null, tint = Color.White) } }, modifier = Modifier.clip(shape)) { CategoryItemContent(onClick, shape, compact, photoUri, colors, icon, title, subtitle, trailingContent, isExpanded, expandedContent, onCallClick, onMessageClick, onEditClick, onDeleteClick, recordId) }
}

@Composable fun DesignGrid() {
    val d = LocalDensity.current
    Canvas(Modifier.fillMaxSize()) {
        val s = with(d) { 8.dp.toPx() }; val c = Color.Red.copy(alpha = 0.2f); val w = size.width; val h = size.height; var x = 0f
        while (x < w) { drawLine(c, Offset(x, 0f), Offset(x, h), 1f); x += s }; var y = 0f
        while (y < h) { drawLine(c, Offset(0f, y), Offset(w, y), 1f); y += s }
    }
}

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    val themeMode = MutableStateFlow(prefs.getString("theme_mode", "System") ?: "System")
    val fontScale = MutableStateFlow(prefs.getFloat("font_scale", 3f))
    val accentColorIndex = MutableStateFlow(prefs.getInt("accent_color_index", 0))
    val dynamicColor = MutableStateFlow(prefs.getBoolean("dynamic_color", false))
    val pureBlack = MutableStateFlow(prefs.getBoolean("pure_black", false))
    val highContrast = MutableStateFlow(prefs.getBoolean("high_contrast", false))
    val mergeDuplicates = MutableStateFlow(prefs.getBoolean("merge_duplicates", false))
    val showDuration = MutableStateFlow(prefs.getBoolean("show_duration", true))
    val showUnknown = MutableStateFlow(prefs.getBoolean("show_unknown", true))
    val historyLimit = MutableStateFlow(prefs.getInt("history_limit", 100))
    val compactView = MutableStateFlow(prefs.getBoolean("compact_view", false))
    val swipeActionsEnabled = MutableStateFlow(prefs.getBoolean("swipe_actions_enabled", true))
    val showFrequentHub = MutableStateFlow(prefs.getBoolean("show_frequent_hub", true))
    val strictGrouping = MutableStateFlow(prefs.getBoolean("strict_grouping", false))
    val showIncoming = MutableStateFlow(prefs.getBoolean("show_incoming", true))
    val showOutgoing = MutableStateFlow(prefs.getBoolean("show_outgoing", true))
    val showMissed = MutableStateFlow(prefs.getBoolean("show_missed", true))
    val showBlocked = MutableStateFlow(prefs.getBoolean("show_blocked", true))
    val favoriteNumbers = MutableStateFlow(prefs.getStringSet("favorite_numbers", emptySet()) ?: emptySet())
    val incomingAlerts = MutableStateFlow(prefs.getBoolean("incoming_alerts", true))
    val missedAlerts = MutableStateFlow(prefs.getBoolean("missed_alerts", true))
    val vibrationIntensity = MutableStateFlow(prefs.getInt("vibration_intensity", 1))
    val privacyMode = MutableStateFlow(prefs.getBoolean("privacy_mode", false))
    val showDesignGrid = MutableStateFlow(prefs.getBoolean("show_design_grid", false))
    val animationMultiplier = MutableStateFlow(prefs.getFloat("animation_multiplier", 1f))
    val isDemoMode = MutableStateFlow(prefs.getBoolean("is_demo_mode", false))
    val deletedCallIds = MutableStateFlow(prefs.getStringSet("deleted_call_ids", emptySet())?.map { it.toLong() }?.toSet() ?: emptySet())

    fun setThemeMode(m: String) { prefs.edit().putString("theme_mode", m).apply(); themeMode.value = m }
    fun setFontScale(s: Float) { prefs.edit().putFloat("font_scale", s).apply(); fontScale.value = s }
    fun setAccentColorIndex(i: Int) { prefs.edit().putInt("accent_color_index", i).apply(); accentColorIndex.value = i }
    fun setDynamicColor(e: Boolean) { prefs.edit().putBoolean("dynamic_color", e).apply(); dynamicColor.value = e }
    fun setPureBlack(e: Boolean) { prefs.edit().putBoolean("pure_black", e).apply(); pureBlack.value = e }
    fun setHighContrast(e: Boolean) { prefs.edit().putBoolean("high_contrast", e).apply(); highContrast.value = e }
    fun setMergeDuplicates(e: Boolean) { prefs.edit().putBoolean("merge_duplicates", e).apply(); mergeDuplicates.value = e }
    fun setShowDuration(e: Boolean) { prefs.edit().putBoolean("show_duration", e).apply(); showDuration.value = e }
    fun setShowUnknown(e: Boolean) { prefs.edit().putBoolean("show_unknown", e).apply(); showUnknown.value = e }
    fun setHistoryLimit(l: Int) { prefs.edit().putInt("history_limit", l).apply(); historyLimit.value = l }
    fun setCompactView(e: Boolean) { prefs.edit().putBoolean("compact_view", e).apply(); compactView.value = e }
    fun setSwipeActionsEnabled(e: Boolean) { prefs.edit().putBoolean("swipe_actions_enabled", e).apply(); swipeActionsEnabled.value = e }
    fun setShowFrequentHub(e: Boolean) { prefs.edit().putBoolean("show_frequent_hub", e).apply(); showFrequentHub.value = e }
    fun setStrictGrouping(e: Boolean) { prefs.edit().putBoolean("strict_grouping", e).apply(); strictGrouping.value = e }
    fun setShowIncoming(e: Boolean) { prefs.edit().putBoolean("show_incoming", e).apply(); showIncoming.value = e }
    fun setShowOutgoing(e: Boolean) { prefs.edit().putBoolean("show_outgoing", e).apply(); showOutgoing.value = e }
    fun setShowMissed(e: Boolean) { prefs.edit().putBoolean("show_missed", e).apply(); showMissed.value = e }
    fun setShowBlocked(e: Boolean) { prefs.edit().putBoolean("show_blocked", e).apply(); showBlocked.value = e }
    fun setFavoriteNumbers(n: Set<String>) { prefs.edit().putStringSet("favorite_numbers", n).apply(); favoriteNumbers.value = n }
    fun toggleFavoriteNumber(n: String) { val c = favoriteNumbers.value.toMutableSet(); if (c.contains(n)) c.remove(n) else c.add(n); setFavoriteNumbers(c) }
    fun setIncomingAlerts(e: Boolean) { prefs.edit().putBoolean("incoming_alerts", e).apply(); incomingAlerts.value = e }
    fun setMissedAlerts(e: Boolean) { prefs.edit().putBoolean("missed_alerts", e).apply(); missedAlerts.value = e }
    fun setVibrationIntensity(i: Int) { prefs.edit().putInt("vibration_intensity", i).apply(); vibrationIntensity.value = i }
    fun setPrivacyMode(e: Boolean) { prefs.edit().putBoolean("privacy_mode", e).apply(); privacyMode.value = e }
    fun setShowDesignGrid(e: Boolean) { prefs.edit().putBoolean("show_design_grid", e).apply(); showDesignGrid.value = e }
    fun setAnimationMultiplier(m: Float) { prefs.edit().putFloat("animation_multiplier", m).apply(); animationMultiplier.value = m }
    fun setIsDemoMode(e: Boolean) { prefs.edit().putBoolean("is_demo_mode", e).apply(); isDemoMode.value = e }
    fun deleteCall(id: Long) { val c = deletedCallIds.value.toMutableSet(); c.add(id); prefs.edit().putStringSet("deleted_call_ids", c.map { it.toString() }.toSet()).apply(); deletedCallIds.value = c }
    fun resetAllPreferences() { prefs.edit().clear().apply(); setThemeMode("System"); setFontScale(3f); setAccentColorIndex(0); setDynamicColor(false); setPureBlack(false); setHighContrast(false); setMergeDuplicates(false); setShowDuration(true); setShowUnknown(true); setHistoryLimit(100); setCompactView(false); setSwipeActionsEnabled(true); setShowFrequentHub(true); setStrictGrouping(false); setShowIncoming(true); setShowOutgoing(true); setShowMissed(true); setShowBlocked(true); setFavoriteNumbers(emptySet()); setIncomingAlerts(true); setMissedAlerts(true); setVibrationIntensity(1); setPrivacyMode(false); setShowDesignGrid(false); setAnimationMultiplier(1f); setIsDemoMode(false); deletedCallIds.value = emptySet() }
    fun saveSession(s: UserSession) { prefs.edit().putString("name", s.name).putString("email", s.email).putString("photoUrl", s.photoUrl).putString("phoneNumber", s.phoneNumber).apply() }
    fun getSession() = prefs.getString("name", null)?.let { UserSession(it, prefs.getString("email", "") ?: "", prefs.getString("photoUrl", null), prefs.getString("phoneNumber", null)) }
    fun clearSession() { prefs.edit().clear().apply() }
}

private fun performGoogleSignIn(context: Context, scope: CoroutineScope, onSuccess: (UserSession) -> Unit) {
    val cm = CredentialManager.create(context); val opt = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(WEB_CLIENT_ID).setAutoSelectEnabled(true).build(); val req = GetCredentialRequest.Builder().addCredentialOption(opt).build()
    scope.launch(Dispatchers.Main) { try { val res = cm.getCredential(context, req); val cred = res.credential; val s = if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { val t = GoogleIdTokenCredential.createFrom(cred.data); UserSession(t.displayName ?: "User", t.id, t.profilePictureUri?.toString(), getDevicePhoneNumber(context)) } else if (cred is GoogleIdTokenCredential) UserSession(cred.displayName ?: "User", cred.id, cred.profilePictureUri?.toString(), getDevicePhoneNumber(context)) else null; s?.let(onSuccess) } catch (e: Exception) { Log.e("Auth", "Fail", e) } }
}

fun getDevicePhoneNumber(context: Context): String? {
    return try { val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager; tm.line1Number?.ifBlank { null } } catch (e: SecurityException) { null }
}

@Composable fun rememberContactLookup(contacts: List<Contact>): (String) -> Contact? {
    return remember(contacts) {
        val map = mutableMapOf<String, Contact>()
        contacts.forEach { c -> val n = c.number.filter { it.isDigit() }.takeLast(10); if (n.isNotEmpty()) map[n] = c }
        val lookup: (String) -> Contact? = { addr -> val n = addr.filter { it.isDigit() }.takeLast(10); if (n.isEmpty()) contacts.find { it.number == addr } else map[n] ?: contacts.find { it.number == addr } }
        lookup
    }
}

fun getCallLog(context: Context): List<CallRecord> {
    val list = mutableListOf<CallRecord>()
    try { context.contentResolver.query(CallLog.Calls.CONTENT_URI, arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION, CallLog.Calls.CACHED_PHOTO_URI), null, null, "${CallLog.Calls.DATE} DESC")?.use { cursor ->
        val idIdx = cursor.getColumnIndex(CallLog.Calls._ID); val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER); val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME); val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE); val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE); val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION); val photoIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_PHOTO_URI)
        while (cursor.moveToNext()) { val photo = if (photoIdx != -1) cursor.getString(photoIdx) else null; list.add(CallRecord(cursor.getLong(idIdx), cursor.getString(nameIdx), cursor.getString(numIdx) ?: "Unknown", cursor.getInt(typeIdx), cursor.getLong(dateIdx), cursor.getLong(durIdx), photo)) }
    } } catch (e: SecurityException) { e.printStackTrace() }
    return list
}

fun getContacts(context: Context): List<Contact> {
    val map = mutableMapOf<String, Contact>()
    try { context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)?.use { cursor ->
        val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID); val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME); val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER); val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
        while (cursor.moveToNext()) { val name = cursor.getString(nameIdx) ?: "Unknown"; val num = cursor.getString(numIdx) ?: ""; val normalized = num.replace(" ", "").replace("-", ""); if (normalized.isNotEmpty() && !map.containsKey(normalized)) map[normalized] = Contact(cursor.getString(idIdx), name, normalized, cursor.getString(photoIdx)) }
    } } catch (e: SecurityException) { e.printStackTrace() }
    return map.values.toList().sortedBy { it.name }
}

fun getCallDateCategory(date: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = date }; val today = Calendar.getInstance()
    return when { c.get(Calendar.YEAR) == today.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"; c.get(Calendar.YEAR) == today.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> "Yesterday"; today.timeInMillis - date < 7 * 24 * 3600 * 1000L -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(date)); else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(date)) }
}

fun performVibration(context: Context, intensity: Int) {
    if (intensity == 0) return
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator else context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    vibrator.vibrate(VibrationEffect.createOneShot(if (intensity == 2) 100 else 50, if (intensity == 2) 255 else 128))
}

private fun placePhoneCall(context: Context, raw: String): Boolean {
    val num = raw.trim().replace(" ", "").replace("-", ""); if (num.isBlank()) return false
    val intent = if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) Intent(Intent.ACTION_CALL, Uri.parse("tel:$num")) else Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); return runCatching { context.startActivity(intent); true }.getOrElse { false }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable fun MainSettingsScreen(callRecords: List<CallRecord>, contactLookup: (String) -> Contact?, profilePhotoUrl: String?, onCallClick: (ActiveCall) -> Unit, onMessageClick: (Contact) -> Unit, onSettingsClick: () -> Unit, onEditFavoritesClick: () -> Unit, onDeleteCall: (Long) -> Unit, onRefresh: () -> Unit = {}) {
    val scope = rememberCoroutineScope(); val state = rememberLazyListState(); val d = LocalDensity.current; var refreshing by remember { mutableStateOf(false) }; val refreshState = rememberPullToRefreshState()
    val isDemo = LocalIsDemoMode.current
    val minH = 64.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding(); val maxH = 180.dp; val minHPx = with(d) { minH.toPx() }; val maxHPx = with(d) { maxH.toPx() }; val topBarH = remember { Animatable(maxHPx) }; var collapse by remember { mutableFloatStateOf(0f) }; var expandedId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(topBarH.value) { collapse = 1f - ((topBarH.value - minHPx) / (maxHPx - minHPx)).coerceIn(0f, 1f) }
    val nsc = remember { object : NestedScrollConnection { override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset { val delta = available.y; if (delta < 0 && (state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0)) return Offset.Zero; val prev = topBarH.value; val new = (prev + delta).coerceIn(minHPx, maxHPx); val consumed = new - prev; if (consumed.roundToInt() != 0) scope.launch { topBarH.snapTo(new) }; return if (!(delta < 0 && new == minHPx)) Offset(0f, consumed) else Offset.Zero } } }
    val showInc = LocalShowIncoming.current; val showOut = LocalShowOutgoing.current; val showMiss = LocalShowMissed.current; val showBlock = LocalShowBlocked.current; val showUnk = LocalShowUnknown.current; val limit = LocalHistoryLimit.current; val compact = LocalCompactView.current; val freq = LocalShowFrequentHub.current; val favs = LocalFavoriteNumbers.current; val strict = LocalStrictGrouping.current; val merge = LocalMergeDuplicates.current; val swipe = LocalSwipeActionsEnabled.current; val dur = LocalShowDuration.current
    
    val displayRecords = if (isDemo) {
        DemoDataProvider.contacts.mapIndexed { i, c ->
            CallRecord(i.toLong(), c.name, c.handle, CallLog.Calls.INCOMING_TYPE, System.currentTimeMillis() - (i * 3600000), 120, null)
        }
    } else {
        callRecords
    }
    
    val demoLookup: (String) -> Contact? = { handle ->
        DemoDataProvider.contacts.find { it.handle == handle }?.let {
            Contact(it.handle, it.name, it.handle, null)
        }
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                onRefresh()
                delay(1500)
                refreshing = false
            }
        },
        state = refreshState,
        indicator = {
            val progress = refreshState.distanceFraction
            val density = LocalDensity.current
            val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            
            // Dimensions similar to the picture (larger contained indicator)
            val indicatorSize = 64.dp
            val hideOffsetPx = with(density) { indicatorSize.toPx() } 
            // Positioned lower, clearly under the notification bar
            val targetOffsetPx = with(density) { (statusBarPadding + 80.dp).toPx() }

            if (progress > 0f || refreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Slide down from "thin air" (above the status bar)
                            translationY = (progress * targetOffsetPx) - hideOffsetPx
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (refreshing) {
                        ContainedLoadingIndicator(
                            modifier = Modifier.size(indicatorSize),
                            containerColor = VibrantTheme.accent(),
                            indicatorColor = VibrantTheme.onAccent()
                        )
                    } else {
                        ContainedLoadingIndicator(
                            progress = { progress },
                            modifier = Modifier.size(indicatorSize),
                            containerColor = VibrantTheme.accent(),
                            indicatorColor = VibrantTheme.onAccent()
                        )
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().nestedScroll(nsc)) {
            val h = with(d) { topBarH.value.toDp() }
            LazyColumn(state = state, contentPadding = PaddingValues(top = h + 8.dp, start = 16.dp, end = 16.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp), modifier = Modifier.fillMaxSize()) {
                val filtered = displayRecords.filter { r -> val tm = when (r.type) { CallLog.Calls.INCOMING_TYPE -> showInc; CallLog.Calls.OUTGOING_TYPE -> showOut; CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> showMiss; CallLog.Calls.BLOCKED_TYPE -> showBlock; else -> true }; tm && (showUnk || isDemo || contactLookup(r.number) != null) }.take(limit)
                if (freq) {
                    val hub = if (favs.isNotEmpty() && !isDemo) favs.map { n -> val c = contactLookup(n); ActiveCall(c?.name ?: n, n, c?.photoUri) } else if (isDemo) DemoDataProvider.contacts.take(5).map { ActiveCall(it.name, it.handle, null) } else callRecords.groupBy { it.number }.toList().sortedByDescending { it.second.size }.take(5).map { (n, rs) -> val c = contactLookup(n); ActiveCall(rs.first().name?.takeIf { it.isNotBlank() } ?: c?.name?.takeIf { it.isNotBlank() } ?: n, n, c?.photoUri) }
                    if (hub.isNotEmpty()) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(if (favs.isNotEmpty() && !isDemo) "FAVORITES" else "FREQUENT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = VibrantTheme.textSecondary(), modifier = Modifier.padding(start = 8.dp)); Text("Edit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = VibrantTheme.accent(), modifier = Modifier.padding(end = 8.dp).clickable(onClick = onEditFavoritesClick)) }
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { hub.forEach { c -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp).clickable { onCallClick(c) }) { Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(if (isDemo) Color(android.graphics.Color.parseColor(DemoDataProvider.contacts.find { it.handle == c.number }?.avatarColor ?: "#808080")) else VibrantTheme.surfaceHigh()), contentAlignment = Alignment.Center) { if (c.photoUri != null) AsyncImage(model = c.photoUri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop) else Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = if (isDemo) Color.White else VibrantTheme.textPrimary()) }; Text(c.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = VibrantTheme.textPrimary(), modifier = Modifier.padding(top = 4.dp)) } } }
                        }
                    }
                }
                val groups = if (strict) filtered.groupBy { it.number }.values.map { it.toMutableList() }.sortedByDescending { it.first().date } else filtered.fold(mutableListOf<MutableList<CallRecord>>()) { acc, r -> if (merge && acc.isNotEmpty() && acc.last().first().number == r.number) acc.last().add(r) else acc.add(mutableListOf(r)); acc }
                if (groups.isEmpty()) { item { Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text("No history.", color = VibrantTheme.textSecondary()) } } }
                else groups.groupBy { getCallDateCategory(it.first().date) }.forEach { (cat, gs) ->
                    item(key = "h_$cat") { Text(cat.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = VibrantTheme.textSecondary(), modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)) }
                    item(key = "g_$cat") { ExpressiveSettingsGroup { Column { for (i in gs.indices) { val mc = gs[i]; val r = mc.first(); val c = if (isDemo) demoLookup(r.number) else contactLookup(r.number); val dn = r.name?.takeIf { it.isNotBlank() } ?: c?.name?.takeIf { it.isNotBlank() } ?: r.number; val locale = LocalConfiguration.current.locales[0]; val ts = SimpleDateFormat("HH:mm", locale).format(Date(r.date)); val ds = if (dur && r.duration > 0) { val m = r.duration / 60; val s = r.duration % 60; if (m > 0) " • ${m}m ${s}s" else " • ${s}s" } else ""; val avatarColor = if (isDemo) Color(android.graphics.Color.parseColor(DemoDataProvider.contacts.find { it.handle == r.number }?.avatarColor ?: "#808080")) else VibrantTheme.IconColors[abs(dn.hashCode()) % VibrantTheme.IconColors.size].first; ExpressiveCategoryItem(dn, if (mc.size > 1 && expandedId != r.id) "${mc.size} calls • $ts" else "$ts$ds", Icons.Filled.Person, c?.photoUri, avatarColor to (if (isDemo) Color.White else VibrantTheme.IconColors[abs(dn.hashCode()) % VibrantTheme.IconColors.size].second), expandedId == r.id, { expandedId = if (expandedId == r.id) null else r.id }, { onCallClick(ActiveCall(dn, r.number, c?.photoUri)) }, { onMessageClick(c ?: Contact("", dn, r.number, null)) }, {}, onDeleteCall, r.id, shapeFor(i, gs.size), compact, swipe, { CallTypeIcon(r.type, Modifier.size(if (compact) 16.dp else 20.dp)) }, { if (mc.size > 1) { Spacer(Modifier.height(12.dp)); Text("History", style = MaterialTheme.typography.labelSmall, color = VibrantTheme.textSecondary(), modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)); for (record in mc) CallHistoryDetailRow(record, dur) } }) } } } }
                }
            }
            CollapsibleTopBar("Current", collapse, h, profilePhotoUrl, onSettingsClick)
        }
    }
}

@Composable fun SettingsScreen(u: UserSession?, onBack: () -> Unit, onProf: () -> Unit, onTheme: () -> Unit, onHist: () -> Unit, onAlrt: () -> Unit, onAbut: () -> Unit, onDev: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) { Row(modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VibrantTheme.textPrimary()) }; Text("Settings", style = MaterialTheme.typography.titleLarge, fontFamily = GoogleSansFlex, color = VibrantTheme.textPrimary(), modifier = Modifier.padding(start = 8.dp)) }; LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { item { PixelSearchBar("Search settings", Modifier.fillMaxWidth().padding(bottom = 12.dp)) }; item { ExpressiveSettingsGroup { ExpressiveSettingItem(u?.name ?: "Log In", u?.let { "${it.phoneNumber} • ${it.email}" } ?: "Backup and import contacts", Icons.Default.Person, u?.photoUrl, null, VibrantTheme.Blue, onProf, shapeFor(0, 1)) } }; item { ExpressiveSettingsGroup { Column { ExpressiveSettingItem("Theme & Accent", "Colors, themes, accessibility", Icons.Default.Palette, null, null, VibrantTheme.Cyan, onTheme, shapeFor(0, 2)); Spacer(modifier = Modifier.height(2.dp)); ExpressiveSettingItem("Call History & Display", "Interface, records, font size", Icons.Default.History, null, null, VibrantTheme.Indigo, onHist, shapeFor(1, 2)) } } }; item { ExpressiveSettingsGroup { ExpressiveSettingItem("Alerts", "Notifications, sounds", Icons.Default.Notifications, null, null, VibrantTheme.Pink, onAlrt, shapeFor(0, 1)) } }; item { ExpressiveSettingsGroup { Column { ExpressiveSettingItem("Developer Options", "Advanced configuration", Icons.Default.DataObject, null, null, VibrantTheme.SystemGrey, onDev, shapeFor(0, 2)); Spacer(modifier = Modifier.height(2.dp)); ExpressiveSettingItem("About", "Version, legal, status", Icons.Default.Info, null, null, VibrantTheme.SystemGrey, onAbut, shapeFor(1, 2)) } } } } }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable fun ThemeAccentScreen(
    onBack: () -> Unit,
    onTheme: (String) -> Unit,
    onFont: (Float) -> Unit,
    onAcc: (Int) -> Unit,
    onDyn: (Boolean) -> Unit,
    onPure: (Boolean) -> Unit,
    onHigh: (Boolean) -> Unit
) {
    val themeMode = LocalThemeMode.current
    val manualFontScale = LocalManualFontScale.current
    val accentIndex = LocalAccentColorIndex.current
    val isDynamic = LocalDynamicColor.current
    val isPureBlack = LocalPureBlack.current
    val isHighContrast = LocalHighContrast.current
    val options = listOf("System", "Light", "Dark")
    var selIdx by remember { mutableIntStateOf(options.indexOf(themeMode).coerceAtLeast(0)) }
    var font by remember { mutableFloatStateOf(manualFontScale) }
    val isLight = if (options[selIdx] == "System") !isSystemInDarkTheme() else options[selIdx] == "Light"

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = VibrantTheme.textPrimary()
                )
            }
            Text(
                "Theme & Accent",
                style = MaterialTheme.typography.titleLarge,
                color = VibrantTheme.textPrimary(),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(VibrantTheme.cardBackground()),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(VibrantTheme.accent()),
                            Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = VibrantTheme.onAccent(),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Preview",
                                style = MaterialTheme.typography.titleMedium,
                                color = VibrantTheme.textPrimary(),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Description",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VibrantTheme.textSecondary()
                            )
                        }
                    }
                }
            }

            item {
                val intS = remember { List(options.size) { MutableInteractionSource() } }
                ButtonGroup(
                    overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    options.forEachIndexed { i, t ->
                        val sel = (selIdx == i)
                        customItem(
                            buttonGroupContent = {
                                ToggleButton(
                                    checked = sel,
                                    onCheckedChange = {
                                        selIdx = i
                                        onTheme(options[i])
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .animateWidth(intS[i]),
                                    shapes = when (i) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                    colors = ToggleButtonDefaults.colors(
                                        checkedContainerColor = VibrantTheme.accent(),
                                        checkedContentColor = VibrantTheme.onAccent(),
                                        containerColor = VibrantTheme.surfaceHigh(),
                                        contentColor = VibrantTheme.textPrimary()
                                    ),
                                    interactionSource = intS[i]
                                ) {
                                    AnimatedVisibility(sel) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.size(8.dp))
                                        }
                                    }
                                    Text(
                                        t,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            },
                            menuContent = {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        if (sel) Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null
                                        )
                                    },
                                    text = { Text(t) },
                                    onClick = {
                                        selIdx = i
                                        onTheme(options[i])
                                    }
                                )
                            }
                        )
                    }
                }
            }

            item {
                val intS = remember { List(VibrantTheme.IconColors.size) { MutableInteractionSource() } }
                ButtonGroup(
                    overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isDynamic) 0.5f else 1f),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    VibrantTheme.IconColors.forEachIndexed { i, c ->
                        val sel = (accentIndex == i)
                        customItem(
                            buttonGroupContent = {
                                ToggleButton(
                                    checked = sel,
                                    onCheckedChange = { onAcc(i) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .animateWidth(intS[i]),
                                    enabled = !isDynamic,
                                    shapes = when (i) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        VibrantTheme.IconColors.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                    colors = ToggleButtonDefaults.colors(
                                        checkedContainerColor = c.first,
                                        checkedContentColor = c.second,
                                        containerColor = VibrantTheme.surfaceHigh(),
                                        contentColor = c.first.copy(0.6f),
                                        disabledContainerColor = VibrantTheme.surfaceHigh().copy(0.3f),
                                        disabledContentColor = Color.Gray
                                    ),
                                    interactionSource = intS[i]
                                ) {
                                    Box(
                                        Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isDynamic) Color.Gray else c.first)
                                            .padding(4.dp),
                                        Alignment.Center
                                    ) {
                                        if (sel && !isDynamic) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = c.second,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            menuContent = {
                                DropdownMenuItem(
                                    text = { Text("Color ${i + 1}") },
                                    onClick = { onAcc(i) },
                                    enabled = !isDynamic
                                )
                            }
                        )
                    }
                }
            }

            item {
                val isS = Build.VERSION.SDK_INT >= 31
                val total = if (isS) 3 else 2
                var idx = 0
                ExpressiveSettingsGroup {
                    Column {
                        if (isS) {
                            HubSwitchItem(
                                "Dynamic Color",
                                "Use system colors",
                                isDynamic,
                                onDyn,
                                Icons.Default.ColorLens,
                                shapeFor(idx++, total)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        HubSwitchItem(
                            "Pure Black",
                            "OLED dark mode",
                            isPureBlack,
                            onPure,
                            enabled = !isLight,
                            icon = Icons.Default.Brightness2,
                            shape = shapeFor(idx++, total)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        HubSwitchItem(
                            "High Contrast",
                            "Text legibility",
                            isHighContrast,
                            onHigh,
                            Icons.Default.Contrast,
                            shapeFor(idx++, total)
                        )
                    }
                }
            }

            item {
                ExpressiveSettingsGroup {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = VibrantTheme.cardBackground(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Small",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VibrantTheme.textSecondary()
                                )
                                Text(
                                    "Large",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VibrantTheme.textSecondary()
                                )
                            }
                            Slider(
                                value = font,
                                onValueChange = {
                                    font = it
                                    onFont(it)
                                },
                                valueRange = 1f..5f,
                                steps = 3,
                                colors = SliderDefaults.colors(
                                    thumbColor = VibrantTheme.accent(),
                                    activeTrackColor = VibrantTheme.accent(),
                                    inactiveTrackColor = VibrantTheme.surfaceHigh(),
                                    activeTickColor = VibrantTheme.onAccent(),
                                    inactiveTickColor = VibrantTheme.textSecondary().copy(0.5f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable fun CallHistoryDisplayScreen(
    onBack: () -> Unit,
    onMerge: (Boolean) -> Unit,
    onDur: (Boolean) -> Unit,
    onUnk: (Boolean) -> Unit,
    onLimit: (Int) -> Unit,
    onComp: (Boolean) -> Unit,
    onSwp: (Boolean) -> Unit,
    onFreq: (Boolean) -> Unit,
    onStrct: (Boolean) -> Unit,
    onInc: (Boolean) -> Unit,
    onOut: (Boolean) -> Unit,
    onMiss: (Boolean) -> Unit,
    onBlck: (Boolean) -> Unit,
    onPriv: (Boolean) -> Unit
) {
    val limits = listOf(50, 100, 250, 500)
    val mergeDuplicates = LocalMergeDuplicates.current
    val strictGrouping = LocalStrictGrouping.current
    val showUnknown = LocalShowUnknown.current
    val privacyMode = LocalPrivacyMode.current
    val compactView = LocalCompactView.current
    val showFrequentHub = LocalShowFrequentHub.current
    val showDuration = LocalShowDuration.current
    val swipeActionsEnabled = LocalSwipeActionsEnabled.current
    val showIncoming = LocalShowIncoming.current
    val showOutgoing = LocalShowOutgoing.current
    val showMissed = LocalShowMissed.current
    val showBlocked = LocalShowBlocked.current
    val historyLimit = LocalHistoryLimit.current

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = VibrantTheme.textPrimary()
                )
            }
            Text(
                "History & Display",
                style = MaterialTheme.typography.titleLarge,
                color = VibrantTheme.textPrimary(),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ExpressiveSettingsGroup {
                    Column {
                        HubSwitchItem(
                            "Merge Duplicates",
                            "Group consecutive",
                            mergeDuplicates,
                            onMerge,
                            Icons.AutoMirrored.Filled.MergeType,
                            shapeFor(0, 4)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        HubSwitchItem(
                            "Strict Grouping",
                            "All by contact",
                            strictGrouping,
                            onStrct,
                            Icons.Default.GroupWork,
                            shapeFor(1, 4)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        HubSwitchItem(
                            "Show Unknown",
                            "Include non-contacts",
                            showUnknown,
                            onUnk,
                            Icons.Default.QuestionMark,
                            shapeFor(2, 4)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        HubSwitchItem(
                            "Privacy Mode",
                            "Hide names in alerts",
                            privacyMode,
                            onPriv,
                            Icons.Default.Lock,
                            shapeFor(3, 4)
                        )
                    }
                }
            }

            item {
                ExpressiveSettingsGroup {
                    Column {
                        HubSwitchItem(
                            "Compact View",
                            "Small items",
                            compactView,
                            onComp,
                            Icons.Default.DensityMedium,
                            shapeFor(0, 3)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        HubSwitchItem(
                            "Frequent Hub",
                            "Show top contacts",
                            showFrequentHub,
                            onFreq,
                            Icons.Default.Star,
                            shapeFor(1, 3)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        HubSwitchItem(
                            "Call Duration",
                            "Show length",
                            showDuration,
                            onDur,
                            Icons.Default.Timer,
                            shapeFor(2, 3)
                        )
                    }
                }
            }

            item {
                ExpressiveSettingsGroup {
                    HubSwitchItem(
                        "Swipe Actions",
                        "Quick call/msg",
                        swipeActionsEnabled,
                        onSwp,
                        Icons.Default.Swipe,
                        shapeFor(0, 1)
                    )
                }
            }

            item {
                val opts = listOf(
                    Triple(showIncoming, onInc, Icons.AutoMirrored.Filled.CallReceived),
                    Triple(showOutgoing, onOut, Icons.AutoMirrored.Filled.CallMade),
                    Triple(showMissed, onMiss, Icons.AutoMirrored.Filled.CallMissed),
                    Triple(showBlocked, onBlck, Icons.Default.Block)
                )
                ButtonGroup(
                    overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    opts.forEachIndexed { i, (s, t, ic) ->
                        customItem(
                            buttonGroupContent = {
                                ToggleButton(
                                    checked = s,
                                    onCheckedChange = { t(it) },
                                    modifier = Modifier.weight(1f),
                                    shapes = when (i) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        opts.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                    colors = ToggleButtonDefaults.colors(
                                        checkedContainerColor = VibrantTheme.accent(),
                                        checkedContentColor = VibrantTheme.onAccent(),
                                        containerColor = VibrantTheme.surfaceHigh(),
                                        contentColor = VibrantTheme.textPrimary()
                                    )
                                ) {
                                    Icon(
                                        imageVector = ic,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            menuContent = {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        if (s) Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null
                                        )
                                    },
                                    text = {
                                        Text(
                                            when (i) {
                                                0 -> "Inc"
                                                1 -> "Out"
                                                2 -> "Miss"
                                                else -> "Blck"
                                            }
                                        )
                                    },
                                    onClick = { t(!s) }
                                )
                            }
                        )
                    }
                }
            }

            item {
                ButtonGroup(
                    overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    limits.forEachIndexed { i, l ->
                        val sel = (historyLimit == l)
                        customItem(
                            buttonGroupContent = {
                                ToggleButton(
                                    checked = sel,
                                    onCheckedChange = { onLimit(l) },
                                    modifier = Modifier.weight(1f),
                                    shapes = when (i) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        limits.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                    colors = ToggleButtonDefaults.colors(
                                        checkedContainerColor = VibrantTheme.accent(),
                                        checkedContentColor = VibrantTheme.onAccent(),
                                        containerColor = VibrantTheme.surfaceHigh(),
                                        contentColor = VibrantTheme.textPrimary()
                                    )
                                ) {
                                    Text(
                                        l.toString(),
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            },
                            menuContent = {
                                DropdownMenuItem(
                                    text = { Text(l.toString()) },
                                    onClick = { onLimit(l) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable fun EditFavoritesScreen(contacts: List<Contact>, onBackClick: () -> Unit, onToggle: (String) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) { Row(modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBackClick) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VibrantTheme.textPrimary()) }; Text("Edit Favorites", style = MaterialTheme.typography.titleLarge, color = VibrantTheme.textPrimary(), modifier = Modifier.padding(start = 8.dp)) }; LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(contacts.size) { i -> val c = contacts[i]; val sel = LocalFavoriteNumbers.current.contains(c.number); Surface(onClick = { onToggle(c.number) }, shape = shapeFor(i, contacts.size), color = VibrantTheme.cardBackground(), modifier = Modifier.fillMaxWidth()) { Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).clip(CircleShape).background(VibrantTheme.IconColors[abs(c.name.hashCode()) % VibrantTheme.IconColors.size].first), Alignment.Center) { if (c.photoUri != null) AsyncImage(model = c.photoUri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop) else Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = VibrantTheme.IconColors[abs(c.name.hashCode()) % VibrantTheme.IconColors.size].second) }; Spacer(modifier = Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text(c.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VibrantTheme.textPrimary()); Text(c.number, style = MaterialTheme.typography.bodySmall, color = VibrantTheme.textSecondary()) }; Checkbox(sel, { onToggle(c.number) }, colors = CheckboxDefaults.colors(VibrantTheme.accent(), VibrantTheme.onAccent())) } } } } }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable fun DeveloperOptionsScreen(
    onBackClick: () -> Unit,
    onRerun: () -> Unit,
    onReset: () -> Unit,
    onGrid: (Boolean) -> Unit,
    onAnim: (Float) -> Unit,
    onDemo: (Boolean) -> Unit
) {
    val speed = listOf(0.25f, 0.50f, 0.75f, 1f, 1.50f, 2.0f, 2.50f, 3.0f, 3.50f, 4.0f, 5f)
    val showDesignGrid = LocalShowDesignGrid.current
    val animationMultiplier = LocalAnimationMultiplier.current
    val isDemoMode = LocalIsDemoMode.current

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = VibrantTheme.textPrimary()
                )
            }
            Text(
                "Dev Options",
                style = MaterialTheme.typography.titleLarge,
                color = VibrantTheme.textPrimary(),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ExpressiveSettingsGroup {
                    Column {
                        HubSwitchItem(
                            "Demo Mode",
                            "Show mock celebrities",
                            isDemoMode,
                            onDemo,
                            Icons.Default.AutoFixHigh,
                            shapeFor(0, 1)
                        )
                    }
                }
            }

            item {
                ExpressiveSettingsGroup {
                    Column {
                        ExpressiveSettingItem(
                            "Model",
                            Build.MODEL,
                            Icons.Default.PhoneAndroid,
                            null,
                            null,
                            VibrantTheme.SystemGrey,
                            {},
                            shapeFor(0, 2)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        ExpressiveSettingItem(
                            "Android",
                            "${Build.VERSION.RELEASE}",
                            Icons.Default.Android,
                            null,
                            null,
                            VibrantTheme.SystemGrey,
                            {},
                            shapeFor(1, 2)
                        )
                    }
                }
            }

            item {
                ExpressiveSettingsGroup {
                    Column {
                        ExpressiveSettingItem(
                            "Rerun Setup",
                            "Restart config",
                            Icons.Default.AutoMode,
                            null,
                            null,
                            VibrantTheme.Magenta,
                            onRerun,
                            shapeFor(0, 2)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        ExpressiveSettingItem(
                            "Reset All",
                            "Clear data",
                            Icons.Default.DeleteForever,
                            null,
                            null,
                            Color.Red to Color.White,
                            onReset,
                            shapeFor(1, 2)
                        )
                    }
                }
            }

            item {
                ExpressiveSettingsGroup {
                    HubSwitchItem(
                        "Show Grid",
                        "8dp overlay",
                        showDesignGrid,
                        onGrid,
                        Icons.Default.GridOn,
                        shapeFor(0, 1)
                    )
                }
            }

            item {
                ExpressiveSettingsGroup {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = VibrantTheme.cardBackground(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(
                                    "Fast",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VibrantTheme.textSecondary()
                                )
                                Text(
                                    "Slow",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VibrantTheme.textSecondary()
                                )
                            }
                            Slider(
                                value = speed.indexOf(animationMultiplier).coerceAtLeast(0).toFloat(),
                                onValueChange = {
                                    onAnim(speed[it.roundToInt().coerceIn(0, speed.size - 1)])
                                },
                                valueRange = 0f..10f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    VibrantTheme.accent(),
                                    VibrantTheme.accent(),
                                    VibrantTheme.surfaceHigh()
                                )
                            )
                            Box(Modifier.fillMaxWidth(), Alignment.Center) {
                                Text(
                                    "${animationMultiplier}x",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VibrantTheme.textPrimary(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable fun AlertsScreen(
    onBackClick: () -> Unit,
    onInc: (Boolean) -> Unit,
    onMiss: (Boolean) -> Unit,
    onVib: (Int) -> Unit
) {
    val vibs = listOf("None", "Def", "Strong")
    val incomingAlerts = LocalIncomingAlerts.current
    val missedAlerts = LocalMissedAlerts.current
    val vibrationIntensity = LocalVibrationIntensity.current

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = VibrantTheme.textPrimary()
                )
            }
            Text(
                "Alerts",
                style = MaterialTheme.typography.titleLarge,
                color = VibrantTheme.textPrimary(),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(VibrantTheme.cardBackground()),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val ctx = LocalContext.current
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Test Vib",
                            style = MaterialTheme.typography.titleMedium,
                            color = VibrantTheme.textPrimary(),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        MorphingIconButton(
                            Icons.Default.PlayArrow,
                            {
                                performVibration(ctx, vibrationIntensity)
                                Toast.makeText(ctx, "Testing...", Toast.LENGTH_SHORT).show()
                            },
                            containerColor = VibrantTheme.accent(),
                            contentColor = VibrantTheme.onAccent()
                        )
                    }
                }
            }

            item {
                ExpressiveSettingsGroup {
                    Column {
                        HubSwitchItem(
                            "Incoming Call",
                            "Popups/sound",
                            incomingAlerts,
                            onInc,
                            Icons.AutoMirrored.Filled.VolumeUp,
                            shapeFor(0, 2)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        HubSwitchItem(
                            "Missed Call",
                            "Alerts",
                            missedAlerts,
                            onMiss,
                            Icons.AutoMirrored.Filled.PhoneMissed,
                            shapeFor(1, 2)
                        )
                    }
                }
            }

            item {
                ExpressiveSettingsGroup {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(VibrantTheme.cardBackground())
                            .padding(20.dp)
                    ) {
                        Text(
                            "Vibration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VibrantTheme.textPrimary()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val ctx = LocalContext.current
                        ButtonGroup(
                            overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                        ) {
                            vibs.forEachIndexed { i, l ->
                                val sel = (vibrationIntensity == i)
                                customItem(
                                    buttonGroupContent = {
                                        ToggleButton(
                                            checked = sel,
                                            onCheckedChange = {
                                                onVib(i)
                                                performVibration(ctx, i)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shapes = when (i) {
                                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                                2 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                            },
                                            colors = ToggleButtonDefaults.colors(
                                                checkedContainerColor = VibrantTheme.accent(),
                                                checkedContentColor = VibrantTheme.onAccent(),
                                                containerColor = VibrantTheme.surfaceHigh(),
                                                contentColor = VibrantTheme.textPrimary()
                                            )
                                        ) {
                                            Text(l, style = MaterialTheme.typography.labelLarge)
                                        }
                                    },
                                    menuContent = {
                                        DropdownMenuItem(text = { Text(l) }, onClick = { onVib(i) })
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable fun AccountScreen(u: UserSession, onBack: () -> Unit, onImport: () -> Unit, onBackup: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) { Row(modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VibrantTheme.textPrimary()) }; Text("Account", style = MaterialTheme.typography.titleLarge, color = VibrantTheme.textPrimary(), modifier = Modifier.padding(start = 8.dp)) }; LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { item { Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(VibrantTheme.surfaceHigh()), contentAlignment = Alignment.Center) { if (u.photoUrl != null) AsyncImage(model = u.photoUrl, contentDescription = "Profile", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop) else Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = VibrantTheme.textPrimary(), modifier = Modifier.size(60.dp)) }; Spacer(modifier = Modifier.height(16.dp)); Text(u.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = VibrantTheme.textPrimary()); Text(u.email, style = MaterialTheme.typography.bodyLarge, color = VibrantTheme.textSecondary()) } }; item { ExpressiveSettingsGroup { Column { ExpressiveSettingItem("Import", "From Google Contacts", Icons.Default.ImportContacts, null, null, VibrantTheme.Blue, onImport, shapeFor(0, 2)); Spacer(modifier = Modifier.height(2.dp)); ExpressiveSettingItem("Backup", "Sync to Cloud", Icons.Default.CloudUpload, null, null, VibrantTheme.Cyan, onBackup, shapeFor(1, 2)) } } }; item { ExpressiveSettingsGroup { ExpressiveSettingItem("Log Out", "Clear session", Icons.AutoMirrored.Filled.Logout, null, null, Color.Red to Color.White, onLogout, shapeFor(0, 1)) } } } }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable fun AboutScreen(onBackClick: () -> Unit) {
    val ctx = LocalContext.current
    val dark = isSystemInDarkThemeCustom()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = VibrantTheme.textPrimary()
                )
            }
            Text(
                "About",
                style = MaterialTheme.typography.titleLarge,
                color = VibrantTheme.textPrimary(),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(if (dark) R.drawable.current_logo_dark else R.drawable.current_logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(32.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Current",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = VibrantTheme.textPrimary()
                    )
                    Text(
                        "Beta 3.0",
                        style = MaterialTheme.typography.bodyLarge,
                        color = VibrantTheme.accent(),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item {
                ExpressiveSettingsGroup {
                    Column {
                        ExpressiveSettingItem(
                            "Model",
                            Build.MODEL,
                            Icons.Default.PhoneAndroid,
                            null,
                            null,
                            VibrantTheme.SystemGrey,
                            {},
                            shapeFor(0, 2)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        ExpressiveSettingItem(
                            "Android",
                            "${Build.VERSION.RELEASE}",
                            Icons.Default.Android,
                            null,
                            null,
                            VibrantTheme.SystemGrey,
                            {},
                            shapeFor(1, 2)
                        )
                    }
                }
            }

            item {
                ExpressiveSettingsGroup {
                    ExpressiveSettingItem(
                        "Andrei Popescu",
                        "Lead Developer",
                        Icons.Default.Code,
                        "https://github.com/Andrei2012GT9.png",
                        null,
                        VibrantTheme.Blue,
                        {},
                        shapeFor(0, 1),
                        trailingContent = {
                            IconButton(onClick = {
                                ctx.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/Andrei2012GT9")
                                    )
                                )
                            }) {
                                Image(
                                    painter = painterResource(if (dark) R.drawable.github_logo_white else R.drawable.github_invertocat_black),
                                    contentDescription = "GitHub",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            ctx.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/Andrei2012GT9/Current")
                                )
                            )
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .width(120.dp)
                    ) {
                        Image(
                            painter = painterResource(if (dark) R.drawable.github_logo_darkmode else R.drawable.github_logo_lightmode),
                            contentDescription = "Repo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable fun InCallScreen(onEndCall: () -> Unit, onAcceptCall: () -> Unit, onMuteClick: () -> Unit, onSpeakerClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    var keypad by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    val name = CallManager.lastName.ifBlank { CallManager.lastNumber }
    val time = remember(CallManager.durationSeconds) {
        String.format(Locale.getDefault(), "%02d:%02d", CallManager.durationSeconds / 60, CallManager.durationSeconds % 60)
    }
    val status = when (CallManager.callState) {
        Call.STATE_RINGING -> "Incoming"
        Call.STATE_DIALING -> "Calling..."
        Call.STATE_CONNECTING -> "Connecting..."
        Call.STATE_ACTIVE -> "Active"
        Call.STATE_HOLDING -> "On Hold"
        Call.STATE_DISCONNECTING -> "Ending..."
        else -> "Call"
    }
    val isRinging = (CallManager.callState == Call.STATE_RINGING)
    val isMuted = CallManager.isMuted
    val audioRoute = CallManager.audioRoute
    val multiplier = LocalAnimationMultiplier.current

    val profileShape = rememberRandomPolygonShape()
    val acceptShape = rememberRandomPolygonShape()
    val declineShape = rememberRandomPolygonShape()

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        if (isRinging) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Card: Info and Messaging
                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = VibrantTheme.cardBackground())
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            status,
                            style = MaterialTheme.typography.titleMedium,
                            color = VibrantTheme.accent(),
                            fontWeight = FontWeight.SemiBold
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(profileShape)
                                    .background(VibrantTheme.surfaceHigh()),
                                contentAlignment = Alignment.Center
                            ) {
                                if (CallManager.lastPhotoUri != null) {
                                    AsyncImage(
                                        model = CallManager.lastPhotoUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = VibrantTheme.textPrimary(),
                                        modifier = Modifier.size(80.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Surface(
                                shape = ClamshellShape,
                                color = VibrantTheme.surfaceHigh(),
                                modifier = Modifier.wrapContentSize()
                            ) {
                                Text(
                                    name,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = VibrantTheme.textPrimary(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Split Button for Message
                        var expanded by rememberSaveable { mutableStateOf(false) }
                        val context = LocalContext.current
                        Box(modifier = Modifier.wrapContentSize()) {
                            SplitButtonLayout(
                                leadingButton = {
                                    SplitButtonDefaults.TonalLeadingButton(
                                        onClick = { Toast.makeText(context, "Messaging...", Toast.LENGTH_SHORT).show() }
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Default.Message,
                                            modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                                            contentDescription = "Message",
                                        )
                                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                        Text("Message")
                                    }
                                },
                                trailingButton = {
                                    SplitButtonDefaults.TonalTrailingButton(
                                        checked = expanded,
                                        onCheckedChange = { expanded = it }
                                    ) {
                                        val rotation: Float by animateFloatAsState(
                                            targetValue = if (expanded) 180f else 0f,
                                            label = "Trailing Icon Rotation",
                                        )
                                        Icon(
                                            Icons.Filled.KeyboardArrowDown,
                                            modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize).graphicsLayer {
                                                this.rotationZ = rotation
                                            },
                                            contentDescription = "Options",
                                        )
                                    }
                                }
                            )

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Ignore") },
                                    onClick = { expanded = false; onEndCall() },
                                    leadingIcon = { Icon(Icons.Default.NotificationsOff, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Block") },
                                    onClick = { expanded = false; Toast.makeText(context, "Blocked", Toast.LENGTH_SHORT).show() },
                                    leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) }
                                )
                            }
                        }
                    }
                }

                // Bottom Card: Call Controls
                Card(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = VibrantTheme.cardBackground())
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "pulseAlpha"
                    )

                    Row(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decline
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                onClick = onEndCall,
                                shape = declineShape,
                                color = VibrantTheme.Red.copy(alpha = 0.2f),
                                contentColor = VibrantTheme.Red,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CallEnd, "Decline", modifier = Modifier.size(36.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Decline", style = MaterialTheme.typography.labelLarge, color = VibrantTheme.textSecondary())
                        }

                        // Answer
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .graphicsLayer {
                                            scaleX = pulseScale
                                            scaleY = pulseScale
                                            alpha = pulseAlpha
                                        }
                                        .background(Color(0xFF4CAF50), acceptShape)
                                )
                                Surface(
                                    onClick = onAcceptCall,
                                    shape = acceptShape,
                                    color = Color(0xFF4CAF50),
                                    contentColor = Color.White,
                                    modifier = Modifier.size(80.dp),
                                    shadowElevation = 8.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Call, "Accept", modifier = Modifier.size(42.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Accept", style = MaterialTheme.typography.labelLarge, color = VibrantTheme.textPrimary(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Active Call UI (Existing)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = keypad,
                        transitionSpec = {
                            fadeIn(tween((250 * multiplier).toInt())) togetherWith
                                    fadeOut(tween((250 * multiplier).toInt()))
                        },
                        label = "InCallKeypad"
                    ) { isKeypadOpen ->
                        if (isKeypadOpen) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(VibrantTheme.surfaceHigh()),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (CallManager.lastPhotoUri != null) {
                                            AsyncImage(
                                                model = CallManager.lastPhotoUri,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = VibrantTheme.textPrimary(),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = VibrantTheme.textPrimary(),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    "$status • $time",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VibrantTheme.textSecondary(),
                                    textAlign = TextAlign.End
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 48.dp)
                            ) {
                                Text(
                                    time,
                                    style = MaterialTheme.typography.displayMedium,
                                    color = VibrantTheme.textPrimary(),
                                    fontFamily = GoogleSansFlex
                                )
                                Text(
                                    status,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VibrantTheme.accent(),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(48.dp))
                                Box(
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(CircleShape)
                                        .background(VibrantTheme.surfaceHigh()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (CallManager.lastPhotoUri != null) {
                                        AsyncImage(
                                            model = CallManager.lastPhotoUri,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = VibrantTheme.textPrimary(),
                                            modifier = Modifier.size(80.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = VibrantTheme.textPrimary(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = keypad,
                    enter = expandVertically(
                        spring(
                            stiffness = 400f / LocalAnimationMultiplier.current,
                            dampingRatio = 0.85f
                        )
                    ) + fadeIn(),
                    exit = shrinkVertically(
                        spring(
                            stiffness = 400f / LocalAnimationMultiplier.current,
                            dampingRatio = 0.85f
                        )
                    ) + fadeOut()
                ) {
                    DialPad(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    ) { d ->
                        CallManager.currentCall?.playDtmfTone(d[0])
                        scope.launch {
                            delay(200)
                            CallManager.currentCall?.stopDtmfTone()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ExpressiveSettingsGroup {
                        Column {
                            val intS1 = remember { List(3) { MutableInteractionSource() } }
                            val g1 = listOf(
                                Triple(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, onMuteClick, "Mute"),
                                Triple(
                                    if (audioRoute == CallAudioState.ROUTE_SPEAKER) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                                    onSpeakerClick,
                                    "Speaker"
                                ),
                                Triple(Icons.Default.Dialpad, { keypad = !keypad }, "Keypad")
                            )
                            ButtonGroup(
                                overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                            ) {
                                g1.forEachIndexed { i, (ic, cl, l) ->
                                    val sel = (l == "Keypad" && keypad) || (l == "Mute" && isMuted) || (l == "Speaker" && audioRoute == CallAudioState.ROUTE_SPEAKER)
                                    customItem(
                                        buttonGroupContent = {
                                            ToggleButton(
                                                checked = sel,
                                                onCheckedChange = { cl() },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .animateWidth(intS1[i]),
                                                shapes = when (i) {
                                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                                    2 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                                },
                                                colors = ToggleButtonDefaults.colors(
                                                    checkedContainerColor = VibrantTheme.accent(),
                                                    checkedContentColor = VibrantTheme.onAccent(),
                                                    containerColor = VibrantTheme.surfaceHigh(),
                                                    contentColor = VibrantTheme.textPrimary()
                                                ),
                                                interactionSource = intS1[i]
                                            ) {
                                                Icon(
                                                    imageVector = ic,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        },
                                        menuContent = {
                                            DropdownMenuItem(text = { Text(l) }, onClick = cl)
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            val hold = (CallManager.callState == Call.STATE_HOLDING)
                            val intS2 = remember { List(2) { MutableInteractionSource() } }
                            ButtonGroup(
                                overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                            ) {
                                customItem(
                                    buttonGroupContent = {
                                        ToggleButton(
                                            checked = hold,
                                            onCheckedChange = { if (hold) CallManager.currentCall?.unhold() else CallManager.currentCall?.hold() },
                                            modifier = Modifier
                                                .weight(1f)
                                                .animateWidth(intS2[0]),
                                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                                            colors = ToggleButtonDefaults.colors(
                                                checkedContainerColor = VibrantTheme.accent(),
                                                checkedContentColor = VibrantTheme.onAccent(),
                                                containerColor = VibrantTheme.surfaceHigh(),
                                                contentColor = VibrantTheme.textPrimary()
                                            ),
                                            interactionSource = intS2[0]
                                        ) {
                                            Icon(
                                                imageVector = if (hold) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    },
                                    menuContent = {
                                        DropdownMenuItem(
                                            text = { Text("Hold") },
                                            onClick = { if (hold) CallManager.currentCall?.unhold() else CallManager.currentCall?.hold() }
                                        )
                                    }
                                )
                                customItem(
                                    buttonGroupContent = {
                                        ToggleButton(
                                            checked = recording,
                                            onCheckedChange = { recording = !recording },
                                            modifier = Modifier
                                                .weight(1f)
                                                .animateWidth(intS2[1]),
                                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                            colors = ToggleButtonDefaults.colors(
                                                checkedContainerColor = VibrantTheme.accent(),
                                                checkedContentColor = VibrantTheme.onAccent(),
                                                containerColor = VibrantTheme.surfaceHigh(),
                                                contentColor = VibrantTheme.textPrimary()
                                            ),
                                            interactionSource = intS2[1]
                                        ) {
                                            Icon(
                                                imageVector = if (recording) Icons.Default.FiberManualRecord else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    },
                                    menuContent = {
                                        DropdownMenuItem(
                                            text = { Text("Record") },
                                            onClick = { recording = !recording }
                                        )
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    FloatingActionButton(
                        onClick = onEndCall,
                        containerColor = VibrantTheme.Red,
                        contentColor = VibrantTheme.RedOn,
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { setShowWhenLocked(true); setTurnScreenOn(true) }
        val sessionManager = SessionManager(this)
        setContent {
            val themeMode by sessionManager.themeMode.collectAsState()
            val manualFontScale by sessionManager.fontScale.collectAsState()
            val accentColorIndex by sessionManager.accentColorIndex.collectAsState()
            val dynamicColor by sessionManager.dynamicColor.collectAsState()
            val pureBlack by sessionManager.pureBlack.collectAsState()
            val highContrast by sessionManager.highContrast.collectAsState()
            val mergeDuplicates by sessionManager.mergeDuplicates.collectAsState()
            val showDuration by sessionManager.showDuration.collectAsState()
            val showUnknown by sessionManager.showUnknown.collectAsState()
            val historyLimit by sessionManager.historyLimit.collectAsState()
            val compactView by sessionManager.compactView.collectAsState()
            val swipeActionsEnabled by sessionManager.swipeActionsEnabled.collectAsState()
            val showFrequentHub by sessionManager.showFrequentHub.collectAsState()
            val strictGrouping by sessionManager.strictGrouping.collectAsState()
            val showIncoming by sessionManager.showIncoming.collectAsState()
            val showOutgoing by sessionManager.showOutgoing.collectAsState()
            val showMissed by sessionManager.showMissed.collectAsState()
            val showBlocked by sessionManager.showBlocked.collectAsState()
            val favoriteNumbers by sessionManager.favoriteNumbers.collectAsState()
            val incomingAlerts by sessionManager.incomingAlerts.collectAsState()
            val missedAlerts by sessionManager.missedAlerts.collectAsState()
            val vibrationIntensity by sessionManager.vibrationIntensity.collectAsState()
            val privacyMode by sessionManager.privacyMode.collectAsState()
            val showDesignGrid by sessionManager.showDesignGrid.collectAsState()
            val animationMultiplier by sessionManager.animationMultiplier.collectAsState()
            val isDemoMode by sessionManager.isDemoMode.collectAsState()
            val deletedCallIds by sessionManager.deletedCallIds.collectAsState()
            val isDark = when(themeMode) { "Dark" -> true; "Light" -> false; else -> isSystemInDarkTheme() }

            CurrentTheme(darkTheme = isDark, dynamicColor = dynamicColor) {
                val context = LocalContext.current; val density = LocalDensity.current
                val combinedDensity = remember(density, manualFontScale) { Density(density.density, density.fontScale * (manualFontScale / 3f)) }
                CompositionLocalProvider(
                    LocalDensity provides combinedDensity, LocalThemeMode provides themeMode, LocalManualFontScale provides manualFontScale, LocalAccentColorIndex provides accentColorIndex, LocalDynamicColor provides dynamicColor, LocalPureBlack provides pureBlack, LocalHighContrast provides highContrast, LocalMergeDuplicates provides mergeDuplicates, LocalShowDuration provides showDuration, LocalShowUnknown provides showUnknown, LocalHistoryLimit provides historyLimit, LocalCompactView provides compactView, LocalSwipeActionsEnabled provides swipeActionsEnabled, LocalShowFrequentHub provides showFrequentHub, LocalStrictGrouping provides strictGrouping, LocalShowIncoming provides showIncoming, LocalShowOutgoing provides showOutgoing, LocalShowMissed provides showMissed, LocalShowBlocked provides showBlocked, LocalFavoriteNumbers provides favoriteNumbers, LocalIncomingAlerts provides incomingAlerts, LocalMissedAlerts provides missedAlerts, LocalVibrationIntensity provides vibrationIntensity, LocalPrivacyMode provides privacyMode, LocalShowDesignGrid provides showDesignGrid, LocalAnimationMultiplier provides animationMultiplier, LocalIsDemoMode provides isDemoMode, LocalDeletedCallIds provides deletedCallIds
                ) {
                    val scope = rememberCoroutineScope()
                    var hasPermissions by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) }
                    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms -> hasPermissions = perms.values.all { it } }
                    LaunchedEffect(Unit) { if (!hasPermissions) permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE)) }
                    if (hasPermissions) {
                        var callLogVersion by remember { mutableIntStateOf(0) }
                        val callLogRecords = remember(callLogVersion) { getCallLog(context) }
                        val filteredSystemCalls = remember(callLogRecords, deletedCallIds) { callLogRecords.filter { !deletedCallIds.contains(it.id) } }
                        val contacts = remember { getContacts(context) }
                        val contactLookup = rememberContactLookup(contacts)
                        var userSession by remember { mutableStateOf(sessionManager.getSession()) }
                        var currentScreen by remember { mutableStateOf("main") }
                        BackHandler(enabled = currentScreen != "main") { currentScreen = when (currentScreen) { "account" -> "settings"; "alerts" -> "settings"; "developer_options" -> "settings"; "theme_accent" -> "settings"; "call_history_display" -> "settings"; "about" -> "settings"; "edit_favorites" -> "main"; else -> "main" } }
                        Box(modifier = Modifier.fillMaxSize().background(VibrantTheme.background())) {
                            val activeCall = CallManager.currentCall
                            val callNumber = remember(activeCall) { activeCall?.details?.handle?.schemeSpecificPart ?: "" }
                            val activeContact = remember(callNumber, contacts) { contactLookup(callNumber) }
                            LaunchedEffect(activeContact, callNumber) { if (activeCall != null) { CallManager.lastName = activeContact?.name ?: callNumber; CallManager.lastPhotoUri = activeContact?.photoUri } }
                            if (activeCall != null && CallManager.callState != CallManager.STATE_IDLE && CallManager.callState != Call.STATE_DISCONNECTED) {
                                InCallScreen(onEndCall = { CallService.endCall() }, onAcceptCall = { CallService.acceptCall() }, onMuteClick = { CallService.setMuted(!CallManager.isMuted) }, onSpeakerClick = { val nr = if (CallManager.audioRoute == CallAudioState.ROUTE_SPEAKER) CallAudioState.ROUTE_EARPIECE else CallAudioState.ROUTE_SPEAKER; CallService.setAudioRoute(nr) })
                            } else {
                                when (currentScreen) {
                                    "main" -> MainSettingsScreen(filteredSystemCalls, contactLookup, userSession?.photoUrl, { placePhoneCall(context, it.number) }, { Toast.makeText(context, "Messaging ${it.name}", Toast.LENGTH_SHORT).show() }, { currentScreen = "settings" }, { currentScreen = "edit_favorites" }, { sessionManager.deleteCall(it) }, { callLogVersion++ })
                                    "settings" -> SettingsScreen(userSession, { currentScreen = "main" }, { if (userSession == null) performGoogleSignIn(context, scope) { userSession = it; sessionManager.saveSession(it) } else currentScreen = "account" }, { currentScreen = "theme_accent" }, { currentScreen = "call_history_display" }, { currentScreen = "alerts" }, { currentScreen = "about" }, { currentScreen = "developer_options" })
                                    "theme_accent" -> ThemeAccentScreen({ currentScreen = "settings" }, { sessionManager.setThemeMode(it) }, { sessionManager.setFontScale(it) }, { sessionManager.setAccentColorIndex(it) }, { sessionManager.setDynamicColor(it) }, { sessionManager.setPureBlack(it) }, { sessionManager.setHighContrast(it) })
                                    "call_history_display" -> CallHistoryDisplayScreen({ currentScreen = "settings" }, { sessionManager.setMergeDuplicates(it) }, { sessionManager.setShowDuration(it) }, { sessionManager.setShowUnknown(it) }, { sessionManager.setHistoryLimit(it) }, { sessionManager.setCompactView(it) }, { sessionManager.setSwipeActionsEnabled(it) }, { sessionManager.setShowFrequentHub(it) }, { sessionManager.setStrictGrouping(it) }, { sessionManager.setShowIncoming(it) }, { sessionManager.setShowOutgoing(it) }, { sessionManager.setShowMissed(it) }, { sessionManager.setShowBlocked(it) }, { sessionManager.setPrivacyMode(it) })
                                    "edit_favorites" -> EditFavoritesScreen(contacts, { currentScreen = "main" }, { sessionManager.toggleFavoriteNumber(it) })
                                    "account" -> AccountScreen(userSession!!, { currentScreen = "settings" }, { Toast.makeText(context, "Importing...", Toast.LENGTH_SHORT).show() }, { Toast.makeText(context, "Backing up...", Toast.LENGTH_SHORT).show() }, { userSession = null; sessionManager.clearSession(); currentScreen = "settings" })
                                    "developer_options" -> DeveloperOptionsScreen({ currentScreen = "settings" }, { Toast.makeText(context, "Rerunning...", Toast.LENGTH_SHORT).show() }, { sessionManager.resetAllPreferences() }, { sessionManager.setShowDesignGrid(it) }, { sessionManager.setAnimationMultiplier(it) }, { sessionManager.setIsDemoMode(it) })
                                    "alerts" -> AlertsScreen({ currentScreen = "settings" }, { sessionManager.setIncomingAlerts(it) }, { sessionManager.setMissedAlerts(it) }, { sessionManager.setVibrationIntensity(it) })
                                    "about" -> AboutScreen { currentScreen = "settings" }
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(VibrantTheme.background()), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Permissions required", style = MaterialTheme.typography.titleMedium, color = VibrantTheme.textPrimary()); Spacer(modifier = Modifier.height(16.dp)); Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE)) }) { Text("Grant Permissions") } } }
                    }
                    if (showDesignGrid) DesignGrid()
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF001F3F) @Composable fun MainSettingsScreenPreview() { CurrentTheme { Box(Modifier.fillMaxSize().background(Color(0xFF001F3F))) { CompositionLocalProvider(LocalMergeDuplicates provides true, LocalShowDuration provides true, LocalShowIncoming provides true, LocalShowOutgoing provides true, LocalShowMissed provides true, LocalShowBlocked provides true, LocalShowUnknown provides true, LocalHistoryLimit provides 100, LocalCompactView provides false, LocalShowFrequentHub provides true, LocalFavoriteNumbers provides emptySet(), LocalStrictGrouping provides false, LocalSwipeActionsEnabled provides true, LocalAnimationMultiplier provides 1f) { MainSettingsScreen(listOf(CallRecord(1, "Alice", "1234567890", CallLog.Calls.INCOMING_TYPE, System.currentTimeMillis(), 120, null)), { null }, null, {}, {}, {}, {}, {}) } } } }
@Preview(showBackground = true, backgroundColor = 0xFF001F3F) @Composable fun DeveloperOptionsScreenPreview() { CurrentTheme { Box(Modifier.fillMaxSize().background(Color(0xFFFFFFFF))) { DeveloperOptionsScreen({}, {}, {}, {}, {}, {}) } } }
@Preview(showBackground = true, backgroundColor = 0xFF001F3F) @Composable fun ThemeAccentScreenPreview() { CurrentTheme { Box(Modifier.fillMaxSize().background(Color(0xFFFFFFFF))) { ThemeAccentScreen({}, {}, {}, {}, {}, {}, {}) } } }
@Preview(showBackground = true, backgroundColor = 0xFF001F3F) @Composable fun CallHistoryDisplayScreenPreview() { CurrentTheme { Box(Modifier.fillMaxSize().background(Color(0xFFFFFFFF))) { CallHistoryDisplayScreen({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) } } }
@Preview(showBackground = true, backgroundColor = 0xFF001F3F) @Composable fun AlertsScreenPreview() { CurrentTheme { Box(Modifier.fillMaxSize().background(Color(0xFFFFFFFF))) { AlertsScreen({}, {}, {}, {}) } } }
@Preview(showBackground = true, backgroundColor = 0xFF001F3F) @Composable fun AccountScreenPreview() { CurrentTheme { Box(Modifier.fillMaxSize().background(Color(0xFF001F3F))) { AccountScreen(UserSession("Andrei Popescu", "andrei@example.com", null, "+40 7xx xxx xxx"), {}, {}, {}, {}) } } }
@Preview(showBackground = true, backgroundColor = 0xFF001F3F) @Composable fun AboutScreenPreview() { CurrentTheme { Box(Modifier.fillMaxSize().background(Color(0xFFFFFFFF))) { AboutScreen({}) } } }
@Preview(showBackground = true, backgroundColor = 0xFF001F3F) @Composable fun SettingsScreenPreview() { CurrentTheme { Box(Modifier.fillMaxSize().background(Color(0xFF001F3F))) { SettingsScreen(null, {}, {}, {}, {}, {}, {}, {}) } } }

@Preview(showBackground = true)
@Composable
fun InCallScreenRingingPreview() {
    CallManager.callState = Call.STATE_RINGING
    CallManager.lastName = "Alice"
    CallManager.lastNumber = "1234567890"
    CurrentTheme(darkTheme = true) {
        Box(Modifier.fillMaxSize().background(VibrantTheme.background())) {
            CompositionLocalProvider(LocalAnimationMultiplier provides 1f) {
                InCallScreen({}, {}, {}, {})
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InCallScreenActivePreview() {
    CallManager.callState = Call.STATE_ACTIVE
    CallManager.lastName = "Alice"
    CallManager.lastNumber = "1234567890"
    CallManager.durationSeconds = 125
    CallManager.isMuted = false
    CallManager.audioRoute = CallAudioState.ROUTE_EARPIECE
    CurrentTheme(darkTheme = true) {
        Box(Modifier.fillMaxSize().background(VibrantTheme.background())) {
            CompositionLocalProvider(LocalAnimationMultiplier provides 1f) {
                InCallScreen({}, {}, {}, {})
            }
        }
    }
}
