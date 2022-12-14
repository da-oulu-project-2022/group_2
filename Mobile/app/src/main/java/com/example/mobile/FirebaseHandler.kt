package com.example.mobile

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirebaseHandler {
    private val db = Firebase.firestore

        fun getCurrentUser(): FirebaseUser? {
            val mFirebaseAuth = FirebaseAuth.getInstance();

            val mFirebaseUser = mFirebaseAuth.currentUser;
            if (mFirebaseUser != null) {
                return mFirebaseUser
            } else
            {
                return null
            }
        }

    fun addDocument(collectionPath:String,training:Any) {
        db.collection("training_test").add(training)
            .addOnSuccessListener { documentReference ->
                Log.d("TAG", "DocumentSnapshot added with ID: ${documentReference.id}")

            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error adding document", e)
            }
    }




}