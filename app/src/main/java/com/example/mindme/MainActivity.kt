package com.example.mindme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mindme.data.Category
import com.example.mindme.data.RepeatType
import com.example.mindme.data.Task
import com.example.mindme.data.TaskStatus
import com.example.mindme.data.notification.NotificationHelper
import com.example.mindme.ui.theme.MindMeTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // YENİ: Splash Screen API'sini başlat (super.onCreate'ten ÖNCE olmalı)
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // YENİ: Splash ekranını 1 saniye (1000ms) boyunca ekranda tut
        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }

        android.os.Handler(mainLooper).postDelayed({
            isReady = true
        }, 1000) // 1000 milisaniye = 1 saniye

        NotificationHelper.createNotificationChannel(this)
        scheduleDailySummaries(this)
        requestNotificationPermission()

        setContent {
            MindMeTheme {
                MindMeScreen()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
    private fun scheduleDailySummaries(context: android.content.Context) {
        val workManager = androidx.work.WorkManager.getInstance(context)

        // 1. SABAH ÖZETİ (Her sabah 09:00)
        val morningRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.mindme.data.notification.MorningSummaryWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        )
            .setInitialDelay(calculateDelayUntil(9, 0), java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "morning_summary_work",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            morningRequest
        )

        // 2. AKŞAM ÖZETİ (Her akşam 21:00)
        val eveningRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.mindme.data.notification.EveningSummaryWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        )
            .setInitialDelay(calculateDelayUntil(21, 0), java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "evening_summary_work",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            eveningRequest
        )

        // Test etmek için Logcat'e mesaj basalım
        android.util.Log.d("MindMe_Test", "✅ Sabah ve Akşam özeti başarıyla planlandı!")
    }

    // Hedef saate kaç ms kaldığını hesaplayan yardımcı fonksiyon
    private fun calculateDelayUntil(hour: Int, minute: Int): Long {
        val now = java.time.LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0)

        // Eğer hedef saat bugün geçmişse, yarına ayarla
        if (target.isBefore(now)) {
            target = target.plusDays(1)
        }
        return java.time.Duration.between(now, target).toMillis()
    }
}

