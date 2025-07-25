// HomeActivity.kt
package com.example.onfit.HomeRegister

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.onfit.HomeRegister.fragment.HomeRegisterFragment
import com.example.onfit.R

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 첫 화면으로 RegisterStartFragment 표시
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeRegisterFragment())
            .commit()
    }
}
