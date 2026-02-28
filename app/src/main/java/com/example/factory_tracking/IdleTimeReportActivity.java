package com.example.factory_tracking;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.factory_tracking.api.ApiModels;
import com.example.factory_tracking.api.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IdleTimeReportActivity extends AppCompatActivity {

    private LinearLayout reportContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idle_time_report);

        reportContainer = findViewById(R.id.reportContainer);

        fetchReportData();
    }

    private void fetchReportData() {
        RetrofitClient.getApi().getIdleTimeReport().enqueue(new Callback<ApiModels.IdleTimeResponse>() {
            @Override
            public void onResponse(Call<ApiModels.IdleTimeResponse> call,
                    Response<ApiModels.IdleTimeResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    displayReport(response.body().data);
                } else {
                    Toast.makeText(IdleTimeReportActivity.this, "Failed to load report", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiModels.IdleTimeResponse> call, Throwable t) {
                Toast.makeText(IdleTimeReportActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void displayReport(List<ApiModels.IdleTimeItem> data) {
        reportContainer.removeAllViews();
        if (data == null || data.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No shift sessions found.");
            empty.setPadding(30, 30, 30, 30);
            reportContainer.addView(empty);
            return;
        }

        for (ApiModels.IdleTimeItem item : data) {
            TextView tv = new TextView(this);
            int idleMinutes = item.totalShiftMinutes - item.totalWorkingMinutes;
            if (idleMinutes < 0)
                idleMinutes = 0; // should not happen conceptually, but to be safe

            tv.setText("Session: " + item.sessionId + " | Line: " + item.lineId + "\n" +
                    "Supervisor: " + item.supervisorId + "\n" +
                    "Shift duration: " + item.totalShiftMinutes + " mins\n" +
                    "Working time: " + item.totalWorkingMinutes + " mins\n" +
                    "Idle time: " + idleMinutes + " mins (" +
                    (item.totalShiftMinutes > 0 ? (idleMinutes * 100 / item.totalShiftMinutes) : 0) + "%)");
            tv.setTextSize(16);
            tv.setPadding(30, 30, 30, 30);
            tv.setBackgroundColor(android.graphics.Color.WHITE);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 20);
            tv.setLayoutParams(params);

            reportContainer.addView(tv);
        }
    }
}
