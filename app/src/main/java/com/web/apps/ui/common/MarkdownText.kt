package com.web.apps.ui.common

import android.widget.TextView
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.linkify.LinkifyPlugin

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textSizeSp: Float = 15f
) {
    val contentColor = LocalContentColor.current.toArgb()

    AndroidView(
        factory = { context ->
            val textView = TextView(context).apply {
                setTextColor(contentColor)
                textSize = textSizeSp
            }

            val markwon = Markwon.builder(context)
                .usePlugin(TablePlugin.create(context))
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TaskListPlugin.create(context))
                .usePlugin(LinkifyPlugin.create())
                .build()

            markwon.setMarkdown(textView, markdown)
            textView
        },
        update = { textView ->
            textView.setTextColor(contentColor)
            val markwon = Markwon.builder(textView.context)
                .usePlugin(TablePlugin.create(textView.context))
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TaskListPlugin.create(textView.context))
                .usePlugin(LinkifyPlugin.create())
                .build()
            markwon.setMarkdown(textView, markdown)
        },
        modifier = modifier
    )
}