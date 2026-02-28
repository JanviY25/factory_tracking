package com.example.factory_tracking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.factory_tracking.api.ApiModels;
import com.example.factory_tracking.api.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUserId, etPassword;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        session = new SessionManager(this);
        session.applyTheme(); 
        
        super.onCreate(savedInstanceState);
        RetrofitClient.init(this);

        if (session.isLoggedIn()) {
            goToNextActivity();
            return;
        }

        setContentView(R.layout.activity_login);

        etUserId = findViewById(R.id.etUserId);
        etPassword = findViewById(R.id.etPassword);

        findViewById(R.id.btnLogin).setOnClickListener(v -> supervisorLogin());
        findViewById(R.id.btnAdminLogin).setOnClickListener(v -> adminLogin());
        findViewById(R.id.btnOperatorLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, OperatorLoginActivity.class));
        });
    }

    private void goToNextActivity() {
        if (session.isSupervisor()) {
            if (session.getSessionId() > 0) {
                startActivity(new Intent(this, DashboardActivity.class));
            } else {
                startActivity(new Intent(this, StartShiftActivity.class));
            }
        } else if (session.isAdmin()) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
        }
        finish();
    }

    private void supervisorLogin() {
        String userId = etUserId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (userId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter User ID and Password", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiModels.LoginRequest req = new ApiModels.LoginRequest(userId, password);
        RetrofitClient.getApi().login(req).enqueue(new Callback<ApiModels.LoginResponse>() {
            @Override
            public void onResponse(Call<ApiModels.LoginResponse> call, Response<ApiModels.LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    ApiModels.LoginResponse body = response.body();
                    session.saveSupervisorSession(body.supervisorId, body.name, body.line);
                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                    goToNextActivity();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiModels.LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void adminLogin() {
        String userId = etUserId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (userId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter Admin ID and Password", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiModels.AdminLoginRequest req = new ApiModels.AdminLoginRequest();
        req.userId = userId;
        req.password = password;
        RetrofitClient.getApi().adminLogin(req).enqueue(new Callback<ApiModels.AdminLoginResponse>() {
            @Override
            public void onResponse(Call<ApiModels.AdminLoginResponse> call, Response<ApiModels.AdminLoginResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    session.saveAdminSession(response.body().adminId);
                    Toast.makeText(LoginActivity.this, "Admin login successful", Toast.LENGTH_SHORT).show();
                    goToNextActivity();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid admin credentials", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiModels.AdminLoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
