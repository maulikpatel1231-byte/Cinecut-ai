package com.example

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

private const val TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        var app: FirebaseApp? = if (FirebaseApp.getApps(context).isNotEmpty()) {
            try {
                FirebaseApp.getInstance()
            } catch (e: Exception) {
                null
            }
        } else {
            try {
                FirebaseApp.initializeApp(context)
            } catch (e: Exception) {
                null
            }
        }

        // If google-services.json is not yet present, initialize with fallback options so default FirebaseApp is registered
        if (app == null) {
            try {
                val fallbackOptions = FirebaseOptions.Builder()
                    .setApplicationId("1:1234567890:android:c4b8e21a0")
                    .setProjectId("cinecut-ai")
                    .setApiKey("AIzaSyDummyKeyForAppInitialization000")
                    .build()
                app = FirebaseApp.initializeApp(context, fallbackOptions)
            } catch (e: Exception) {
                Log.w(TAG, "Fallback FirebaseApp initialization failed: ${e.message}")
            }
        }

        if (app != null && BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = app.persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // Synchronous commit required by App Check contract
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            try {
                FirebaseAppCheck.getInstance(app).installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "DebugAppCheckProviderFactory installed.")
            } catch (e: Exception) {
                Log.w(TAG, "Could not install DebugAppCheckProviderFactory: ${e.message}")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}
