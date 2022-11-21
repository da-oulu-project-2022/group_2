package com.example.mobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        //navigation
        val backArrow = findViewById<TextView>(R.id.signup_nav_bar)
        backArrow.setOnClickListener {
            val signInpage = Intent(this, SignInActivity::class.java)
            startActivity(signInpage)
            finish()
        }

    }


}