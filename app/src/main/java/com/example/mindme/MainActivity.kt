package com.example.mindme

// Added missing imports for Column, Spacer, height, dp, sp, and FontWeight
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        android.util.Log.d("MindMe", "🚀 onCreate başladı")

        NotificationHelper.createNotificationChannel(this)
        android.util.Log.d("MindMe", "✅ Bildirim kanalı oluşturuldu")

         android.util.Log.d("MindMe", "✅ Worker planlandı")

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
}

// Added optional modifier parameter to allow applying padding from Scaffold/parent
@Composable
fun MindMeScreen(
    modifier: Modifier = Modifier,
    viewModel: TaskViewModel = viewModel()
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredTasks = if (searchQuery.isBlank()) {
        tasks
    } else {
        tasks.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Create, contentDescription = "Görev Ekle")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "MindMe",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Let me remind you",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Görevlerde ara...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Ara"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Temizle"
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            if (filteredTasks.isEmpty()) {
                Text(
                    text = if (searchQuery.isBlank()) {
                        "Henüz görev yok. + butonuna basarak ekle."
                    } else {
                        "\"$searchQuery\" ile eşleşen görev bulunamadı."
                    },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onMarkCompleted = { viewModel.markAsCompleted(it) },
                            onMarkMissed = { viewModel.markAsMissed(it) },
                            onReschedule = { t, newDateTime -> viewModel.markAsPending(t, newDateTime) },
                            onDelete = { viewModel.deleteTask(it) },
                            onDateChange = { t, newDate -> viewModel.updateTaskDate(t, newDate) },
                            onTimeChange = { t, h, m -> viewModel.updateTaskTime(t, h, m) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, dateTime, repeatType, reminder ->   // ← 4 parametre (reminder eklendi)
                viewModel.addTask(title, dateTime, repeatType, reminder)
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
    onTimeChange: (Task, Int, Int) -> Unit = { _, _, _ -> }
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showReschedulePicker by remember { mutableStateOf(false) }

    // Duruma göre ikon ve renk
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (task.status) {
                TaskStatus.MISSED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                TaskStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                TaskStatus.PENDING -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Üst: ikon + başlık + tarih/saat
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = statusIcon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (task.status == TaskStatus.COMPLETED)
                                TextDecoration.LineThrough else TextDecoration.None,
                            color = statusColor
                        )
                        // Tekrarlayan görevse ikon göster
                        if (task.repeatType != RepeatType.NONE) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = when (task.repeatType) {
                                        RepeatType.DAILY -> "Günlük"
                                        RepeatType.WEEKLY -> "Haftalık"
                                        RepeatType.MONTHLY -> "Aylık"
                                        RepeatType.YEARLY -> "Yıllık"

                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "📅 " + task.dueDateTime.format(
                                DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("tr-TR"))
                            ),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { showDatePicker = true }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "🕐 " + task.dueDateTime.format(
                                DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("tr-TR"))
                            ),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { showTimePicker = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Alt: aksiyon butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (task.status != TaskStatus.COMPLETED) {
                        TextButton(onClick = { onMarkCompleted(task) }) {
                            Text("✅ Yaptım")
                        }
                    }
                    if (task.status != TaskStatus.MISSED) {
                        TextButton(onClick = { onMarkMissed(task) }) {
                            Text("❌ Yapamadım")
                        }
                    }
                    if (task.status == TaskStatus.MISSED) {
                        TextButton(onClick = { showReschedulePicker = true }) {
                            Text("🔄 Yeniden Planla")
                        }
                    }
                }
                IconButton(onClick = { onDelete(task) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // 📅 Tarih Değiştirme Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = task.dueDateTime
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        onDateChange(task, picked)
                    }
                    showDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("İptal") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // 🕐 Saat Değiştirme Dialog
    if (showTimePicker) {
        var isInputMode by remember { mutableStateOf(false) }

        val timePickerState = rememberTimePickerState(
            initialHour = task.dueDateTime.hour,
            initialMinute = task.dueDateTime.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Saat Seç")
                    IconButton(onClick = { isInputMode = !isInputMode }) {
                        Icon(
                            imageVector = if (isInputMode) Icons.Default.Schedule else Icons.Default.Edit,
                            contentDescription = if (isInputMode) "Kadrana geç" else "Klavyeye geç"
                        )
                    }
                }
            },
            text = {
                if (isInputMode) {
                    TimeInput(state = timePickerState)
                } else {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(task, timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("İptal") }
            }
        )
    }

    // 🔄 Yeniden Planlama Dialog
    if (showReschedulePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showReschedulePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        val newDateTime = task.dueDateTime
                            .withYear(picked.year)
                            .withMonth(picked.monthValue)
                            .withDayOfMonth(picked.dayOfMonth)
                        onReschedule(task, newDateTime)
                    }
                    showReschedulePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { showReschedulePicker = false }) { Text("İptal") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, LocalDateTime, RepeatType, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedDateTime by remember {
        mutableStateOf(LocalDateTime.now().plusHours(1))
    }
    var selectedReminder by remember { mutableStateOf(2) } // Varsayılan 2 dk
    var expandedDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedRepeat by remember { mutableStateOf(RepeatType.NONE) }

    val dateFormatter = DateTimeFormatter.ofPattern(
        "dd MMM yyyy, HH:mm",
        Locale.forLanguageTag("tr-TR")
    )

    // 📅 Tarih Seçici Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        selectedDateTime = selectedDateTime
                            .withYear(picked.year)
                            .withMonth(picked.monthValue)
                            .withDayOfMonth(picked.dayOfMonth)
                    }
                    showDatePicker = false
                }) {
                    Text("Tamam")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("İptal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // 🕐 Saat Seçici Dialog
    if (showTimePicker) {
        var isInputMode by remember { mutableStateOf(false) }

        val timePickerState = rememberTimePickerState(
            initialHour = selectedDateTime.hour,
            initialMinute = selectedDateTime.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Saat Seç")
                    IconButton(onClick = { isInputMode = !isInputMode }) {
                        Icon(
                            imageVector = if (isInputMode) Icons.Default.Schedule else Icons.Default.Edit,
                            contentDescription = if (isInputMode) "Kadrana geç" else "Klavyeye geç"
                        )
                    }
                }
            },
            text = {
                if (isInputMode) {
                    TimeInput(state = timePickerState)
                } else {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateTime = selectedDateTime
                        .withHour(timePickerState.hour)
                        .withMinute(timePickerState.minute)
                    showTimePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("İptal") }
            }
        )
    }

    // 📝 Ana Dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Görev") },
        text = {
            Column {
                // 1. Görev Başlığı
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Görev başlığı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Zaman Bilgisi ve Seçiciler
                Text(
                    text = "Zaman: ${selectedDateTime.format(dateFormatter)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("📅 Tarih Seç")
                    }
                    TextButton(onClick = { showTimePicker = true }) {
                        Text("🕐 Saat Seç")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Tekrar Seçici
                Text(
                    text = "Tekrar:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
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
                                    maxLines = 1,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Hatırlatma Zamanı Dropdown
                Text(
                    text = "Hatırlatma Zamanı:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Dropdown Alanı
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
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

                    // Menüyü aşağı doğru açmaya zorluyoruz
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.offset(y = 40.dp) // Import'u unutma!
                    ) {
                        val options = listOf(0, 15, 30, 45, 60, 120)
                        options.forEach { minutes ->
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
                                onClick = {
                                    selectedReminder = minutes
                                    expandedDropdown = false
                                }
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
                        onAdd(title.trim(), selectedDateTime, selectedRepeat, selectedReminder)
                    }
                }
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
