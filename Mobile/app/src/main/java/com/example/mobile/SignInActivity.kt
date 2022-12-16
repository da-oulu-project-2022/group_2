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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.FileOutputStream

class SignInActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var loginButton: Button
    private lateinit var emailText: TextView
    private lateinit var passwordText: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        auth = Firebase.auth
        loginButton=findViewById(R.id.button_login)

        emailText=findViewById(R.id.editText_email_signin)

        passwordText=findViewById(R.id.editText_password_signin)

        onStart()
        //signOut()
        //declare the animation
        val topTomiddle = AnimationUtils.loadAnimation(this, R.anim.toptomiddle)
        val scaleTobig = AnimationUtils.loadAnimation(this, R.anim.scaletobig)

        val dropSlogan = findViewById<TextView>(R.id.textView_slogan)
        val scaleIcon = findViewById<ImageView>(R.id.ic_boxing)

        //set the animation
        dropSlogan.startAnimation(topTomiddle)
        scaleIcon.startAnimation(scaleTobig)

        //get button listener

        loginButton.setOnClickListener{
                login(emailText.text.toString(),passwordText.text.toString())
        }


        //navigation home screen
        val noNeedAccountButton = findViewById<TextView>(R.id.no_need_account_button)
        noNeedAccountButton.setOnClickListener {
            //logic for save user's selection
            val fileName: String = "DoINeedAccount.txt"
            try {
                var fout: FileOutputStream = openFileOutput(fileName, MODE_PRIVATE)
                fout.write("no".toByteArray())
                fout.close()
            } catch (ex: Exception) {
                //Toast.makeText(this, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
                Log.e("hihihaha", "Error: ${ex.message}")
            }

            val nextPage = Intent(this, HomeActivity::class.java)
            nextPage.putExtra("isNoAccount",true)
            startActivity(nextPage)

        }

        val signUp = findViewById<Button>(R.id.button_createAccount)
        signUp.setOnClickListener {
            val signUppage = Intent(this, SignUpActivity::class.java)
            startActivity(signUppage)

        }

    }
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if(currentUser != null){
            Log.e("TAG",auth.currentUser?.email.toString())
            updateUI(currentUser)
        }
    }
//     private fun signOut() {
//        // [START auth_sign_out]
//        Firebase.auth.signOut()
//        // [END auth_sign_out]
//    }

    private fun login(email:String,password:String){
        auth.signInWithEmailAndPassword(email,password )
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("TAG", "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("TAG", "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()

                }
            }
    }

    private fun updateUI(auth: FirebaseUser?) {
        val intent = Intent(this,MainActivity::class.java)
        intent.putExtra("Login",true)

        startActivity(intent)
    }

}