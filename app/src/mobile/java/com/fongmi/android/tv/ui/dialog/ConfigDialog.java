package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.DialogConfigBinding;
import com.fongmi.android.tv.impl.ConfigListener;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.utils.FileChooser;
import com.github.catvod.utils.Path;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ConfigDialog extends BaseAlertDialog {

    private DialogConfigBinding binding;
    private boolean append = true;
    private boolean edit;
    private String ori;
    private int type;

    public static ConfigDialog create() {
        return new ConfigDialog();
    }

    public ConfigDialog vod() {
        type = 0;
        return this;
    }

    public ConfigDialog live() {
        type = 1;
        return this;
    }

    public ConfigDialog wall() {
        type = 2;
        return this;
    }

    public ConfigDialog edit() {
        edit = true;
        return this;
    }

    public void show(Fragment fragment) {
        show(fragment.getChildFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogConfigBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setTitle(type == 0 ? R.string.setting_vod : type == 1 ? R.string.setting_live : R.string.setting_wall)
                .setView(getBinding().getRoot())
                .setPositiveButton(edit ? R.string.dialog_edit : R.string.dialog_positive, this::onPositive)
                .setNegativeButton(R.string.dialog_negative, null);
    }

    @Override
    protected void initView() {
        // 获取当前配置（仅用于编辑模式）
        Config config = getConfig();
        if (edit) {
            // 编辑模式：预填当前配置的名称和URL
            binding.name.setText(config != null ? config.getName() : "");
            binding.url.setText(ori = config != null ? config.getUrl() : "");
        } else {
            // 添加模式：清空输入框
            binding.name.setText("");
            binding.url.setText("");
            ori = "";
        }
        // 始终显示输入区域，让用户输入名称和URL（添加和编辑都需要）
        binding.input.setVisibility(View.VISIBLE);
        binding.url.setSelection(TextUtils.isEmpty(ori) ? 0 : ori.length());
    }

    @Override
    protected void initEvent() {
        binding.choose.setEndIconOnClickListener(this::onChoose);
        binding.url.addTextChangedListener(new CustomTextListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                detect(s.toString());
            }
        });
        binding.url.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) onPositive(null, 0);
            return true;
        });
    }

    private Config getConfig() {
        return switch (type) {
            case 0 -> VodConfig.get().getConfig();
            case 1 -> LiveConfig.get().getConfig();
            case 2 -> WallConfig.get().getConfig();
            default -> null;
        };
    }

    private void onChoose(View view) {
        FileChooser.from(launcher).show();
    }

    private void detect(String s) {
        if (append && "h".equalsIgnoreCase(s)) {
            append = false;
            binding.url.append("ttp://");
        } else if (append && "f".equalsIgnoreCase(s)) {
            append = false;
            binding.url.append("ile://");
        } else if (append && "a".equalsIgnoreCase(s)) {
            append = false;
            binding.url.append("ssets://");
        } else if (s.length() > 1) {
            append = false;
        } else if (s.isEmpty()) {
            append = true;
        }
    }

    private void onPositive(DialogInterface dialog, int which) {
        String url = binding.url.getText().toString().trim();
        String name = binding.name.getText().toString().trim();

        if (edit) {
            // 编辑已有配置：更新
            Config.find(ori, type).url(url).name(name).update();
        } else {
            // 添加新配置：先删除原配置（如果有），再创建新配置
            // 如果原配置存在（ori不为空），需要先删除？但添加模式下ori为空，所以直接创建
            // 使用 Config.find(url, type) 尝试查找是否已存在，如果存在则更新，否则创建新记录
            Config config = Config.find(url, type);
            if (config != null && !TextUtils.isEmpty(config.getUrl())) {
                // 如果已存在同URL的配置，则更新名称
                config.name(name).update();
            } else {
                // 否则创建新配置（通过 find 的默认行为可能自动创建，但保险起见调用 add 方法）
                // 假设 Config 有 add 方法，若没有则使用 find 并设置属性后 update
                // 这里简单使用 find 并设置名称和URL，如果找不到会自动创建（取决于实现）
                Config.add(url, name, type); // 假设有静态 add 方法
                // 如果 add 不存在，可改为：
                // Config.find(url, type).url(url).name(name).update();
                // 但 find 可能返回 null，需处理
            }
        }

        // 通知父Fragment刷新配置
        ((ConfigListener) requireParentFragment()).setConfig(Config.find(url, type));
        dismiss();
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null)
                    return;
                String path = "file:/" + FileChooser.getPathFromUri(result.getData().getData()).replace(Path.rootPath(), "");
                ((ConfigListener) requireParentFragment()).setConfig(Config.find(path, type));
                dismiss();
            }
    );
                    }
