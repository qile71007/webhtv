package com.fongmi.android.tv.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.AdapterConfigBinding;

import java.util.ArrayList;
import java.util.List;

public class ConfigAdapter extends RecyclerView.Adapter<ConfigAdapter.ViewHolder> {

    private final OnClickListener listener;
    private List<Config> mItems;            // 全部数据
    private List<Config> mFilteredItems;    // 过滤后展示的数据
    private boolean readOnly;
    private int selectedPosition = -1;      // 当前高亮位置

    public ConfigAdapter(OnClickListener listener) {
        this.listener = listener;
        this.mFilteredItems = new ArrayList<>();
    }

    public interface OnClickListener {
        void onTextClick(Config item);
        void onDeleteClick(Config item);
    }

    public ConfigAdapter readOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public ConfigAdapter addAll(int type) {
        mItems = Config.getAll(type);
        if (!mItems.isEmpty() && !readOnly) mItems.remove(0);
        mFilteredItems.clear();
        mFilteredItems.addAll(mItems);
        return this;
    }

    // ==================== 新增：搜索过滤 ====================
    public void filter(String keyword) {
        mFilteredItems.clear();
        if (TextUtils.isEmpty(keyword)) {
            mFilteredItems.addAll(mItems);
        } else {
            String lower = keyword.toLowerCase();
            for (Config item : mItems) {
                if (item.getName().toLowerCase().contains(lower) ||
                        item.getUrl().toLowerCase().contains(lower) ||
                        item.getDesc().toLowerCase().contains(lower)) {
                    mFilteredItems.add(item);
                }
            }
        }
        selectedPosition = -1; // 重置选中位置，由外部重新设置
        notifyDataSetChanged();
    }

    // ==================== 新增：获取当前展示列表 ====================
    public List<Config> getDisplayItems() {
        return mFilteredItems;
    }

    // ==================== 新增：查找配置位置（基于URL） ====================
    public int findPosition(Config target) {
        if (target == null) return -1;
        String targetUrl = target.getUrl();
        for (int i = 0; i < mFilteredItems.size(); i++) {
            if (mFilteredItems.get(i).getUrl().equals(targetUrl)) {
                return i;
            }
        }
        return -1;
    }

    // ==================== 新增：设置选中位置 ====================
    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    // ==================== 新增：获取当前选中的配置 ====================
    public Config getSelectedItem() {
        if (selectedPosition >= 0 && selectedPosition < mFilteredItems.size()) {
            return mFilteredItems.get(selectedPosition);
        }
        return null;
    }

    // ==================== 原有删除方法（适配过滤列表） ====================
    public int remove(Config item) {
        // 先从过滤列表中移除
        int position = mFilteredItems.indexOf(item);
        if (position == -1) return -1;
        item.delete();
        mFilteredItems.remove(position);
        // 同时从全部列表中移除
        mItems.remove(item);
        notifyItemRemoved(position);
        return getItemCount();
    }

    @Override
    public int getItemCount() {
        return mFilteredItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterConfigBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Config item = mFilteredItems.get(position);
        holder.binding.text.setText(item.getDesc());

        // ==================== 新增：高亮选中项 ====================
        boolean isSelected = (position == selectedPosition);
        holder.binding.text.setSelected(isSelected);
        holder.binding.text.setBackgroundResource(isSelected ? R.drawable.shape_item_selected : R.drawable.shape_item_normal);

        holder.binding.text.setOnClickListener(v -> {
            // 点击时更新选中位置
            setSelectedPosition(position);
            if (listener != null) listener.onTextClick(item);
        });

        holder.binding.delete.setVisibility(readOnly ? View.GONE : View.VISIBLE);
        holder.binding.delete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(item);
        });
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final AdapterConfigBinding binding;

        ViewHolder(@NonNull AdapterConfigBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}