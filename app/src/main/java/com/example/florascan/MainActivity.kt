package com.example.florascan

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import com.example.florascan.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enable full-screen mode
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )

        // Use ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Assuming you have animation XML files named text_drop_animation and popup_animation in res/anim
        val textAnimation = AnimationUtils.loadAnimation(this, R.anim.text_drop_animation)
        val popupAnimation = AnimationUtils.loadAnimation(this, R.anim.pop_up_animation)

        // Access views using binding
        binding.linearLayout.startAnimation(textAnimation)
        binding.imageView.startAnimation(popupAnimation)
        binding.button.startAnimation(popupAnimation)

        binding.button.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(R.anim.custom_bezier_in, R.anim.custom_bezier_in)  // Apply fade-in and fade-out animation
        }

    }
}