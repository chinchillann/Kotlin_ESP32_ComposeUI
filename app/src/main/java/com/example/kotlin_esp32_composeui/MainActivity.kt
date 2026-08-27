package com.example.kotlin_esp32_composeui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.text.SimpleDateFormat
import java.util.*

val BgColor = Color(0xFF0F172A)
val CardBg = Color(0xFF1E293B)
val AccentBlue = Color(0xFF38BDF8)
val AccentGreen = Color(0xFF22C55E)
val AccentRed = Color(0xFFEF4444)
val BorderColor = Color(0xFF334155)
val ConsoleBg = Color(0xFF090D16)
val LogTextColor = Color(0xFFA7F3D0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NodeMcuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgColor
                ) {
                    Esp32ControlConsole()
                }
            }
        }
    }
}

@Composable
fun NodeMcuTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        background = BgColor,
        surface = CardBg,
        onSurface = Color.White
    )
    MaterialTheme(colorScheme = darkColorScheme, content = content)
}

@Composable
fun Esp32ControlConsole() {
    var ipAddress by remember { mutableStateOf("192.168.1.100") }
    var customMsg by remember { mutableStateOf("") }
    var isConnected by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf("[系統] 請輸入 IP 並點擊連線...") }

    val client = remember { OkHttpClient() }
    // 明確宣告 WebSocket? 型別即可解決 null 與推導問題
    var webSocket by remember { mutableStateOf<WebSocket?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun addLog(text: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.add("[$time] $text")
        coroutineScope.launch {
            if (logs.isNotEmpty()) {
                listState.animateScrollToItem(logs.size - 1)
            }
        }
    }

    fun toggleConnect() {
        if (isConnected) {
            webSocket?.close(1000, "使用者關閉")
            webSocket = null
            isConnected = false
            addLog("WebSocket 連線已斷開")
        } else {
            if (ipAddress.isBlank()) {
                addLog("錯誤: 請輸入有效的 ESP32 IP 位址！")
                return
            }
            val wsUrl = "ws://$ipAddress:81/"
            addLog("正在嘗試連線至 $wsUrl...")

            val request = Request.Builder().url(wsUrl).build()
            val listener = object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    isConnected = true
                    addLog("成功連線至 NodeMCU-32S Wi-Fi！")
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    addLog("ESP32 傳來: $text")
                }

                override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                    isConnected = false
                    addLog("連線關閉中...")
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    isConnected = false
                    addLog("WebSocket 連線已斷開")
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    isConnected = false
                    addLog("連線發生錯誤: ${t.localizedMessage}")
                }
            }
            webSocket = client.newWebSocket(request, listener)
        }
    }

    fun sendCommand(cmd: String) {
        if (!isConnected || webSocket == null) {
            addLog("錯誤: 請先連線至 ESP32 Wi-Fi！")
            return
        }
        val cleanCmd = cmd.trim()
        webSocket?.send(cleanCmd)
        addLog("已傳送: $cleanCmd")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CardContainer {
            Text(
                text = "NodeMCU-32S Wi-Fi 控制台",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AccentBlue,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = { Text("ESP32 IP 位址") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = BgColor,
                    unfocusedContainerColor = BgColor
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { toggleConnect() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) AccentRed else AccentBlue
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isConnected) "斷開 Wi-Fi 連線" else "連線至 ESP32",
                    color = if (isConnected) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Surface(
                    color = if (isConnected) AccentGreen.copy(alpha = 0.2f) else BorderColor,
                    border = if (isConnected) androidx.compose.foundation.BorderStroke(1.dp, AccentGreen) else null,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (isConnected) "已連線" else "尚未連線",
                        color = if (isConnected) AccentGreen else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        CardContainer {
            Text("板載 LED 控制 (GPIO 2)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { sendCommand("LED_ON") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("開燈 (ON)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { sendCommand("LED_OFF") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("關燈 (OFF)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        CardContainer {
            Text("傳送自訂指令/文字", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customMsg,
                    onValueChange = { customMsg = it },
                    placeholder = { Text("輸入要傳給 ESP32 的訊息...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = BgColor,
                        unfocusedContainerColor = BgColor
                    )
                )
                Button(
                    onClick = {
                        if (customMsg.isNotBlank()) {
                            sendCommand(customMsg)
                            customMsg = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("傳送", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        CardContainer {
            Text("即時接收日誌 (WebSocket)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(ConsoleBg, shape = RoundedCornerShape(6.dp))
                    .border(1.dp, BorderColor, shape = RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(state = listState) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            color = LogTextColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardContainer(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}