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

public class OperatorLoginActivity extends AppCompatActivity {

    private EditText etName, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_login);
        RetrofitClient.init(this);

        etName = findViewById(R.id.etOperatorName);
        etPassword = findViewById(R.id.etOperatorPassword);

        findViewById(R.id.btnOperatorLogin).setOnClickListener(v -> loginOperator());
    }

    private void loginOperator() {
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter name and password", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiModels.OperatorLoginRequest req = new ApiModels.OperatorLoginRequest();
        req.name = name;
        req.password = password;

        RetrofitClient.getApi().operatorLogin(req).enqueue(new Callback<ApiModels.OperatorLoginResponse>() {
            @Override
            public void onResponse(Call<ApiModels.OperatorLoginResponse> call, Response<ApiModels.OperatorLoginResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    ApiModels.OperatorLoginResponse body = response.body();
                    Toast.makeText(OperatorLoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(OperatorLoginActivity.this, OperatorDashboardActivity.class);
                    intent.putExtra("operator_id", body.operatorId);
                    intent.putExtra("name", body.name);
                    startActivity(intent);
                    finish();
                } else {
                    String message = response.body() != null ? response.body().message : "Invalid credentials";
                    Toast.makeText(OperatorLoginActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiModels.OperatorLoginResponse> call, Throwable t) {
                Toast.makeText(OperatorLoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
