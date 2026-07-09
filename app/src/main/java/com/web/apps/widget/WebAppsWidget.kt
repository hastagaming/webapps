package com.web.apps.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.web.apps.MainActivity
import kotlinx.coroutines.flow.first

class WebAppsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val containerRepository = getContainerRepositoryForWidget(context)
        val containers = containerRepository.observeAllContainers().first().take(8)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFFFFFFFF))
                    .padding(8.dp)
            ) {
                Text(
                    text = "WebApps",
                    style = TextStyle(fontSize = androidx.compose.ui.unit.sp(16))
                )

                containers.forEach { container ->
                    val intent = android.content.Intent(context, MainActivity::class.java).apply {
                        putExtra("EXTRA_CONTAINER_ID", container.containerId)
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }

                    Text(
                        text = container.name,
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable(actionStartActivity(intent))
                    )
                }

                if (containers.isEmpty()) {
                    Text(text = "No containers yet")
                }
            }
        }
    }
}