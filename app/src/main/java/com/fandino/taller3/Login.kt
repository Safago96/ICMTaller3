package com.fandino.taller3

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fandino.taller3.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Login : AppCompatActivity() {

    private lateinit var bindingLogin: ActivityLoginBinding

    private lateinit var authentication: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        bindingLogin = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(bindingLogin.root)
        authentication = Firebase.auth

        bindingLogin.LoginBtn.setOnClickListener {
            val email = bindingLogin.emailInput.text.toString()
            val password = bindingLogin.passwordInput.text.toString()
            login(email, password)
        }

        bindingLogin.signupRedirect.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        val usuarioActual = authentication.currentUser
        updateUI(usuarioActual)
    }

    private fun login(email: String, password: String) {

        if (validate() && validEmail(email)) {

            authentication.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        val user = authentication.currentUser
                        updateUI(user)
                    } else {
                        Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
        }
    }

    private fun validate(): Boolean {
        var valid = true
        val email = bindingLogin.emailInput.text.toString()
        if (TextUtils.isEmpty(email)) {
            bindingLogin.emailInput.error = "Required"
            valid = false
        } else {
            bindingLogin.emailInput.error = null
        }

        val password = bindingLogin.passwordInput.text.toString()
        if (TextUtils.isEmpty(password)) {
            bindingLogin.passwordInput.error = "Required"
            valid = false
        } else {
            bindingLogin.passwordInput.error = null
        }
        return valid
    }

    private fun validEmail(email: String): Boolean {
        return !(!email.contains("@") || !email.contains(".") || email.length < 5)
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val loginIntent = Intent(this, Map::class.java)
            loginIntent.putExtra("User", currentUser.email)
            startActivity(loginIntent)
        } else {
            bindingLogin.emailInput.setText("")
            bindingLogin.passwordInput.setText("")
        }
    }
}