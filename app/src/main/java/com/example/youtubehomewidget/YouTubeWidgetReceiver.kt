package com.example.youtubehomewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

class YouTubeWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        for (appWidgetId in appWidgetIds) {

            val views = RemoteViews(
                context.packageName,
                R.layout.widget_youtube
            )

            views.setTextViewText(
                R.id.widget_title,
                "YouTube For You"
            )

            views.setTextViewText(
                R.id.widget_status,
                "Tap to open YouTube"
            )

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/")
            )

            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(
                R.id.widget_open,
                pendingIntent
            )

            appWidgetManager.updateAppWidget(
                appWidgetId,
                views
            )
        }
    }
}
