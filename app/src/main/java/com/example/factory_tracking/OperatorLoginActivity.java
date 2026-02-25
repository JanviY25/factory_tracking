package com.example.factory_tracking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class OperatorLoginActivity extends AppCompatActivity {

    private EditText etName, etPassword;
    private Button btnLogin;
    private final String OPERATOR_LOGIN_URL = "http://192.168.1.4:3000/operator/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_login);

        etName = findViewById(R.id.etOperatorName);
        etPassword = findViewById(R.id.etOperatorPassword);
        btnLogin = findViewById(R.id.btnOperatorLogin);

        btnLogin.setOnClickListener(v -> loginOperator());
    }

    private void loginOperator() {
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter name and password", Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest request = new StringRequest(Request.Method.POST, OPERATOR_LOGIN_URL,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        String status = json.getString("status");

                        if (status.equals("success")) {
                            String operatorId = json.getString("operator_id");
                            String operatorName = json.getString("name");

                            Toast.makeText(OperatorLoginActivity.this,
                                    "Login successful", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(OperatorLoginActivity.this,
                                    OperatorDashboardActivity.class);
                            intent.putExtra("operator_id", operatorId);
                            intent.putExtra("name", operatorName);
                            startActivity(intent);
                            finish();
                        } else {
                            String message = json.optString("message", "Invalid credentials");
                            Toast.makeText(OperatorLoginActivity.this,
                                    message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(OperatorLoginActivity.this,
                                "JSON error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(OperatorLoginActivity.this,
                        "Server error: " + error.getMessage(), Toast.LENGTH_LONG).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("name", name);
                params.put("password", password);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}
