package com.example.factory_tracking;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.factory_tracking.api.ApiModels;
import com.example.factory_tracking.api.RetrofitClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private static final int POLLING_INTERVAL_MS = 3000;
    private static final int EXPIRY_CHECK_INTERVAL_MS = 10000;

    private TextView tvWelcome, tvShiftInfo, tvScanStatus, tvActiveStationsCount, tvOperatorsCount;
    private RecyclerView recyclerStations;
    private StationAdapter adapter;
    private Spinner spLineSelection;
    private EditText etSearch;
    private SessionManager session;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private Runnable expiryCheckRunnable;
    
    private String currentSelectedLine;
    private List<String> supervisorLines = new ArrayList<>();
    private List<ApiModels.StationItem> fullStationList = new ArrayList<>();

    private StringBuilder scanBuffer = new StringBuilder();
    private String pendingStationId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        session = new SessionManager(this);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvShiftInfo = findViewById(R.id.tvShiftInfo);
        tvScanStatus = findViewById(R.id.tvScanStatus);
        tvActiveStationsCount = findViewById(R.id.tvActiveStationsCount);
        tvOperatorsCount = findViewById(R.id.tvOperatorsCount);
        recyclerStations = findViewById(R.id.recyclerStations);
        spLineSelection = findViewById(R.id.spLineSelection);
        etSearch = findViewById(R.id.etSearch);

        adapter = new StationAdapter();
        recyclerStations.setLayoutManager(new LinearLayoutManager(this));
        recyclerStations.setAdapter(adapter);

        tvWelcome.setText("Welcome " + session.getName());
        tvShiftInfo.setText("Shift: " + session.getShift());
        tvScanStatus.setText("Ready to scan (Station then Operator)...");

        String linesStr = session.getLine();
        if (!TextUtils.isEmpty(linesStr)) {
            supervisorLines = Arrays.asList(linesStr.split(","));
        }

        setupLineSpinner();
        setupSearch();

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            session.logout(); 
            startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        findViewById(R.id.btnEndShift).setOnClickListener(v -> endShift());

        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentSelectedLine != null) {
                    loadStations(currentSelectedLine);
                }
                handler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        };
        handler.postDelayed(pollingRunnable, POLLING_INTERVAL_MS);

        expiryCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkShiftExpiry();
                handler.postDelayed(this, EXPIRY_CHECK_INTERVAL_MS);
            }
        };
        handler.postDelayed(expiryCheckRunnable, EXPIRY_CHECK_INTERVAL_MS);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStations(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterStations(String query) {
        if (TextUtils.isEmpty(query)) {
            adapter.setStations(fullStationList);
            return;
        }
        List<ApiModels.StationItem> filtered = new ArrayList<>();
        for (ApiModels.StationItem item : fullStationList) {
            boolean matchesStation = item.stationId != null && item.stationId.toLowerCase().contains(query.toLowerCase());
            boolean matchesOperator = item.operatorId != null && item.operatorId.toLowerCase().contains(query.toLowerCase());
            if (matchesStation || matchesOperator) {
                filtered.add(item);
            }
        }
        adapter.setStations(filtered);
    }

    private void checkShiftExpiry() {
        String endTimeStr = session.getEndTime();
        if (TextUtils.isEmpty(endTimeStr)) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        try {
            Date expiryDate = sdf.parse(endTimeStr);
            if (expiryDate != null && new Date().after(expiryDate)) {
                Toast.makeText(this, "Shift time expired! Ending shift...", Toast.LENGTH_LONG).show();
                endShift();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void setupLineSpinner() {
        if (supervisorLines.isEmpty()) return;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, supervisorLines);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLineSelection.setAdapter(adapter);

        currentSelectedLine = supervisorLines.get(0);
        spLineSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSelectedLine = supervisorLines.get(position);
                loadStations(currentSelectedLine);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                String scannedData = scanBuffer.toString().trim();
                if (!scannedData.isEmpty()) {
                    processScan(scannedData);
                }
                scanBuffer.setLength(0); 
                return true;
            } else {
                char unicodeChar = (char) event.getUnicodeChar();
                if (Character.isLetterOrDigit(unicodeChar) || unicodeChar == '-' || unicodeChar == ':' || unicodeChar == ' ') {
                    scanBuffer.append(unicodeChar);
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void processScan(String raw) {
        String stationId = ScanHelper.extractStationId(raw);
        String operatorId = ScanHelper.extractOperatorId(raw);

        if (stationId != null) {
            pendingStationId = stationId;
            tvScanStatus.setText("STATION: " + stationId + " | Scan Operator QR...");
            tvScanStatus.setTextColor(android.graphics.Color.BLUE);
            return;
        }

        if (operatorId != null) {
            if (pendingStationId != null) {
                assignOperator(pendingStationId, operatorId);
                pendingStationId = null; 
            } else {
                tvScanStatus.setText("Scan STATION QR first!");
                tvScanStatus.setTextColor(android.graphics.Color.RED);
            }
            return;
        }

        if (raw.length() > 3) {
            if (pendingStationId == null) {
                pendingStationId = raw;
                tvScanStatus.setText("STATION: " + raw + " | Scan Operator QR...");
                tvScanStatus.setTextColor(android.graphics.Color.BLUE);
            } else {
                assignOperator(pendingStationId, raw);
                pendingStationId = null;
            }
        }
    }

    private void assignOperator(String sId, String oId) {
        ApiModels.AssignRequest req = new ApiModels.AssignRequest();
        req.stationId = sId;
        req.operatorId = oId;
        req.supervisorId = session.getSupervisorId();
        req.shift = session.getShift();
        req.sessionId = session.getSessionId();

        tvScanStatus.setText("Assigning " + oId + " to " + sId + "...");
        tvScanStatus.setTextColor(android.graphics.Color.BLACK);
        
        RetrofitClient.getApi().assign(req).enqueue(new Callback<ApiModels.AssignResponse>() {
            @Override
            public void onResponse(Call<ApiModels.AssignResponse> call, Response<ApiModels.AssignResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    tvScanStatus.setText("SUCCESS: " + oId + " -> " + sId);
                    tvScanStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32")); 
                    loadStations(currentSelectedLine); 
                } else {
                    String msg = response.body() != null ? response.body().message : "Assignment failed";
                    tvScanStatus.setText("FAILED: " + msg);
                    tvScanStatus.setTextColor(android.graphics.Color.RED);
                }
            }

            @Override
            public void onFailure(Call<ApiModels.AssignResponse> call, Throwable t) {
                tvScanStatus.setText("Server Error. Check Wi-Fi.");
                tvScanStatus.setTextColor(android.graphics.Color.RED);
            }
        });
    }

    private void loadStations(String line) {
        ApiModels.GetStationsRequest req = new ApiModels.GetStationsRequest(line);
        RetrofitClient.getApi().getStations(req).enqueue(new Callback<ApiModels.GetStationsResponse>() {
            @Override
            public void onResponse(Call<ApiModels.GetStationsResponse> call, Response<ApiModels.GetStationsResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    fullStationList = response.body().stations;
                    // Apply current search filter to the fresh data
                    filterStations(etSearch.getText().toString());
                    updateCounters(fullStationList);
                }
            }
            @Override
            public void onFailure(Call<ApiModels.GetStationsResponse> call, Throwable t) {}
        });
    }

    private void updateCounters(List<ApiModels.StationItem> stations) {
        if (stations == null) return;
        int activeCount = 0;
        int operatorCount = 0;
        for (ApiModels.StationItem s : stations) {
            if (s.operatorId != null && !s.operatorId.isEmpty()) {
                activeCount++;
                operatorCount++;
            }
        }
        tvActiveStationsCount.setText(String.valueOf(activeCount));
        tvOperatorsCount.setText(String.valueOf(operatorCount));
    }

    private void endShift() {
        int sessionId = session.getSessionId();
        if (sessionId <= 0) return;
        
        ApiModels.EndShiftRequest req = new ApiModels.EndShiftRequest();
        req.sessionId = sessionId;
        req.lineId = TextUtils.join(",", supervisorLines);
        
        RetrofitClient.getApi().endShift(req).enqueue(new Callback<ApiModels.EndShiftResponse>() {
            @Override
            public void onResponse(Call<ApiModels.EndShiftResponse> call, Response<ApiModels.EndShiftResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    session.clearShiftSession(); 
                    startActivity(new Intent(DashboardActivity.this, StartShiftActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    finish();
                }
            }
            @Override
            public void onFailure(Call<ApiModels.EndShiftResponse> call, Throwable t) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(pollingRunnable);
        handler.removeCallbacks(expiryCheckRunnable);
    }
}
