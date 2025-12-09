package com.example.myapplication.ui.main

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rootView = findViewById<View>(R.id.root_layout)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        setupBottomNavigation(rootView, bottomNavigation)
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }


        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_heart -> {
                    loadFragment(PreparingFragment())
                    true
                }
                R.id.nav_piggy_bank -> {
                    loadFragment(PreparingFragment())
                    true
                }
                R.id.nav_chart -> {
                    loadFragment(PreparingFragment())
                    true
                }
                R.id.nav_menu -> {
                    loadFragment(PreparingFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBottomNavigation(rootView: View, bottomNav: BottomNavigationView) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            val params = bottomNav.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = navBarHeight
            bottomNav.layoutParams = params

            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { _, insets ->
            insets
        }

        bottomNav.post {
            bottomNav.setPadding(
                bottomNav.paddingLeft,
                0,
                bottomNav.paddingRight,
                0
            )
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fast_fade_in,
                R.anim.fast_fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}