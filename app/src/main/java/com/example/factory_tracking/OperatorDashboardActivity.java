package com.example.factory_tracking;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.factory_tracking.api.ApiModels;
import com.example.factory_tracking.api.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OperatorDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private LinearLayout historyContainer;
    private EditText etStepName;
    private Button btnLogStep;
    private String operatorId;
    private String currentStationId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_dashboard);
        RetrofitClient.init(this);

        tvWelcome = findViewById(R.id.tvOperatorWelcome);
        historyContainer = findViewById(R.id.historyContainer);
        etStepName = findViewById(R.id.etStepName);
        btnLogStep = findViewById(R.id.btnLogStep);

        operatorId = getIntent().getStringExtra("operator_id");
        String operatorName = getIntent().getStringExtra("name");

        tvWelcome.setText("Welcome " + (operatorName != null ? operatorName : ""));

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, LoginActivity.class).setFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        btnLogStep.setOnClickListener(v -> logProcessStep());

        loadWorkHistory();
    }

    private void loadWorkHistory() {
        ApiModels.OperatorHistoryRequest req = new ApiModels.OperatorHistoryRequest();
        req.operatorId = operatorId;

        RetrofitClient.getApi().operatorHistory(req).enqueue(new Callback<ApiModels.OperatorHistoryResponse>() {
            @Override
            public void onResponse(Call<ApiModels.OperatorHistoryResponse> call,
                    Response<ApiModels.OperatorHistoryResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    historyContainer.removeAllViews();
                    java.util.List<ApiModels.OperatorHistoryRecord> history = response.body().history;
                    if (history == null || history.isEmpty()) {
                        TextView empty = new TextView(OperatorDashboardActivity.this);
                        empty.setText("No work history found.");
                        empty.setTextSize(18);
                        empty.setPadding(30, 30, 30, 30);
                        historyContainer.addView(empty);
                    } else {
                        currentStationId = history.get(0).stationId;
                        for (ApiModels.OperatorHistoryRecord r : history) {
                            addHistoryRecord(r.stationId, r.date, r.shift, r.status);
                        }
                    }
                } else {
                    Toast.makeText(OperatorDashboardActivity.this, "No history available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiModels.OperatorHistoryResponse> call, Throwable t) {
                Toast.makeText(OperatorDashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addHistoryRecord(String stationId, String date, String shift, String status) {
        TextView tv = new TextView(this);
        tv.setText("Station: " + stationId + "\nDate: " + date + "  Shift: " + (shift != null ? shift : "N/A"));
        tv.setTextSize(16);
        tv.setPadding(30, 30, 30, 30);
        tv.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 20, 0, 0);
        tv.setLayoutParams(params);
        historyContainer.addView(tv);
    }

    private void logProcessStep() {
        if (currentStationId == null) {
            Toast.makeText(this, "No active station found in history.", Toast.LENGTH_SHORT).show();
            return;
        }
        String stepName = etStepName.getText().toString().trim();
        if (stepName.isEmpty()) {
            Toast.makeText(this, "Please enter a step name", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiModels.ProcessCompletionRequest req = new ApiModels.ProcessCompletionRequest();
        req.operatorId = operatorId;
        req.stationId = currentStationId;
        req.stepName = stepName;

        RetrofitClient.getApi().logProcessCompletion(req).enqueue(new Callback<ApiModels.SimpleResponse>() {
            @Override
            public void onResponse(Call<ApiModels.SimpleResponse> call, Response<ApiModels.SimpleResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    Toast.makeText(OperatorDashboardActivity.this, "Step logged successfully!", Toast.LENGTH_SHORT)
                            .show();
                    etStepName.setText(""); // clear the input
                } else {
                    Toast.makeText(OperatorDashboardActivity.this, "Failed to log step.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiModels.SimpleResponse> call, Throwable t) {
                Toast.makeText(OperatorDashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
