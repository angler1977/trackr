package com.trackr.ui.screens.more

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.trackr.data.remote.amap.AmapSdkManager

@Composable
fun AmapKeySettings() {
 val context=LocalContext.current
 val prefs=remember { context.getSharedPreferences("amap_settings",Context.MODE_PRIVATE) }
 var key by remember { mutableStateOf(prefs.getString("android_key","").orEmpty()) }
 var draft by remember { mutableStateOf("") }; var editing by remember { mutableStateOf(false) }
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Text("地图与服务",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
  Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) {
   Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text("高德 Android Key",style=MaterialTheme.typography.titleSmall)
    Text(if(key.isBlank()) "尚未配置。地图和高德地点搜索需要自行申请 Key。" else "已配置（"+key.take(4)+"••••••••）",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     FilledTonalButton(onClick={draft=key;editing=true}) { Text(if(key.isBlank()) "填写 Key" else "修改 Key") }
     TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://console.amap.com/")))}) { Text("申请 Key") }
     if(key.isNotBlank()) TextButton(onClick={key="" ;prefs.edit().remove("android_key").apply();AmapSdkManager.applyApiKey(context,null)}) {Text("清除")}
    }
    Text("Key 仅保存在本机。Android Key 需绑定应用包名和签名 SHA1。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
 }
 if(editing) AlertDialog(onDismissRequest={editing=false},title={Text("配置高德 Android Key")},text={OutlinedTextField(draft,{draft=it},label={Text("API Key")},singleLine=true)},confirmButton={TextButton(onClick={key=draft.trim();prefs.edit().putString("android_key",key).apply();AmapSdkManager.applyApiKey(context,key.ifBlank{null});editing=false}){Text("保存")}},dismissButton={TextButton(onClick={editing=false}){Text("取消")}})
}
