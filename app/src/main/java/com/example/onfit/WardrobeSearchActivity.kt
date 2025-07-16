package com.example.onfit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class WardrobeSearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wardrobe_search)

        // WardrobeSearchFragment를 이 Activity에 넣기
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_wardrobe_search, WardrobeSearchFragment())
                .commit()
        }
    }
}