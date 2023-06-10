package com.example.accidentdetection.Firebase

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.accidentdetection.Utils.Vals
import com.example.accidentdetection.Utils.Vals.Utils.showToastLong
import com.example.accidentdetection.Utils.Vals.Utils.showToastShort
import com.example.accidentdetection.R
import kotlinx.android.synthetic.main.activity_reset_password.*

class ResetPassword : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        reset_btn.setOnClickListener {
            forgottenPass()
        }
        login_tv_return.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }
    }
    private fun forgottenPass() {
        val emailFor = email_editText_reset.text.toString().trim()
        Vals.authFirebase.sendPasswordResetEmail(emailFor)
            .addOnSuccessListener {
                showToastShort(this, "Reset mail sent")
            }
            .addOnFailureListener {error->
                showToastLong(this, "Error: $error")
            }
    }
}