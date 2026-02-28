package com.example.factory_tracking;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.factory_tracking.api.ApiModels;
import com.example.factory_tracking.api.RetrofitClient;
import com.example.factory_tracking.api.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScanActivity extends AppCompatActivity {

    private TextView tvScanStatus;
    private EditText etScanInput;
    private String pendingStationId;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        session = new SessionManager(this);
        tvScanStatus = findViewById(R.id.tvScanStatus);
        etScanInput = findViewById(R.id.etScanInput);

        findViewById(R.id.btnBackToDashboard).setOnClickListener(v -> finish());

        // Keep keyboard hidden - hardware scanner will inject text
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        etScanInput.setShowSoftInputOnFocus(false);

        etScanInput.requestFocus();
        etScanInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                onScanReceived(etScanInput.getText().toString());
                etScanInput.setText("");
                return true;
            }
            return false;
        });
        // Also handle raw text input (scanner often sends line break)
        etScanInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                onScanReceived(etScanInput.getText().toString());
                etScanInput.setText("");
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        etScanInput.requestFocus();
    }

    private void onScanReceived(String raw) {
        if (raw == null || raw.trim().isEmpty()) return;

        String stationId = ScanHelper.extractStationId(raw);
        String operatorId = ScanHelper.extractOperatorId(raw);

        if (stationId != null) {
            pendingStationId = stationId;
            tvScanStatus.setText("Station: " + stationId + " — Now scan operator QR");
            etScanInput.setText("");
            return;
        }

        if (operatorId != null && pendingStationId != null) {
            assignOperator(pendingStationId, operatorId);
            pendingStationId = null;
            tvScanStatus.setText("Scan station first...");
            etScanInput.setText("");
            return;
        }

        // Try raw as station or operator if no prefix
        if (pendingStationId == null && raw.matches("^[A-Za-z0-9\\-]+$")) {
            pendingStationId = raw.trim();
            tvScanStatus.setText("Station: " + pendingStationId + " — Now scan operator QR");
            etScanInput.setText("");
            return;
        }
        if (pendingStationId != null && raw.matches("^[A-Za-z0-9\\-]+$")) {
            assignOperator(pendingStationId, raw.trim());
            pendingStationId = null;
            tvScanStatus.setText("Scan station first...");
            etScanInput.setText("");
            return;
        }

        tvScanStatus.setText("Unknown format. Scan \"STATION ID: xxx\" then \"OPERATOR ID: xxx\"");
        etScanInput.setText("");
    }

    private void assignOperator(String stationId, String operatorId) {
        ApiModels.AssignRequest req = new ApiModels.AssignRequest();
        req.stationId = stationId;
        req.operatorId = operatorId;
        req.supervisorId = session.getSupervisorId();
        req.shift = session.getShift();
        req.sessionId = session.getSessionId() > 0 ? session.getSessionId() : null;

        RetrofitClient.getApi().assign(req).enqueue(new Callback<ApiModels.AssignResponse>() {
            @Override
            public void onResponse(Call<ApiModels.AssignResponse> call, Response<ApiModels.AssignResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    Toast.makeText(ScanActivity.this, "Assigned " + operatorId + " to " + stationId, Toast.LENGTH_SHORT).show();
                    tvScanStatus.setText("Assigned. Scan next station then operator.");
                } else {
                    Toast.makeText(ScanActivity.this, "Assign failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiModels.AssignResponse> call, Throwable t) {
                Toast.makeText(ScanActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
