package com.liuchang.momohongbao.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.liuchang.momohongbao.R;
import com.liuchang.momohongbao.model.bean.Hongbao;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by liuchang on 2018/3/4.
 */

public class HongbaoAdapter extends RecyclerView.Adapter<HongbaoAdapter.HongbaoViewHolder> {
    private List<Hongbao> mData = new ArrayList<>();

    @NonNull
    @Override
    public HongbaoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hongbao, parent, false);
        return new HongbaoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HongbaoViewHolder holder, int position) {
        holder.initItemView(mData.get(position));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void refreshData(List<Hongbao> data) {
        mData = data;
        notifyDataSetChanged();
    }

    static class HongbaoViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.date)
        TextView date;
        @BindView(R.id.time)
        TextView time;
        @BindView(R.id.amount)
        TextView amount;

        HongbaoViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void initItemView(Hongbao hongbao) {
            date.setText(hongbao.getDate());
            time.setText(hongbao.getTime());
            amount.setText(String.format("%.2f", hongbao.getAmount()));
        }
    }
}
