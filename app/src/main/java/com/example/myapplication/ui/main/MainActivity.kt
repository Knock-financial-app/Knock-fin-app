package com.example.myapplication.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)

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