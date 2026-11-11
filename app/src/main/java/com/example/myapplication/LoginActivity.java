package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.myapplication.database.DatabaseHelper;
import com.example.myapplication.model.User;
import com.example.myapplication.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvGoToRegister;

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Si l'utilisateur est déjà connecté, redirection vers MainActivity
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        databaseHelper = new DatabaseHelper(this);

        initViews();
        setupListeners();

        // Pré-remplir si retour depuis l'inscription
        if (getIntent() != null && getIntent().hasExtra("registered_email")) {
            String registeredEmail = getIntent().getStringExtra("registered_email");
            etEmail.setText(registeredEmail);
            etPassword.requestFocus();
        }
    }

    private void initViews() {
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoToRegister = findViewById(R.id.tv_go_to_register);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> handleLogin());

        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void handleLogin() {
        // Réinitialiser les erreurs
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        // Validation des champs
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_empty_fields));
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_empty_fields));
            etPassword.requestFocus();
            return;
        }

        // Vérification dans la base de données SQLite
        User user = databaseHelper.checkUserCredentials(email, password);

        if (user != null) {
            // Connexion réussie -> Enregistrer la session
            sessionManager.createLoginSession(user.getId(), user.getEmail(), user.getFullName());

            Toast.makeText(this, "Connexion réussie ! Bienvenue " + user.getFullName() + " 👋", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, getString(R.string.error_login_failed), Toast.LENGTH_LONG).show();
            tilPassword.setError(getString(R.string.error_login_failed));
        }
    }
}
