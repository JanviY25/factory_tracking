package com.example.factory_tracking;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    TextView tvWelcome, tvLine;
    LinearLayout stationsContainer;

    // backend API
    String STATIONS_URL = "http://192.168.1.4:3000/stations";

    String supervisorName;
    String line;

    // 🔥 POLLING VARIABLES
    private Handler handler = new Handler();
    private Runnable pollingRunnable;
    private static final int POLLING_INTERVAL = 2000; // 5 sec

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvLine = findViewById(R.id.tvLine);
        stationsContainer = findViewById(R.id.stationsContainer);

        // data from LoginActivity
        supervisorName = getIntent().getStringExtra("name");
        line = getIntent().getStringExtra("line");

        tvWelcome.setText("Welcome " + supervisorName);
        tvLine.setText("Line: " + line);

        // first load
        loadStations();

        // start auto polling
        startPolling();
    }

    // 🔥 START POLLING
    private void startPolling() {

        pollingRunnable = new Runnable() {
            @Override
            public void run() {

                loadStations(); // refresh data

                handler.postDelayed(this, POLLING_INTERVAL);
            }
        };

        handler.postDelayed(pollingRunnable, POLLING_INTERVAL);
    }

    private void loadStations() {

        StringRequest request = new StringRequest(
                Request.Method.POST,
                STATIONS_URL,

                response -> {
                    try {

                        JSONObject obj = new JSONObject(response);
                        String status = obj.getString("status");

                        if(status.equals("success")) {

                            // 🔥 CLEAR OLD VIEWS BEFORE RELOADING
                            stationsContainer.removeAllViews();

                            JSONArray stations = obj.getJSONArray("stations");

                            for(int i=0; i<stations.length(); i++) {

                                JSONObject station = stations.getJSONObject(i);

                                String stationId = station.getString("station_id");
                                String operatorId = station.optString("operator_id", "N/A");
                                String statusColor = station.optString("status","none");

                                addStationRow(stationId, operatorId, statusColor);
                            }

                        } else {

                            Toast.makeText(this,"No stations found",Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },

                error -> Toast.makeText(this,
                        "Error: "+error.toString(),
                        Toast.LENGTH_LONG).show()
        ) {

            @Override
            protected Map<String, String> getParams() {

                Map<String,String> params = new HashMap<>();
                params.put("line", line);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void addStationRow(String stationId, String operatorId, String status) {

        TextView tv = new TextView(this);

        tv.setText("Station: " + stationId +
                "\nOperator: " + operatorId);

        tv.setTextSize(18);
        tv.setPadding(30,30,30,30);

        // status color
        if(status.equalsIgnoreCase("green")) {
            tv.setBackgroundColor(Color.parseColor("#A5D6A7"));
        }
        else if(status.equalsIgnoreCase("yellow")) {
            tv.setBackgroundColor(Color.parseColor("#FFF59D"));
        }
        else if(status.equalsIgnoreCase("red")) {
            tv.setBackgroundColor(Color.parseColor("#EF9A9A"));
        }
        else {
            tv.setBackgroundColor(Color.LTGRAY);
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        params.setMargins(0,20,0,0);
        tv.setLayoutParams(params);

        stationsContainer.addView(tv);
    }

    // 🔥 STOP POLLING WHEN ACTIVITY CLOSES
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(handler != null && pollingRunnable != null) {
            handler.removeCallbacks(pollingRunnable);
        }
    }
}