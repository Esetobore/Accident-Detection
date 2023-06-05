package com.example.accidentdetection.LocationAndMaps

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class Vals {

    companion object{
        public var lati : Double=0.0
        public var longi : Double=0.0

        var auth = FirebaseAuth.getInstance()
        var currUser = auth.currentUser!!
    }
    object Utils {
        fun showToastLong(mContext: Context?, message: String?) {
            Toast.makeText(mContext, message, Toast.LENGTH_LONG).show()
        }
        fun showToastShort(mContext: Context?, message: String?) {
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show()
        }
    }

}