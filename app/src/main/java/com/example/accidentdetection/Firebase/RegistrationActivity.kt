package com.example.accidentdetection.Firebase

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.accidentdetection.Utils.Vals.Companion.authFirebase
import com.example.accidentdetection.Utils.Vals.Companion.currUser
import com.example.accidentdetection.Utils.Vals.Utils.checkEmpty
import com.example.accidentdetection.Utils.Vals.Utils.showToastShort
import com.example.accidentdetection.R
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_register.*

class RegistrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        btn_register.setOnClickListener {
            register()
        }
        btn_login.setOnClickListener {
            startActivity(Intent(this,LoginActivity::class.java))
            finish()
        }

    }



    private fun register(){
        val fullName = txt_name.text.toString().trim()
        val email = txt_email.text.toString().trim()
        val password = txt_password.text.toString().trim()
        val confirmPassword = txt_confirm_password.text.toString().trim()
        val phoneNumber = txt_phone.text.toString().trim()


        if ( checkEmpty(email,txt_email) || checkEmpty(password, txt_password) || checkEmpty(confirmPassword, txt_confirm_password)
        ) return
//        if (phoneNumber.length == 11) {
//            txt_phone.error = "Invalid Number"
//            return
//        }
        if (password.length <= 6) {
            txt_password.error = "Password is too short"
            return
        }
        if (password != confirmPassword) {
            txt_confirm_password.error = "Password don't match"
            return
        }


        authFirebase.createUserWithEmailAndPassword(email,password).addOnCompleteListener {
            if (it.isSuccessful){
                val user = authFirebase.currentUser!!
                user.sendEmailVerification().addOnSuccessListener {
                    showToastShort(this,"Email verification mail has been sent")
                }.addOnFailureListener {failure->
                    showToastShort(this,"Email verification Failed $failure")
                }

                startActivity(Intent(this, UserDetails::class.java))
            } else{
                showToastShort(this,"User Not Registered")
            }
        }
    }

}