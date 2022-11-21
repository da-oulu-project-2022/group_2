package com.example.mobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        var logged: Boolean = false
        var noNeedAccount: Boolean = false

        if (logged || noNeedAccount) {
            val nextPage = Intent(this, HomeActivity::class.java)
            startActivity(nextPage)
        } else {
            val nextPage = Intent(this, SignInActivity::class.java)
            startActivity(nextPage)
        }

        finish()
    }
}