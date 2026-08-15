package com.example.youtubehomewidget

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val apiKeyInput = findViewById<EditText>(R.id.api_key_input)
        val saveButton = findViewById<Button>(R.id.save_button)

        val preferences = getSharedPreferences("settings", MODE_PRIVATE)

        apiKeyInput.setText(
            preferences.getString("youtube_api_key", "")
        )

        saveButton.setOnClickListener {
            val key = apiKeyInput.text.toString().trim()

            if (key.isEmpty()) {
                Toast.makeText(
                    this,
                    "Enter your YouTube API key",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            preferences.edit()
                .putString("youtube_api_key", key)
                .apply()

            Toast.makeText(
                this,
                "API key saved",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
