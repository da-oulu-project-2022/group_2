package com.example.mobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class SignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        //declare the animation
        val topTomiddle = AnimationUtils.loadAnimation(this, R.anim.toptomiddle)
        val scaleTobig = AnimationUtils.loadAnimation(this, R.anim.scaletobig)

        val dropSlogan = findViewById<TextView>(R.id.textView_slogan)
        val scaleIcon = findViewById<ImageView>(R.id.ic_boxing)

        //set the animation
        dropSlogan.startAnimation(topTomiddle)
        scaleIcon.startAnimation(scaleTobig)

        //navigation
        val trainingButton = findViewById<TextView>(R.id.no_need_account_button)
        trainingButton.setOnClickListener {
            val nextPage = Intent(this, HomeActivity::class.java)
            startActivity(nextPage)
            finish()
        }

        val signUp = findViewById<Button>(R.id.button_createAccount)
        signUp.setOnClickListener {
            val signUppage = Intent(this, SignUpActivity::class.java)
            startActivity(signUppage)
            finish()
        }

    }
}