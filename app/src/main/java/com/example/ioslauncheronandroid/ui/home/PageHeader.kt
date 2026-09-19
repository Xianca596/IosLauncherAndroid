package com.example.ioslauncheronandroid.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ioslauncheronandroid.ui.glass.GlassButton

@Composable
fun PageHeader(
    caption: String,
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    titleContent: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(caption, color = Color.White.copy(0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            if (titleContent != null) titleContent() else Text(title, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        GlassButton(actionLabel, onClick = onAction)
    }
}
