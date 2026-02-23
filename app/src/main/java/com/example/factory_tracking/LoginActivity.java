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

public class LoginActivity extends AppCompatActivity {

    EditText etUserId, etPassword;
    Button btnLogin;

    // Backend login URL
    String LOGIN_URL = "http://192.168.1.4:3000/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUserId = findViewById(R.id.etUserId);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {

        String userId = etUserId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if(userId.isEmpty() || password.isEmpty()){
            Toast.makeText(LoginActivity.this,
                    "Enter UserID and Password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest stringRequest =
                new StringRequest(Request.Method.POST, LOGIN_URL,

                        response -> {

                            try {

                                JSONObject jsonObject = new JSONObject(response);

                                String status = jsonObject.getString("status");

                                if(status.equals("success")) {

                                    // Extract supervisor details
                                    String supervisorId =
                                            jsonObject.getString("supervisor_id");

                                    String name =
                                            jsonObject.getString("name");

                                    String line =
                                            jsonObject.getString("line");

                                    Toast.makeText(LoginActivity.this,
                                            "Login Successful",
                                            Toast.LENGTH_SHORT).show();

                                    // Send data to DashboardActivity
                                    Intent intent =
                                            new Intent(LoginActivity.this,
                                                    DashboardActivity.class);

                                    intent.putExtra("supervisor_id",
                                            supervisorId);

                                    intent.putExtra("name",
                                            name);

                                    intent.putExtra("line",
                                            line);

                                    startActivity(intent);

                                    finish();

                                }
                                else {

                                    Toast.makeText(LoginActivity.this,
                                            "Invalid Credentials",
                                            Toast.LENGTH_SHORT).show();

                                }

                            }
                            catch (JSONException e) {

                                Toast.makeText(LoginActivity.this,
                                        "JSON Error: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();

                            }

                        },

                        error -> Toast.makeText(LoginActivity.this,
                                "Server Error: " + error.toString(),
                                Toast.LENGTH_LONG).show()

                ){

                    @Override
                    protected Map<String, String> getParams(){

                        Map<String,String> params =
                                new HashMap<>();

                        params.put("userId", userId);
                        params.put("password", password);

                        return params;
                    }

                };

        RequestQueue queue =
                Volley.newRequestQueue(LoginActivity.this);

        queue.add(stringRequest);

    }
}