package com.example.factory_tracking;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.factory_tracking.api.ApiModels;
import com.example.factory_tracking.api.RetrofitClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final int POLLING_INTERVAL_MS = 3000;

    private RecyclerView recyclerAllStations;
    private AdminStationsAdapter adapter;
    private SessionManager session;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;

    private String selectedStartDate = "";
    private String selectedEndDate = "";
    private Button btnStartDate, btnEndDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        session = new SessionManager(this);
        recyclerAllStations = findViewById(R.id.recyclerAllStations);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);

        adapter = new AdminStationsAdapter(this::confirmRemoveLine);
        recyclerAllStations.setLayoutManager(new LinearLayoutManager(this));
        recyclerAllStations.setAdapter(adapter);

        loadAllLines();

        findViewById(R.id.btnSupervisors)
                .setOnClickListener(v -> startActivity(new Intent(this, SupervisorsActivity.class)));
        findViewById(R.id.btnAddLine).setOnClickListener(v -> showAddLineDialog());

        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));

        findViewById(R.id.btnExportCsv).setOnClickListener(v -> downloadCsvReport());

        findViewById(R.id.btnIdleTimeReport).setOnClickListener(v -> {
            startActivity(new Intent(this, IdleTimeReportActivity.class));
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            session.logout();
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        pollingRunnable = () -> {
            loadAllLines();
            handler.postDelayed(pollingRunnable, POLLING_INTERVAL_MS);
        };
        handler.postDelayed(pollingRunnable, POLLING_INTERVAL_MS);
    }

    private void confirmRemoveLine(String lineName) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Line?")
                .setMessage("This will delete " + lineName + " and all its 70 stations. This cannot be undone.")
                .setPositiveButton("Remove", (dialog, which) -> deleteLine(lineName))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteLine(String name) {
        // We will reuse addLine endpoint logic on backend to handle deletion or add a
        // delete endpoint
        // For now, let's assume a deleteLine endpoint exists or add it
        Map<String, Object> body = new HashMap<>();
        body.put("lineName", name);

        RetrofitClient.getApi().removeLine(body).enqueue(new Callback<ApiModels.AssignResponse>() {
            @Override
            public void onResponse(Call<ApiModels.AssignResponse> call, Response<ApiModels.AssignResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    Toast.makeText(AdminDashboardActivity.this, "Line removed", Toast.LENGTH_SHORT).show();
                    loadAllLines();
                }
            }

            @Override
            public void onFailure(Call<ApiModels.AssignResponse> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Error removing line", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddLineDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Production Line");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_line, null);
        EditText etLineName = view.findViewById(R.id.etLineName);
        EditText etStationCount = view.findViewById(R.id.etStationCount);

        builder.setView(view);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = etLineName.getText().toString().trim();
            String countStr = etStationCount.getText().toString().trim();
            if (!name.isEmpty() && !countStr.isEmpty()) {
                addNewLine(name, Integer.parseInt(countStr));
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addNewLine(String name, int count) {
        Map<String, Object> body = new HashMap<>();
        body.put("lineName", name);
        body.put("stationCount", count);

        RetrofitClient.getApi().addLine(body).enqueue(new Callback<ApiModels.AssignResponse>() {
            @Override
            public void onResponse(Call<ApiModels.AssignResponse> call, Response<ApiModels.AssignResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    Toast.makeText(AdminDashboardActivity.this, "Line added successfully!", Toast.LENGTH_SHORT).show();
                    loadAllLines();
                }
            }

            @Override
            public void onFailure(Call<ApiModels.AssignResponse> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Error adding line", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            if (isStart) {
                selectedStartDate = date;
                btnStartDate.setText("From: " + date);
            } else {
                selectedEndDate = date;
                btnEndDate.setText("To: " + date);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadAllLines() {
        RetrofitClient.getApi().getAdminLines().enqueue(new Callback<ApiModels.GetStationsResponse>() {
            @Override
            public void onResponse(Call<ApiModels.GetStationsResponse> call,
                    Response<ApiModels.GetStationsResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    List<ApiModels.StationItem> list = response.body().stations;
                    adapter.setStations(list != null ? list : new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiModels.GetStationsResponse> call, Throwable t) {
            }
        });
    }

    private void downloadCsvReport() {
        if (selectedStartDate.isEmpty() || selectedEndDate.isEmpty()) {
            Toast.makeText(this, "Please select both dates", Toast.LENGTH_SHORT).show();
            return;
        }

        RetrofitClient.getApi().exportCsvRange(selectedStartDate, selectedEndDate)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            saveAndOpenCsv(response.body(),
                                    "report_" + selectedStartDate + "_to_" + selectedEndDate + ".csv");
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                    }
                });
    }

    private void saveAndOpenCsv(ResponseBody body, String filename) {
        try {
            File dir = getExternalFilesDir(null);
            File file = new File(dir, filename);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(body.bytes());
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "text/csv");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open Report"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(pollingRunnable);
    }
}
