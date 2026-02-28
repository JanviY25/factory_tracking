package com.example.factory_tracking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.factory_tracking.api.ApiModels;

import java.util.ArrayList;
import java.util.List;

public class SupervisorsAdapter extends RecyclerView.Adapter<SupervisorsAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(ApiModels.SupervisorItem item);
    }

    private final List<ApiModels.SupervisorItem> list = new ArrayList<>();
    private final OnDeleteListener onDelete;

    public SupervisorsAdapter(OnDeleteListener onDelete) {
        this.onDelete = onDelete;
    }

    public void setList(List<ApiModels.SupervisorItem> list) {
        this.list.clear();
        if (list != null) this.list.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_supervisor, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApiModels.SupervisorItem item = list.get(position);
        holder.tvSupervisorId.setText(item.supervisorId);
        holder.tvSupervisorName.setText(item.name != null ? item.name : "");
        holder.tvSupervisorLine.setText("Line: " + (item.line != null ? item.line : ""));
        holder.btnDelete.setOnClickListener(v -> onDelete.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSupervisorId, tvSupervisorName, tvSupervisorLine;
        Button btnDelete;

        ViewHolder(View v) {
            super(v);
            tvSupervisorId = v.findViewById(R.id.tvSupervisorId);
            tvSupervisorName = v.findViewById(R.id.tvSupervisorName);
            tvSupervisorLine = v.findViewById(R.id.tvSupervisorLine);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
