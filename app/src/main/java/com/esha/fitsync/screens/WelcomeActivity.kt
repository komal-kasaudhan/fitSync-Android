package com.esha.fitsync.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.esha.fitsync.R
import com.google.android.material.imageview.ShapeableImageView

class WelcomeActivity : AppCompatActivity() {

    private lateinit var fitText: TextView
    private lateinit var welcome: TextView
    private lateinit var subtitle: TextView

    private lateinit var outerRing: View
    private lateinit var middleRing: View
    private lateinit var innerRing: View

    private lateinit var hologram: ShapeableImageView
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)



        fitText = findViewById(R.id.fitText)
        welcome = findViewById(R.id.wlc)
        subtitle = findViewById(R.id.wlcsubtitle)

        outerRing = findViewById(R.id.ringOuter)
        middleRing = findViewById(R.id.ringMiddle)
        innerRing = findViewById(R.id.ringInner)

        hologram = findViewById(R.id.hologramImage)

        startButton = findViewById(R.id.startBtn)



        fitText.alpha = 0f

        welcome.alpha = 0f
        welcome.translationY = 80f

        subtitle.alpha = 0f

        hologram.alpha = 0f
        hologram.scaleX = 0.8f
        hologram.scaleY = 0.8f

        startButton.alpha = 0f
        startButton.translationY = 120f


        fitText.animate()
            .alpha(1f)
            .setDuration(700)
            .start()

        welcome.postDelayed({

            welcome.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .start()

        }, 300)

        subtitle.postDelayed({

            subtitle.animate()
                .alpha(1f)
                .setDuration(600)
                .start()

        }, 700)

        hologram.postDelayed({

            hologram.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(700)
                .start()

        }, 1000)

        startButton.postDelayed({

            startButton.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .start()

        }, 1400)


        startRingAnimation()
        startFloatingAnimation()
        startButtonPulse()



        startButton.setOnClickListener {

            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {

                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()

                    startActivity(
                        Intent(
                            this@WelcomeActivity,
                            OnboardingActivity::class.java
                        )
                    )

                    finish()
                }
                .start()
        }

    }



    private fun startRingAnimation() {

        val outer = ObjectAnimator.ofFloat(
            outerRing,
            View.ROTATION,
            0f,
            360f
        )

        outer.duration = 9000
        outer.repeatCount = ValueAnimator.INFINITE
        outer.interpolator = LinearInterpolator()

        val middle = ObjectAnimator.ofFloat(
            middleRing,
            View.ROTATION,
            360f,
            0f
        )

        middle.duration = 7000
        middle.repeatCount = ValueAnimator.INFINITE
        middle.interpolator = LinearInterpolator()

        val inner = ObjectAnimator.ofFloat(
            innerRing,
            View.ROTATION,
            0f,
            360f
        )

        inner.duration = 5000
        inner.repeatCount = ValueAnimator.INFINITE
        inner.interpolator = LinearInterpolator()

        outer.start()
        middle.start()
        inner.start()

    }


    private fun startFloatingAnimation() {

        val floatAnim = ObjectAnimator.ofFloat(
            hologram,
            View.TRANSLATION_Y,
            -20f,
            20f
        )

        floatAnim.duration = 2500
        floatAnim.repeatMode = ValueAnimator.REVERSE
        floatAnim.repeatCount = ValueAnimator.INFINITE
        floatAnim.interpolator = AccelerateDecelerateInterpolator()

        floatAnim.start()

    }



    private fun startButtonPulse() {

        val scaleX = ObjectAnimator.ofFloat(
            startButton,
            View.SCALE_X,
            1f,
            1.05f
        )

        val scaleY = ObjectAnimator.ofFloat(
            startButton,
            View.SCALE_Y,
            1f,
            1.05f
        )

        scaleX.duration = 1200
        scaleY.duration = 1200

        scaleX.repeatCount = ValueAnimator.INFINITE
        scaleY.repeatCount = ValueAnimator.INFINITE

        scaleX.repeatMode = ValueAnimator.REVERSE
        scaleY.repeatMode = ValueAnimator.REVERSE

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }

    }

}