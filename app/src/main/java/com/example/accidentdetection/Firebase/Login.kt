package com.example.accidentdetection.Firebase

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy.LOG
import com.example.accidentdetection.LocationAndMaps.Vals.Companion.auth
import com.example.accidentdetection.LocationAndMaps.Vals.Utils.checkEmpty
import com.example.accidentdetection.LocationAndMaps.Vals.Utils.showToastLong
import com.example.accidentdetection.LocationAndMaps.Vals.Utils.showToastShort
import com.example.accidentdetection.MainActivity
import com.example.accidentdetection.R
import kotlinx.android.synthetic.main.activity_login.*

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btn_login.setOnClickListener {
            loginUser()
        }
        textView_register.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }
        textView_forgotPass.setOnClickListener {
            startActivity(Intent(this, ResetPassword::class.java))

        }
    }


    private fun loginUser() {
        val email = txt_email_login.text.toString().trim()
        val password = txt_password_login.text.toString().trim()

        if (email.isEmpty()){
            txt_email_login.error = "Enter email address"
            return
        }
        if (password.isEmpty()){
            txt_password_login.error = "Enter Password"
            return
        }
        auth.signInWithEmailAndPassword(email,password)
            .addOnSuccessListener {
                showToastShort(this,"Login Successful!")
                startActivity(Intent(this, MainActivity::class.java))
            }
            .addOnFailureListener {error->
                showToastLong(this, "Login Failed kindly check details provided")
                Log.e("Error", "Message:$error")
            }

    }

}