package com.speedyblur.anticaptivate2;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class LogcatAdapter extends RecyclerView.Adapter<LogcatAdapter.ViewHolder> {
    ArrayList<String> data = new ArrayList<>();

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv;

        ViewHolder(View v) {
            super(v);
            this.tv = v.findViewById(R.id.logcatLine);
        }
    }

    LogcatAdapter() {}

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.logcat_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.tv.setText(data.get(i));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
