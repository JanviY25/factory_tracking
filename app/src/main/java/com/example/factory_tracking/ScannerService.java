package com.example.factory_tracking;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.example.factory_tracking.api.ApiModels;
import com.example.factory_tracking.api.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScannerService extends AccessibilityService {

    private StringBuilder scanBuffer = new StringBuilder();
    private String pendingStationId = null;
    private SessionManager session;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        session = new SessionManager(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not needed for key filtering
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                String data = scanBuffer.toString().trim();
                if (!data.isEmpty()) {
                    processScan(data);
                }
                scanBuffer.setLength(0);
                // We return false so the "Enter" still goes to whatever app is open
                return false; 
            } else {
                char c = (char) event.getUnicodeChar();
                if (Character.isLetterOrDigit(c) || c == '-' || c == ':' || c == ' ') {
                    scanBuffer.append(c);
                }
            }
        }
        return super.onKeyEvent(event);
    }

    private void processScan(String raw) {
        String stationId = ScanHelper.extractStationId(raw);
        String operatorId = ScanHelper.extractOperatorId(raw);

        if (stationId != null) {
            pendingStationId = stationId;
            return;
        }

        if (operatorId != null) {
            if (pendingStationId != null && session.isLoggedIn()) {
                assignOperator(pendingStationId, operatorId);
                pendingStationId = null;
            }
            return;
        }

        // Raw fallback
        if (raw.length() > 3) {
            if (pendingStationId == null) {
                pendingStationId = raw;
            } else if (session.isLoggedIn()) {
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

        RetrofitClient.getApi().assign(req).enqueue(new Callback<ApiModels.AssignResponse>() {
            @Override
            public void onResponse(Call<ApiModels.AssignResponse> call, Response<ApiModels.AssignResponse> response) {
                // Background update successful
            }
            @Override
            public void onFailure(Call<ApiModels.AssignResponse> call, Throwable t) {}
        });
    }
}
