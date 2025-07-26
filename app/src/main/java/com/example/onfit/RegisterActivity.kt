package com.example.onfit

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.onfit.HomeRegister.fragment.RegisterFragment
import com.example.onfit.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 처음 실행 시에만 프래그먼트 추가
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.register_container, RegisterFragment())
                .commit()
        }
    }
}