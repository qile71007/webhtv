package com.fongmi.android.tv.ui.custom;

import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.utils.Util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PlayerOsdController {

    public interface Source {
        PlayerManager getPlayer();

        String getTitle();
    }

    private static final DecimalFormat SPEED_FORMAT = new DecimalFormat("#.0");
    private static final int UID = App.get().getApplicationInfo().uid;
    private static final String KEY_SHOW_RESOLUTION = "show_resolution";

    private final SimpleDateFormat timeFormat;
    private final TextView topLeft;
    private final TextView topRight;
    private final TextView bottomLeft;
    private final TextView bottomRight;
    private final MiniProgressView miniProgress;
    private final Runnable update;
    private final Source source;
    private final View root;
    private final float miniSp;

    private long lastTotalRxBytes;
    private long lastTimeStamp;
    private boolean controlsVisible;
    private boolean started;

    public PlayerOsdController(View root, TextView topLeft, TextView topRight, TextView bottomLeft, TextView bottomRight, MiniProgressView miniProgress, Source source, float miniSp) {
        this.timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        this.miniProgress = miniProgress;
        this.bottomRight = bottomRight;
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
        this.topLeft = topLeft;
        this.miniSp = miniSp;
        this.source = source;
        this.root = root;
        this.update = this::update;
    }

    public void start() {
        started = true;
        if (!PlayerSetting.isOsdEnabled()) {
            root.setVisibility(View.GONE);
            return;
        }
        resetSpeed();
        App.post(update, 0);
    }

    public void stop() {
        started = false;
        App.removeCallbacks(update);
    }

    public void release() {
        stop();
    }

    public void setControlsVisible(boolean controlsVisible) {
        if (this.controlsVisible == controlsVisible) return;
        this.controlsVisible = controlsVisible;
        if (started) render();
    }

    private void update() {
        if (render()) App.post(update, 1000);
    }

    private boolean render() {
        boolean enabled = PlayerSetting.isOsdEnabled();
        root.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (!enabled) return false;
        setTextSize(miniSp);
        PlayerManager player = source.getPlayer();
        setTopLeft(player);
        setTopRight();
        setBottomLeft(player);
        setBottomRight();
        setMiniProgress(player);
        return true;
    }

    /**
     * 标题和分辨率分开控制显示，支持独立开关
     */
    private void setTopLeft(PlayerManager player) {
        String title = source.getTitle();
        String resolution = (player == null) ? "" : player.getSizeText();

        boolean showTitle = PlayerSetting.isOsdTitle();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.get());
        boolean showResolution = prefs.getBoolean(KEY_SHOW_RESOLUTION, true);

        StringBuilder display = new StringBuilder();
        if (showTitle && !TextUtils.isEmpty(title)) {
            display.append(title);
        }
        if (showResolution && !TextUtils.isEmpty(resolution)) {
            if (display.length() > 0) {
                display.append("\n");
            }
            display.append(resolution);
        }

        if (display.length() == 0) {
            topLeft.setVisibility(View.GONE);
            topLeft.setText(null);
        } else {
            topLeft.setText(display.toString());
            topLeft.setVisibility(View.VISIBLE);
        }
    }

    private void setTopRight() {
        topRight.setVisibility(PlayerSetting.isOsdTime() ? View.VISIBLE : View.GONE);
        if (PlayerSetting.isOsdTime()) topRight.setText(timeFormat.format(new Date()));
    }

    /**
     * 左下角播放进度：直播和点播均显示（去除 isLive 和 controlsVisible 限制）
     */
    private void setBottomLeft(PlayerManager player) {
        if (!PlayerSetting.isOsdProgress() || player == null) {
            bottomLeft.setVisibility(View.GONE);
            return;
        }
        long position = Math.max(0, player.getPosition());
        long duration = Math.max(0, player.getDuration());
        bottomLeft.setText(Util.timeMs(position) + " / " + Util.timeMs(duration));
        bottomLeft.setVisibility(View.VISIBLE);
    }

    private void setBottomRight() {
        bottomRight.setVisibility(PlayerSetting.isOsdTraffic() ? View.VISIBLE : View.GONE);
        if (!PlayerSetting.isOsdTraffic()) return;
        String speed = getSpeed();
        bottomRight.setText(speed);
        bottomRight.setVisibility(TextUtils.isEmpty(speed) ? View.GONE : View.VISIBLE);
    }

    private void setMiniProgress(PlayerManager player) {
        if (controlsVisible || !PlayerSetting.isOsdMini() || player == null || player.isLive()) {
            miniProgress.setVisibility(View.GONE);
            return;
        }
        long duration = Math.max(0, player.getDuration());
        if (duration <= 0) {
            miniProgress.setVisibility(View.GONE);
            return;
        }
        miniProgress.setProgress(player.getPosition(), duration);
        miniProgress.setVisibility(View.VISIBLE);
    }

    private void setTextSize(float sp) {
        topLeft.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        topRight.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        bottomLeft.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        bottomRight.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
    }

    private String getSpeed() {
        long total = TrafficStats.getUidRxBytes(UID);
        if (total == TrafficStats.UNSUPPORTED) return "";
        long now = System.currentTimeMillis();
        long rxKb = total / 1024;
        long speed = (rxKb - lastTotalRxBytes) * 1000 / Math.max(now - lastTimeStamp, 1);
        lastTimeStamp = now;
        lastTotalRxBytes = rxKb;
        return speed < 1000 ? speed + " KB/s" : SPEED_FORMAT.format(speed / 1024f) + " MB/s";
    }

    private void resetSpeed() {
        long total = TrafficStats.getUidRxBytes(UID);
        lastTotalRxBytes = total == TrafficStats.UNSUPPORTED ? 0 : total / 1024;
        lastTimeStamp = System.currentTimeMillis();
    }
}