package com.example.mobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_home.*
import org.w3c.dom.Text
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

class HomeActivity : AppCompatActivity() {
    var mFirebaseAuth = FirebaseAuth.getInstance();

    private val fileName: String = "MyDevicesId.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val trainingButton = findViewById<Button>(R.id.training_button)
        val helpButton = findViewById<Button>(R.id.help_button)
        val settingButton = findViewById<Button>(R.id.setting_button)
        val staticsButton = findViewById<Button>(R.id.statics_button)
        val backArrow = findViewById<TextView>(R.id.backArrow_tv)


        var mFirebaseUser = mFirebaseAuth.currentUser?.email?.replace("@gmail.com","");

        Log.e("TAG",mFirebaseUser.toString())
        trainingButton.setOnClickListener {
            try {
                var fin: FileInputStream? = null
                fin = openFileInput(fileName)
                var inputStreamReader: InputStreamReader = InputStreamReader(fin)
                val bufferedReader: BufferedReader = BufferedReader(inputStreamReader)

                val nextPage = Intent(this, TrainingActivity::class.java)
                startActivity(nextPage)
            } catch (ex: Exception) {
                if (ex.message?.contains("No such file or directory") == true) {
                    val nextPage = Intent(this, SettingActivity::class.java)
                    startActivity(nextPage)
                }
            }
        }

        helpButton.setOnClickListener {
            val nextPage = Intent(this, HelpActivity::class.java)
            startActivity(nextPage)
        }

        staticsButton.setOnClickListener {
            val nextPage = Intent(this, StatisticActivity::class.java)
            startActivity(nextPage)
        }


        settingButton.setOnClickListener {
            val nextPage = Intent(this, SettingActivity::class.java)
            startActivity(nextPage)
        }

        backArrow.setOnClickListener {
            val nextPage = Intent(this, SignInActivity::class.java)
            startActivity(nextPage)
        }

        // declare the animation
        val middleTotop = AnimationUtils.loadAnimation(this, R.anim.middletotop)
        val middleTobottom = AnimationUtils.loadAnimation(this, R.anim.middletobottom)
        

        val waveTop = findViewById<ImageView>(R.id.imageView_wave_top)
        val waveBottom = findViewById<ImageView>(R.id.imageView_wave_bottom)

        //set the animation
        waveTop.startAnimation(middleTotop)
        waveBottom.startAnimation(middleTobottom)

        //DISPLAY USERNAME
//        val displayEmail = intent.getStringExtra("DISPLAY_EMAIL")
//        Log.e("Displayusername",displayEmail.toString())
        if(mFirebaseUser != null){
            val getString = "Welcome, $mFirebaseUser"
            userDisplay_tv.text = getString
        }else {
            val getString = "Welcome"
            userDisplay_tv.text = getString
        }




    }
}