package com.example.accidentdetection.Utils

import android.content.Context
import android.text.TextUtils
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class Vals {

    companion object{
        public var lati : Double=0.0
        public var longi : Double=0.0

        var authFirebase = FirebaseAuth.getInstance()
        var currUser = authFirebase.currentUser?.uid
        val storageFirebase = FirebaseStorage.getInstance().getReference("profilePicUploads")
    }
    object Utils {
        fun showToastLong(mContext: Context?, message: String?) {
            Toast.makeText(mContext, message, Toast.LENGTH_LONG).show()
        }
        fun showToastShort(mContext: Context?, message: String?) {
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show()
        }
        fun checkEmpty(s: String, et: EditText): Boolean {
            if (TextUtils.isEmpty(s)) {
                et.error = "This can't be Empty"
                return true
            }
            return false
        }
    }

}