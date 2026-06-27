package com.liquor.ledger

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.FirebaseApp
import com.liquor.ledger.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// SplashActivity is the first screen shown when the app launches
// Shows an animated logo while Firebase finishes initializing
// Once initialization is complete it navigates to LoginActivity

import android.annotation.SuppressLint

@SuppressLint("CustomSplashScreen")
class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ROOT — full screen dark background matching sidebar color
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setBackgroundColor(Color.rgb(16, 30, 55))

        // APP NAME
        val appName = TextView(this)
        appName.text = "Liquor Ledger"
        appName.textSize = 48f
        appName.setTextColor(Color.WHITE)
        appName.setTypeface(null, Typeface.BOLD)
        appName.gravity = Gravity.CENTER
        appName.letterSpacing = 0.1f

        // SUBTITLE
        val subtitle = TextView(this)
        subtitle.text = "Business Tracker"
        subtitle.textSize = 18f
        subtitle.setTextColor(Color.rgb(156, 163, 175))
        subtitle.gravity = Gravity.CENTER
        subtitle.setPadding(0, dp(12), 0, dp(48))
        subtitle.letterSpacing = 0.15f

        // LOADING TEXT
        val loadingText = TextView(this)
        loadingText.text = "Initializing..."
        loadingText.textSize = 13f
        loadingText.setTextColor(Color.rgb(107, 114, 128))
        loadingText.gravity = Gravity.CENTER
        loadingText.alpha = 0f

        root.addView(appName)
        root.addView(subtitle)
        root.addView(loadingText)

        setContentView(root)

        // Start the pulse animation then initialize Firebase
        startPulseAnimation(appName, subtitle) {
            // Animation started - Firebase initializing in background
            showLoadingText(loadingText)
            initializeFirebase {
                goToLogin()
            }
        }
    }

    // Pulse animation — logo fades in then pulses while Firebase loads
    private fun startPulseAnimation(
        appName: TextView,
        subtitle: TextView,
        onAnimationStart: () -> Unit
    ) {
        // Start invisible
        appName.alpha = 0f
        subtitle.alpha = 0f

        // Fade in app name
        val fadeInName = ObjectAnimator.ofFloat(appName, View.ALPHA, 0f, 1f)
        fadeInName.duration = 800
        fadeInName.interpolator = AccelerateDecelerateInterpolator()

        // Fade in subtitle
        val fadeInSubtitle = ObjectAnimator.ofFloat(subtitle, View.ALPHA, 0f, 1f)
        fadeInSubtitle.duration = 600
        fadeInSubtitle.interpolator = AccelerateDecelerateInterpolator()

        // Scale pulse on app name
        val scaleUpX = ObjectAnimator.ofFloat(appName, View.SCALE_X, 1f, 1.08f)
        val scaleUpY = ObjectAnimator.ofFloat(appName, View.SCALE_Y, 1f, 1.08f)
        val scaleDownX = ObjectAnimator.ofFloat(appName, View.SCALE_X, 1.08f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(appName, View.SCALE_Y, 1.08f, 1f)

        scaleUpX.duration = 500
        scaleUpY.duration = 500
        scaleDownX.duration = 500
        scaleDownY.duration = 500

        val scaleUp = AnimatorSet()
        scaleUp.playTogether(scaleUpX, scaleUpY)

        val scaleDown = AnimatorSet()
        scaleDown.playTogether(scaleDownX, scaleDownY)

        // Glow effect using alpha pulse on subtitle
        val glowUp = ObjectAnimator.ofFloat(subtitle, View.ALPHA, 0.6f, 1f)
        val glowDown = ObjectAnimator.ofFloat(subtitle, View.ALPHA, 1f, 0.6f)
        glowUp.duration = 500
        glowDown.duration = 500

        val glowPulse = AnimatorSet()
        glowPulse.playSequentially(glowUp, glowDown)

        // Chain all animations together
        val fullAnimation = AnimatorSet()
        fullAnimation.playSequentially(
            fadeInName,
            fadeInSubtitle,
            scaleUp,
            scaleDown
        )

        fullAnimation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Start continuous pulse while Firebase loads
                startContinuousPulse(appName, subtitle)
                onAnimationStart()
            }
        })

        fullAnimation.start()
    }

    // Continuous pulse animation while Firebase is initializing
    private fun startContinuousPulse(appName: TextView, subtitle: TextView) {
        val pulseUp = AnimatorSet()
        val scaleUpX = ObjectAnimator.ofFloat(appName, View.SCALE_X, 1f, 1.05f)
        val scaleUpY = ObjectAnimator.ofFloat(appName, View.SCALE_Y, 1f, 1.05f)
        val alphaUp = ObjectAnimator.ofFloat(subtitle, View.ALPHA, 0.7f, 1f)
        scaleUpX.duration = 800
        scaleUpY.duration = 800
        alphaUp.duration = 800
        pulseUp.playTogether(scaleUpX, scaleUpY, alphaUp)

        val pulseDown = AnimatorSet()
        val scaleDownX = ObjectAnimator.ofFloat(appName, View.SCALE_X, 1.05f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(appName, View.SCALE_Y, 1.05f, 1f)
        val alphaDown = ObjectAnimator.ofFloat(subtitle, View.ALPHA, 1f, 0.7f)
        scaleDownX.duration = 800
        scaleDownY.duration = 800
        alphaDown.duration = 800
        pulseDown.playTogether(scaleDownX, scaleDownY, alphaDown)

        val continuousPulse = AnimatorSet()
        continuousPulse.playSequentially(pulseUp, pulseDown)
        continuousPulse.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Keep pulsing until Firebase is ready
                continuousPulse.start()
            }
        })

        continuousPulse.start()
    }

    // Fades in the loading text
    private fun showLoadingText(loadingText: TextView) {
        val fadeIn = ObjectAnimator.ofFloat(loadingText, View.ALPHA, 0f, 1f)
        fadeIn.duration = 500
        fadeIn.start()
    }

    // Initializes Firebase and calls onReady when complete
    private fun initializeFirebase(onReady: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Make sure Firebase is initialized
                FirebaseApp.getInstance()

                // Lightweight read of firestore to ensure connection established
                FirebaseManager.db
                    .collection("employees")
                    .limit(1)
                    .get()
                    .await()

                // Small delay to animation
                kotlinx.coroutines.delay(500)

                withContext(Dispatchers.Main) {
                    onReady()
                }

            } catch (e: Exception) {
                // Even if Firebase fails still proceed to login after a short delay
                kotlinx.coroutines.delay(2000)
                withContext(Dispatchers.Main) {
                    onReady()
                }
            }
        }
    }

    // Navigates to LoginActivity with a fade out transition
    private fun goToLogin() {
        // Sign out any existing session so login is always required
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        SessionManager.clear()

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)

        // Fade out transition
        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }

    // Converts dp to pixels
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
