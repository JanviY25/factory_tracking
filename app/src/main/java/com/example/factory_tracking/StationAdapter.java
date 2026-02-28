package com.example.factory_tracking;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.factory_tracking.api.ApiModels;

import java.util.ArrayList;
import java.util.List;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.ViewHolder> {

    private final List<ApiModels.StationItem> list = new ArrayList<>();

    public void setStations(List<ApiModels.StationItem> stations) {
        list.clear();
        if (stations != null) list.addAll(stations);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_station, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApiModels.StationItem item = list.get(position);
        holder.tvStationId.setText(item.stationId);
        
        // Show Operator Name if available, otherwise show ID or "Vacant"
        String opDisplayName = "Vacant";
        if (item.operatorName != null && !item.operatorName.isEmpty()) {
            opDisplayName = item.operatorName;
        } else if (item.operatorId != null && !item.operatorId.isEmpty()) {
            opDisplayName = item.operatorId;
        }
        
        holder.tvOperatorId.setText("Operator: " + opDisplayName);
        
        String status = item.status != null ? item.status : "none";
        if ("green".equalsIgnoreCase(status)) {
            holder.itemView.setBackgroundColor(Color.parseColor("#A5D6A7"));
        } else if ("yellow".equalsIgnoreCase(status)) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFF59D"));
        } else if ("red".equalsIgnoreCase(status)) {
            holder.itemView.setBackgroundColor(Color.parseColor("#EF9A9A"));
        } else if ("maintenance".equalsIgnoreCase(status)) {
            holder.itemView.setBackgroundColor(Color.LTGRAY);
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStationId, tvOperatorId;

        ViewHolder(View itemView) {
            super(itemView);
            tvStationId = itemView.findViewById(R.id.tvStationId);
            tvOperatorId = itemView.findViewById(R.id.tvOperatorId);
        }
    }
}
