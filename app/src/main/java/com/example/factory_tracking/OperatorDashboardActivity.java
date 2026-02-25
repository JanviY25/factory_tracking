package com.example.factory_tracking;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class OperatorDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private LinearLayout historyContainer;
    private String operatorId;
    private String operatorName;
    private final String OPERATOR_HISTORY_URL = "http://192.168.1.4:3000/operator/history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_dashboard);

        tvWelcome = findViewById(R.id.tvOperatorWelcome);
        historyContainer = findViewById(R.id.historyContainer);

        operatorId = getIntent().getStringExtra("operator_id");
        operatorName = getIntent().getStringExtra("name");

        tvWelcome.setText("Welcome " + operatorName);

        loadWorkHistory();
    }

    private void loadWorkHistory() {
        StringRequest request = new StringRequest(Request.Method.POST, OPERATOR_HISTORY_URL,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        String status = obj.getString("status");

                        if (status.equals("success")) {
                            JSONArray history = obj.getJSONArray("history");

                            if (history.length() == 0) {
                                TextView emptyView = new TextView(OperatorDashboardActivity.this);
                                emptyView.setText("No work history found.");
                                emptyView.setTextSize(18);
                                emptyView.setPadding(30, 30, 30, 30);
                                historyContainer.addView(emptyView);
                            } else {
                                for (int i = 0; i < history.length(); i++) {
                                    JSONObject record = history.getJSONObject(i);
                                    String stationId = record.getString("station_id");
                                    String date = record.getString("date");
                                    String shift = record.optString("shift", "N/A");
                                    String statusColor = record.optString("status", "none");

                                    addHistoryRecord(stationId, date, shift, statusColor);
                                }
                            }
                        } else {
                            Toast.makeText(this, "No history available", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "JSON error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(this,
                        "Server error: " + error.getMessage(),
                        Toast.LENGTH_LONG).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("operator_id", operatorId);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void addHistoryRecord(String stationId, String date, String shift, String status) {
        TextView tv = new TextView(this);
        tv.setText("Station: " + stationId + "\nDate: " + date + "  Shift: " + shift);
        tv.setTextSize(16);
        tv.setPadding(30, 30, 30, 30);
        tv.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0")); // light gray

        if (status.equalsIgnoreCase("green")) {
            tv.setBackgroundColor(android.graphics.Color.parseColor("#A5D6A7"));
        } else if (status.equalsIgnoreCase("yellow")) {
            tv.setBackgroundColor(android.graphics.Color.parseColor("#FFF59D"));
        } else if (status.equalsIgnoreCase("red")) {
            tv.setBackgroundColor(android.graphics.Color.parseColor("#EF9A9A"));
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 20, 0, 0);
        tv.setLayoutParams(params);

        historyContainer.addView(tv);
    }
}
