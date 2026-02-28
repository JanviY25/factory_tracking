package com.example.factory_tracking;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.factory_tracking.api.ApiModels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminStationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnRemoveLineListener {
        void onRemove(String lineName);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_STATION = 1;

    private final List<Object> items = new ArrayList<>();
    private final Map<String, String> lineSupervisorMap = new HashMap<>();
    private final OnRemoveLineListener onRemoveLine;

    public AdminStationsAdapter(OnRemoveLineListener onRemoveLine) {
        this.onRemoveLine = onRemoveLine;
    }

    public void setStations(List<ApiModels.StationItem> stations) {
        items.clear();
        lineSupervisorMap.clear();
        if (stations == null || stations.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // Extract supervisor info from the station items (passed from backend)
        for (ApiModels.StationItem s : stations) {
            if (s.line != null && s.inCharge != null) {
                lineSupervisorMap.put(s.line, s.inCharge);
            }
        }

        stations.sort((a, b) -> {
            int c = (a.line == null ? "" : a.line).compareTo(b.line == null ? "" : b.line);
            if (c != 0) return c;
            return (a.stationId == null ? "" : a.stationId).compareTo(b.stationId == null ? "" : b.stationId);
        });

        String lastLine = null;
        for (ApiModels.StationItem s : stations) {
            String line = s.line != null ? s.line : "";
            if (!line.equals(lastLine)) {
                items.add(line);
                lastLine = line;
            }
            items.add(s);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_STATION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_line_header, parent, false);
            return new HeaderHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_station, parent, false);
            return new StationHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object o = items.get(position);
        if (holder instanceof HeaderHolder) {
            String lineName = (String) o;
            String supervisor = lineSupervisorMap.getOrDefault(lineName, "N/A");
            HeaderHolder hh = (HeaderHolder) holder;
            hh.tvLineHeader.setText(lineName + " | In-Charge: " + supervisor);
            hh.btnRemoveLine.setOnClickListener(v -> onRemoveLine.onRemove(lineName));
        } else {
            ApiModels.StationItem s = (ApiModels.StationItem) o;
            StationHolder sh = (StationHolder) holder;
            sh.tvStationId.setText(s.stationId);
            
            String opDisplayName = "Vacant";
            if (s.operatorName != null && !s.operatorName.isEmpty()) {
                opDisplayName = s.operatorName;
            } else if (s.operatorId != null && !s.operatorId.isEmpty()) {
                opDisplayName = s.operatorId;
            }
            sh.tvOperatorId.setText("Operator: " + opDisplayName);

            String status = s.status != null ? s.status : "none";
            if ("green".equalsIgnoreCase(status)) {
                sh.itemView.setBackgroundColor(Color.parseColor("#A5D6A7"));
            } else if ("yellow".equalsIgnoreCase(status)) {
                sh.itemView.setBackgroundColor(Color.parseColor("#FFF59D"));
            } else if ("red".equalsIgnoreCase(status)) {
                sh.itemView.setBackgroundColor(Color.parseColor("#EF9A9A"));
            } else {
                sh.itemView.setBackgroundColor(Color.LTGRAY);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        TextView tvLineHeader;
        Button btnRemoveLine;
        HeaderHolder(View v) {
            super(v);
            tvLineHeader = v.findViewById(R.id.tvLineHeader);
            btnRemoveLine = v.findViewById(R.id.btnRemoveLine);
        }
    }

    static class StationHolder extends RecyclerView.ViewHolder {
        TextView tvStationId, tvOperatorId;
        StationHolder(View v) {
            super(v);
            tvStationId = v.findViewById(R.id.tvStationId);
            tvOperatorId = v.findViewById(R.id.tvOperatorId);
        }
    }
}