@Composable
fun MindMeScreen(
    modifier: Modifier = Modifier,
    viewModel: TaskViewModel = viewModel() // <-- BUNUN SİLİNMEDİĞİNDEN EMİN OL
) {
    // Hata veren kısım burasıydı: 'by' ve 'collectAsStateWithLifecycle' eksikti
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var logFilter by remember { mutableStateOf(0) }

    val today = java.time.LocalDate.now()

    // Sadece bugün ve gelecek günlerin görevlerini göster
    val upcomingTasks = tasks.filter {
        it.dueDateTime.toLocalDate() >= today
    }

    val filteredTasks = if (searchQuery.isBlank()) {
        upcomingTasks
    } else {
        upcomingTasks.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Create, contentDescription = "Görev Ekle")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = "Görevler") },
                    label = { Text("Görevler") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Geçmiş") },
                    label = { Text("Geçmiş") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // === BAŞLIK VE ARAMA ÇUBUĞU (YAN YANA) ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("MindMe", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Let me remind you", fontSize = 12.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                }

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Ara...", fontSize = 12.sp) },
                        modifier = Modifier.width(220.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // === GÖREVLER SEKMESİ ===
            if (selectedTab == 0) {
                if (filteredTasks.isEmpty()) {
                    Text("Görev bulunamadı.", color = Color.Gray)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredTasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onMarkCompleted = { viewModel.markAsCompleted(it) },
                                onMarkMissed = { viewModel.markAsMissed(it) },
                                onReschedule = { t, newDateTime -> viewModel.markAsPending(t, newDateTime) },
                                onDelete = { viewModel.deleteTask(it) },
                                onDateChange = { t, newDate -> viewModel.updateTaskDate(t, newDate) },
                                onTimeChange = { t, h, m -> viewModel.updateTaskTime(t, h, m) },
                                onEditTitle = { t, newTitle -> viewModel.updateTaskTitle(t, newTitle) }
                            )
                        }
                    }
                }
            } else {
                // === GEÇMİŞ SEKMESİ ===
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("Tümü", "Yapılanlar", "Yapılamayanlar")
                    filters.forEachIndexed { index, title ->
                        val isSelected = logFilter == index
                        Surface(
                            selected = isSelected,
                            onClick = { logFilter = index },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val filteredLogs = when (logFilter) {
                    1 -> logs.filter { it.status == TaskStatus.COMPLETED }
                    2 -> logs.filter { it.status == TaskStatus.MISSED }
                    else -> logs
                }

                if (filteredLogs.isEmpty()) {
                    Text("Bu filtreye uygun bir işlem geçmişi yok.", color = Color.Gray)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredLogs, key = { it.id }) { log ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(log.taskTitle, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = log.actionDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.forLanguageTag("tr-TR"))),
                                            fontSize = 12.sp, color = Color.Gray
                                        )
                                    }
                                    Icon(
                                        imageVector = if (log.status == TaskStatus.COMPLETED) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (log.status == TaskStatus.COMPLETED) Color(0xFF4CAF50) else Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, dateTime, repeatType, reminder, category ->
                viewModel.addTask(title, dateTime, repeatType, reminder, category)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskRow(
    task: Task,
    onMarkCompleted: (Task) -> Unit = {},
    onMarkMissed: (Task) -> Unit = {},
    onReschedule: (Task, LocalDateTime) -> Unit = { _, _ -> },
    onDelete: (Task) -> Unit = {},
    onDateChange: (Task, java.time.LocalDate) -> Unit = { _, _ -> },
    onTimeChange: (Task, Int, Int) -> Unit = { _, _, _ -> },
    onEditTitle: (Task, String) -> Unit = { _, _ -> }
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showReschedulePicker by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val statusIcon = when (task.status) {
        TaskStatus.PENDING -> "⬜"
        TaskStatus.COMPLETED -> "✅"
        TaskStatus.MISSED -> "❌"
    }
    val statusColor = when (task.status) {
        TaskStatus.PENDING -> MaterialTheme.colorScheme.onSurface
        TaskStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
        TaskStatus.MISSED -> MaterialTheme.colorScheme.error
    }

    // SADECE SOLA KAYDIRMA (SİLME) İÇİN STATE
    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {
                onDelete(task)
                true
            } else {
                false
            }
        }
    )

    androidx.compose.material3.SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // Sadece kaydırma anında arka planı kırmızı yap ve ikonu göster
            if (dismissState.dismissDirection != androidx.compose.material3.SwipeToDismissBoxValue.Settled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false // Sağa kaydırmayı tamamen kapat
    ) {
        // --- KART TASARIMI ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { },
                    onLongClick = { showEditDialog = true }
                ),
            colors = CardDefaults.cardColors(
                containerColor = when (task.status) {
                    TaskStatus.MISSED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f) // Şeffaflığı azalttık
                    TaskStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    TaskStatus.PENDING -> MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // ÜST KISIM
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = statusIcon, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (task.status == TaskStatus.COMPLETED)
                                TextDecoration.LineThrough else TextDecoration.None,
                            color = statusColor,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(1.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📅 " + task.dueDateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("tr-TR"))),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { showDatePicker = true }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🕐 " + task.dueDateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("tr-TR"))),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { showTimePicker = true }
                            )
                        }
                    }

                    // Sağ Taraf Etiketler
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        if (task.category != Category.NONE) {
                            Surface(
                                color = when(task.category) {
                                    Category.WORK -> Color(0xFFE3F2FD)
                                    Category.PERSONAL -> Color(0xFFE8F5E9)
                                    Category.SHOPPING -> Color(0xFFFFF3E0)
                                    else -> Color.Transparent
                                },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = task.category.displayName,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    color = Color.Black
                                )
                            }
                        }

                        if (task.repeatType != RepeatType.NONE) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = when (task.repeatType) {
                                        RepeatType.DAILY -> "Günlük"
                                        RepeatType.WEEKLY -> "Haftalık"
                                        RepeatType.MONTHLY -> "Aylık"
                                        RepeatType.YEARLY -> "Yıllık"
                                        else -> ""
                                    },
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // ALT KISIM BUTONLAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.status != TaskStatus.COMPLETED) {
                        TextButton(
                            onClick = { onMarkCompleted(task) },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("✅ Yaptım", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                    if (task.status != TaskStatus.MISSED) {
                        TextButton(
                            onClick = { onMarkMissed(task) },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("❌ Yapamadım", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                    if (task.status == TaskStatus.MISSED) {
                        TextButton(
                            onClick = { showReschedulePicker = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("🔄 Yenile", fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                data = android.provider.CalendarContract.Events.CONTENT_URI
                                putExtra(android.provider.CalendarContract.Events.TITLE, task.title)
                                putExtra(
                                    android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                    task.dueDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                                )
                            }
                            try { context.startActivity(intent) } catch (e: Exception) {}
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("📅 Takvim", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }

    // Dialoglar (Aynı kalacak)
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = task.dueDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        onDateChange(task, picked)
                    }
                    showDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("İptal") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        var isInputMode by remember { mutableStateOf(false) }
        val timePickerState = rememberTimePickerState(initialHour = task.dueDateTime.hour, initialMinute = task.dueDateTime.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Saat Seç")
                    IconButton(onClick = { isInputMode = !isInputMode }) {
                        Icon(imageVector = if (isInputMode) Icons.Default.Schedule else Icons.Default.Edit, contentDescription = null)
                    }
                }
            },
            text = { if (isInputMode) TimeInput(state = timePickerState) else TimePicker(state = timePickerState) },
            confirmButton = { TextButton(onClick = { onTimeChange(task, timePickerState.hour, timePickerState.minute); showTimePicker = false }) { Text("Tamam") } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("İptal") } }
        )
    }

    if (showReschedulePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showReschedulePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        val newDateTime = task.dueDateTime.withYear(picked.year).withMonth(picked.monthValue).withDayOfMonth(picked.dayOfMonth)
                        onReschedule(task, newDateTime)
                    }
                    showReschedulePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { showReschedulePicker = false }) { Text("İptal") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEditDialog) {
        var editTitle by remember { mutableStateOf(task.title) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Görevi Düzenle") },
            text = { OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Görev başlığı") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { if (editTitle.isNotBlank()) { onEditTitle(task, editTitle); showEditDialog = false } }) { Text("Kaydet") } },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("İptal") } }
        )
    }
}
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, LocalDateTime, RepeatType, Int, Category) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(Category.NONE) }
    var title by remember { mutableStateOf("") }
    var selectedDateTime by remember { mutableStateOf(LocalDateTime.now().plusHours(1)) }
    var selectedReminder by remember { mutableStateOf(2) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedRepeat by remember { mutableStateOf(RepeatType.NONE) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.forLanguageTag("tr-TR"))

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        selectedDateTime = selectedDateTime.withYear(picked.year).withMonth(picked.monthValue).withDayOfMonth(picked.dayOfMonth)
                    }
                    showDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("İptal") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        var isInputMode by remember { mutableStateOf(false) }
        val timePickerState = rememberTimePickerState(initialHour = selectedDateTime.hour, initialMinute = selectedDateTime.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Saat Seç")
                    IconButton(onClick = { isInputMode = !isInputMode }) {
                        Icon(imageVector = if (isInputMode) Icons.Default.Schedule else Icons.Default.Edit, contentDescription = null)
                    }
                }
            },
            text = { if (isInputMode) TimeInput(state = timePickerState) else TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = { selectedDateTime = selectedDateTime.withHour(timePickerState.hour).withMinute(timePickerState.minute); showTimePicker = false }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("İptal") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Görev") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Görev başlığı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Zaman: ${selectedDateTime.format(dateFormatter)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showDatePicker = true }) { Text("📅 Tarih Seç") }
                    TextButton(onClick = { showTimePicker = true }) { Text("🕐 Saat Seç") }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Tekrar:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RepeatType.values().filter { it != RepeatType.NONE }.forEach { type ->
                        val isSelected = selectedRepeat == type
                        Surface(
                            selected = isSelected,
                            onClick = { selectedRepeat = if (isSelected) RepeatType.NONE else type },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            // Seçiliyse koyu mavi (primary), değilse açık gri (surfaceVariant)
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (type) {
                                        RepeatType.DAILY -> "Günlük"
                                        RepeatType.WEEKLY -> "Haftalık"
                                        RepeatType.MONTHLY -> "Aylık"
                                        RepeatType.YEARLY -> "Yıllık"
                                        else -> ""
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, // Seçiliyse kalın yazı
                                    maxLines = 1,
                                    // Seçiliyse beyaz yazı, değilse koyu gri
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Kategori:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(Category.WORK, Category.PERSONAL, Category.SHOPPING).forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            selected = isSelected,
                            onClick = { selectedCategory = if (isSelected) Category.NONE else cat },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = when(cat) {
                                Category.WORK -> if (isSelected) Color(0xFF1976D2) else Color(0xFFE3F2FD)
                                Category.PERSONAL -> if (isSelected) Color(0xFF388E3C) else Color(0xFFE8F5E9)
                                Category.SHOPPING -> if (isSelected) Color(0xFFF57C00) else Color(0xFFFFF3E0)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = cat.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Hatırlatma Zamanı:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = when (selectedReminder) {
                                0 -> "Görev saatinde"
                                15 -> "15 dakika önce"
                                30 -> "30 dakika önce"
                                45 -> "45 dakika önce"
                                60 -> "1 saat önce"
                                120 -> "2 saat önce"
                                else -> "$selectedReminder dakika önce"
                            }
                        )
                    }
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.offset(y = 40.dp)
                    ) {
                        listOf(0, 15, 30, 45, 60, 120).forEach { minutes ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (minutes) {
                                            0 -> "Görev saatinde"
                                            15 -> "15 dakika önce"
                                            30 -> "30 dakika önce"
                                            45 -> "45 dakika önce"
                                            60 -> "1 saat önce"
                                            120 -> "2 saat önce"
                                            else -> "$minutes dakika önce"
                                        }
                                    )
                                },
                                onClick = { selectedReminder = minutes; expandedDropdown = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(title.trim(), selectedDateTime, selectedRepeat, selectedReminder, selectedCategory)
                    }
                }
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}