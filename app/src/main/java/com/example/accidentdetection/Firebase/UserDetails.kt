package com.example.accidentdetection.Firebase

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.accidentdetection.MainActivity
import com.example.accidentdetection.Utils.Vals.Companion.storageFirebase
import com.example.accidentdetection.R
import com.example.accidentdetection.Users
import com.example.accidentdetection.Utils.Vals.Companion.authFirebase
import com.example.accidentdetection.Utils.Vals.Companion.currUser
import com.example.accidentdetection.Utils.Vals.Utils.showToastShort
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import kotlinx.android.synthetic.main.activity_details.*
import kotlinx.android.synthetic.main.activity_register.*

@Suppress("DEPRECATION")
class UserDetails : AppCompatActivity() {
    private lateinit var selectImage : Uri
    private lateinit var firebaseStore : FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
            btn_addImage.setOnClickListener {
                addImage()
            }
        btn_saveDetails.setOnClickListener {

            saveAllDetails()
        }
    }
    private fun saveAllDetails(){
        val fullName = Name_text.text.toString().trim()
        val age = Age_text.text.toString().trim()
        val sex = Sex_text.text.toString().trim()
        val EMC = phoneNumber.text.toString().trim()

        if (fullName.isEmpty()){
            Name_text.error = "Enter Name"
            return
        }
        if (sex.length !=1){
            Sex_text.error = "Enter M or F"
            return
        }
        if (EMC.length != 11) {
            phoneNumber.error = "check your number"
        }
        val user = Users(fullName = fullName, age = age, sex = sex, EMC = EMC)

        if(currUser !=null){

            val UID = authFirebase.uid.toString()
            //Recommend use Firebase Realtime Database for storing user information this usually has a delay unlike realtimedatabase
            firebaseStore.collection("User").document(UID).set(user).addOnCompleteListener {
                if (it.isSuccessful){
                    showToastShort(this,"Information Updated")
                    startActivity(Intent(this,MainActivity::class.java))
                }
            }.addOnFailureListener { updateerror->
                showToastShort(this,"Failed to update")
                Log.e("Update Profile", "Error$updateerror")
            }
        }
        storageFirebase.putFile(selectImage).addOnFailureListener{error->
            Log.e("ProfileImg", "Notuploaded($error)")
        }
    }


    private fun addImage() {
        val imageIntent = Intent()
        imageIntent.action = Intent.ACTION_GET_CONTENT
        imageIntent.type = "image/*"
        startActivityForResult(imageIntent,10)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null){
            if (data.data != null){
                selectImage = data.data!!
                    profile_pic.setImageURI(selectImage)
            }
        }
    }


}