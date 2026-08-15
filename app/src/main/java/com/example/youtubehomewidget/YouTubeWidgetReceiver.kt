package com.example.youtubehomewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import org.json.JSONObject

class YouTubeWidgetReceiver : AppWidgetProvider() {

    companion object {
        private const val ACTION_REFRESH =
            "com.example.youtubehomewidget.REFRESH"

        private const val API_URL =
            "https://www.googleapis.com/youtube/v3/search"

        private const val MAX_VIDEOS = 3
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            loadVideos(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(
                    context,
                    YouTubeWidgetReceiver::class.java
                )
            )

            for (id in ids) {
                loadVideos(context, manager, id)
            }
        }
    }

    private fun loadVideos(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int
    ) {
        val preferences =
            context.getSharedPreferences("settings", Context.MODE_PRIVATE)

        val apiKey =
            preferences.getString("youtube_api_key", "") ?: ""

        if (apiKey.isEmpty()) {
            showMessage(
                context,
                manager,
                widgetId,
                "Open the app and enter your YouTube API key"
            )
            return
        }

        showMessage(
            context,
            manager,
            widgetId,
            "Loading recommendations..."
        )

        thread {
            try {
                val url = URL(
                    "$API_URL" +
                    "?part=snippet" +
                    "&type=video" +
                    "&maxResults=$MAX_VIDEOS" +
                    "&order=relevance" +
                    "&q=trending" +
                    "&regionCode=AU" +
                    "&key=$apiKey"
                )

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode

                if (responseCode != 200) {
                    throw Exception(
                        "YouTube API returned $responseCode"
                    )
                }

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                val json = JSONObject(response)
                val items = json.getJSONArray("items")

                val videos = mutableListOf<Video>()

                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val id = item
                        .getJSONObject("id")
                        .getString("videoId")

                    val snippet =
                        item.getJSONObject("snippet")

                    val title =
                        snippet.getString("title")

                    val channel =
                        snippet.getString("channelTitle")

                    val thumbnail =
                        snippet
                            .getJSONObject("thumbnails")
                            .getJSONObject("medium")
                            .getString("url")

                    videos.add(
                        Video(
                            id,
                            title,
                            channel,
                            thumbnail
                        )
                    )
                }

                updateWidget(
                    context,
                    manager,
                    widgetId,
                    videos
                )

                connection.disconnect()

            } catch (e: Exception) {
                showMessage(
                    context,
                    manager,
                    widgetId,
                    "Unable to load YouTube videos"
                )
            }
        }
    }

    private fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        videos: List<Video>
    ) {
        val views = RemoteViews(
            context.packageName,
            R.layout.widget_youtube
        )

        views.setTextViewText(
            R.id.widget_title,
            "YouTube For You"
        )

        val slots = listOf(
            Triple(
                R.id.video_1_image,
                R.id.video_1_title,
                R.id.video_1_channel
            ),
            Triple(
                R.id.video_2_image,
                R.id.video_2_title,
                R.id.video_2_channel
            ),
            Triple(
                R.id.video_3_image,
                R.id.video_3_title,
                R.id.video_3_channel
            )
        )

        for (i in slots.indices) {

            val imageId = slots[i].first
            val titleId = slots[i].second
            val channelId = slots[i].third

            if (i < videos.size) {

                val video = videos[i]

                views.setTextViewText(
                    titleId,
                    video.title
                )

                views.setTextViewText(
                    channelId,
                    video.channel
                )

                thread {
                    try {
                        val bitmap =
                            BitmapFactory.decodeStream(
                                URL(video.thumbnail).openStream()
                            )

                        views.setImageViewBitmap(
                            imageId,
                            bitmap
                        )

                        manager.updateAppWidget(
                            widgetId,
                            views
                        )

                    } catch (_: Exception) {
                    }
                }

                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://www.youtube.com/watch?v=${video.id}"
                    )
                )

                val pendingIntent =
                    PendingIntent.getActivity(
                        context,
                        widgetId * 10 + i,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                    )

                views.setOnClickPendingIntent(
                    imageId,
                    pendingIntent
                )

                views.setOnClickPendingIntent(
                    titleId,
                    pendingIntent
                )

            } else {
                views.setTextViewText(
                    titleId,
                    ""
                )

                views.setTextViewText(
                    channelId,
                    ""
                )
            }
        }

        val refreshIntent = Intent(
            context,
            YouTubeWidgetReceiver::class.java
        ).apply {
            action = ACTION_REFRESH
        }

        val refreshPendingIntent =
            PendingIntent.getBroadcast(
                context,
                widgetId + 1000,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        views.setOnClickPendingIntent(
            R.id.refresh_button,
            refreshPendingIntent
        )

        manager.updateAppWidget(
            widgetId,
            views
        )
    }

    private fun showMessage(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        message: String
    ) {
        val views = RemoteViews(
            context.packageName,
            R.layout.widget_youtube
        )

        views.setTextViewText(
            R.id.widget_title,
            "YouTube For You"
        )

        views.setTextViewText(
            R.id.widget_message,
            message
        )

        manager.updateAppWidget(
            widgetId,
            views
        )
    }

    data class Video(
        val id: String,
        val title: String,
        val channel: String,
        val thumbnail: String
    )
}
