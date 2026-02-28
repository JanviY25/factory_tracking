package com.example.factory_tracking;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.factory_tracking.api.ApiModels;
import com.example.factory_tracking.api.RetrofitClient;
import com.example.factory_tracking.api.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupervisorsActivity extends AppCompatActivity {

    private RecyclerView recyclerSupervisors;
    private SupervisorsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supervisors);

        recyclerSupervisors = findViewById(R.id.recyclerSupervisors);
        adapter = new SupervisorsAdapter(this::deleteSupervisor);
        recyclerSupervisors.setLayoutManager(new LinearLayoutManager(this));
        recyclerSupervisors.setAdapter(adapter);

        loadSupervisors();

        findViewById(R.id.btnAddSupervisor).setOnClickListener(v -> showAddDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSupervisors();
    }

    private void loadSupervisors() {
        RetrofitClient.getApi().getSupervisors().enqueue(new Callback<ApiModels.SupervisorsListResponse>() {
            @Override
            public void onResponse(Call<ApiModels.SupervisorsListResponse> call, Response<ApiModels.SupervisorsListResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    adapter.setList(response.body().list);
                }
            }

            @Override
            public void onFailure(Call<ApiModels.SupervisorsListResponse> call, Throwable t) {
                Toast.makeText(SupervisorsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAddDialog() {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        final EditText etId = new EditText(this);
        etId.setHint("Supervisor ID");
        final EditText etName = new EditText(this);
        etName.setHint("Name");
        final EditText etPassword = new EditText(this);
        etPassword.setHint("Password");
        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        final EditText etLine = new EditText(this);
        etLine.setHint("Line (e.g. LINE1)");
        layout.addView(etId);
        layout.addView(etName);
        layout.addView(etPassword);
        layout.addView(etLine);
        b.setTitle("Add Supervisor");
        b.setView(layout);
        b.setPositiveButton("Add", (dialog, which) -> {
            String id = etId.getText().toString().trim();
            String name = etName.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String line = etLine.getText().toString().trim();
            if (id.isEmpty() || password.isEmpty() || line.isEmpty()) {
                Toast.makeText(SupervisorsActivity.this, "ID, password and line required", Toast.LENGTH_SHORT).show();
                return;
            }
            ApiModels.AddSupervisorRequest req = new ApiModels.AddSupervisorRequest();
            req.supervisorId = id;
            req.name = name.isEmpty() ? id : name;
            req.password = password;
            req.line = line;
            RetrofitClient.getApi().addSupervisor(req).enqueue(new Callback<ApiModels.AssignResponse>() {
                @Override
                public void onResponse(Call<ApiModels.AssignResponse> call, Response<ApiModels.AssignResponse> response) {
                    if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                        Toast.makeText(SupervisorsActivity.this, "Added", Toast.LENGTH_SHORT).show();
                        loadSupervisors();
                    } else {
                        Toast.makeText(SupervisorsActivity.this, "Add failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiModels.AssignResponse> call, Throwable t) {
                    Toast.makeText(SupervisorsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void deleteSupervisor(ApiModels.SupervisorItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete supervisor?")
                .setMessage(item.supervisorId + " - " + item.name)
                .setPositiveButton("Delete", (dialog, which) -> {
                    RetrofitClient.getApi().deleteSupervisor(item.supervisorId).enqueue(new Callback<ApiModels.AssignResponse>() {
                        @Override
                        public void onResponse(Call<ApiModels.AssignResponse> call, Response<ApiModels.AssignResponse> response) {
                            if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                                Toast.makeText(SupervisorsActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                                loadSupervisors();
                            } else {
                                Toast.makeText(SupervisorsActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiModels.AssignResponse> call, Throwable t) {
                            Toast.makeText(SupervisorsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
