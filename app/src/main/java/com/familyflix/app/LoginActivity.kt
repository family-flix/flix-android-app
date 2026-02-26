package com.familyflix.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.familyflix.app.databinding.ActivityLoginBinding
import com.familyflix.app.network.RetrofitClient
import com.familyflix.app.network.TokenManager
import kotlinx.coroutines.launch

import android.view.View

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        TokenManager.init(applicationContext)

        // Check if already logged in
        if (TokenManager.getToken() != null) {
            startMainActivity()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val token = "b571efc6c3710a4" // Hardcoded token query
        val tmp = 1

        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Logging in..."
        binding.tvError.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting login with token: $token")
                val request = com.familyflix.app.model.LoginRequest(token, tmp)
                val response = RetrofitClient.apiService.loginWithTokenId(request)
                
                if (response.code == 0) {
                    val authToken = response.data.token
                    TokenManager.saveToken(authToken)
                    Log.d(TAG, "Login Success, Token: $authToken")
                    Toast.makeText(this@LoginActivity, "Login Success", Toast.LENGTH_SHORT).show()
                    startMainActivity()
                } else {
                    val errorMsg = "Login Failed: ${response.msg}"
                    Log.e(TAG, errorMsg)
                    showError(errorMsg)
                    resetButton()
                }
            } catch (e: Exception) {
                val errorMsg = "Login Error: ${e.message}"
                Log.e(TAG, errorMsg, e)
                showError(errorMsg)
                e.printStackTrace()
                resetButton()
            }
        }
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun resetButton() {
        binding.btnLogin.isEnabled = true
        binding.btnLogin.text = "Login"
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
