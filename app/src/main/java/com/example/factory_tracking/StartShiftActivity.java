package com.example.factory_tracking;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.factory_tracking.api.ApiModels;
import com.example.factory_tracking.api.RetrofitClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StartShiftActivity extends AppCompatActivity {

    private EditText etShift;
    private TimePicker timeStart, timeEnd;
    private TextView tvSelectedLines;
    private Button btnSelectLines;
    private SessionManager session;
    private List<String> allLines = new ArrayList<>();
    private List<String> selectedLines = new ArrayList<>();
    private boolean[] checkedItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_shift);

        session = new SessionManager(this);
        etShift = findViewById(R.id.etShift);
        timeStart = findViewById(R.id.timeStart);
        timeEnd = findViewById(R.id.timeEnd);
        tvSelectedLines = findViewById(R.id.tvSelectedLines);
        btnSelectLines = findViewById(R.id.btnSelectLines);
        Button btnStartShift = findViewById(R.id.btnStartShift);

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            session.logout();
            startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        loadLines();

        btnSelectLines.setOnClickListener(v -> showLinesDialog());

        Calendar cal = Calendar.getInstance();
        timeStart.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
        timeStart.setCurrentMinute(cal.get(Calendar.MINUTE));
        timeEnd.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY) + 4);
        timeEnd.setCurrentMinute(cal.get(Calendar.MINUTE));

        btnStartShift.setOnClickListener(v -> startShift());
    }

    private void loadLines() {
        RetrofitClient.getApi().getLines().enqueue(new Callback<ApiModels.LinesResponse>() {
            @Override
            public void onResponse(Call<ApiModels.LinesResponse> call, Response<ApiModels.LinesResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    allLines = response.body().lines;
                    checkedItems = new boolean[allLines.size()];
                }
            }
            @Override
            public void onFailure(Call<ApiModels.LinesResponse> call, Throwable t) {
                Toast.makeText(StartShiftActivity.this, "Error loading lines", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLinesDialog() {
        if (allLines.isEmpty()) {
            Toast.makeText(this, "No lines loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] linesArray = allLines.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Lines");
        builder.setMultiChoiceItems(linesArray, checkedItems, (dialog, which, isChecked) -> {
            checkedItems[which] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            selectedLines.clear();
            for (int i = 0; i < checkedItems.length; i++) {
                if (checkedItems[i]) {
                    selectedLines.add(allLines.get(i));
                }
            }
            tvSelectedLines.setText(TextUtils.join(", ", selectedLines));
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void startShift() {
        String shiftName = etShift.getText().toString().trim();
        if (shiftName.isEmpty()) shiftName = "SHIFT";
        final String shift = shiftName;

        if (selectedLines.isEmpty()) {
            Toast.makeText(this, "Please select at least one line", Toast.LENGTH_SHORT).show();
            return;
        }

        String linesString = TextUtils.join(",", selectedLines);

        int eh = timeEnd.getCurrentHour();
        int em = timeEnd.getCurrentMinute();
        final String endTime = String.format(Locale.US, "%04d-%02d-%02d %02d:%02d:00",
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH) + 1,
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                eh, em);

        ApiModels.StartShiftRequest req = new ApiModels.StartShiftRequest();
        req.supervisorId = session.getSupervisorId();
        req.lineId = linesString; // Sending comma-separated lines
        req.shift = shift;
        req.endTime = endTime;

        RetrofitClient.getApi().startShift(req).enqueue(new Callback<ApiModels.StartShiftResponse>() {
            @Override
            public void onResponse(Call<ApiModels.StartShiftResponse> call, Response<ApiModels.StartShiftResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    session.saveShiftSession(response.body().sessionId, shift, endTime, linesString);
                    Toast.makeText(StartShiftActivity.this, "Shift started", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(StartShiftActivity.this, DashboardActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    finish();
                } else {
                    Toast.makeText(StartShiftActivity.this, "Failed to start shift", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiModels.StartShiftResponse> call, Throwable t) {
                Toast.makeText(StartShiftActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
