package com.example.mobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val trainingButton = findViewById<Button>(R.id.training_button)
        val helpButton = findViewById<Button>(R.id.help_button)
        val settingButton = findViewById<Button>(R.id.setting_button)

        trainingButton.setOnClickListener {
            val nextPage = Intent(this, TrainingActivity::class.java)
            startActivity(nextPage)
        }

        helpButton.setOnClickListener {
            val nextPage = Intent(this, HelpActivity::class.java)
            startActivity(nextPage)
        }

        settingButton.setOnClickListener {
            val nextPage = Intent(this, SettingActivity::class.java)
            startActivity(nextPage)
        }
    }
}